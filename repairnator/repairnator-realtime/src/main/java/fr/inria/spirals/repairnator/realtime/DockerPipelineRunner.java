package fr.inria.spirals.repairnator.realtime;

import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.InputBuildId;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.docker.DockerHelper;
import fr.inria.spirals.repairnator.dockerpool.DockerPoolManager;
import fr.inria.spirals.repairnator.dockerpool.RunnablePipelineContainer;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.json.JSONFileSerializerEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * This class is in charge with launching the docker containers
 */
public class DockerPipelineRunner extends DockerPoolManager implements PipelineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DockerPipelineRunner.class);
    private static final int DELAY_BETWEEN_DOCKER_IMAGE_REFRESH = 60; // in minutes
    public static final String REPAIRNATOR_PIPELINE_DOCKER_IMAGE_NAME = "repairnator/pipeline";
    private int nbThreads;
    private Deque<Build> waitingBuilds;

    public ExecutorService getExecutorService() {
        return executorService;
    }

    private ExecutorService executorService;
    private String dockerImageId= REPAIRNATOR_PIPELINE_DOCKER_IMAGE_NAME;
    private String dockerImageName;
    private Date limitDateNextRetrieveDockerImage;

    public DockerPipelineRunner(RTScanner rtScanner) {
        LOGGER.info("Init build runner");
        super.setDockerOutputDir(RepairnatorConfig.getInstance().getLogDirectory());
        super.setRunId(RepairnatorConfig.getInstance().getRunId());
        super.setEngines(rtScanner.getEngines());
    }

    public DockerPipelineRunner() {
        LOGGER.info("Init build runner");
        super.setDockerOutputDir("./");
        super.setRunId("hardcoded-in-constructor");
        ArrayList<SerializerEngine> engines = new ArrayList<>();
        engines.add(new JSONFileSerializerEngine("."));
        super.setEngines(engines);
    }

    public void initRunner() {
        if (RepairnatorConfig.getInstance().getDockerImageName() != null) {
            this.setDockerImageName(RepairnatorConfig.getInstance().getDockerImageName());
        }
        this.initExecutorService(RepairnatorConfig.getInstance().getNbThreads());
    }

    public void setDockerImageName(String dockerImageName) {
        this.dockerImageName = dockerImageName;
        this.refreshDockerImage();
    }

    /**
     * This allows us to automatically refresh docker images every 60 minutes
     */
    private void refreshDockerImage() {
        this.dockerImageId = DockerHelper.findDockerImage(this.dockerImageName, this.getDockerClient());
        this.limitDateNextRetrieveDockerImage = new Date(new Date().toInstant().plus(DELAY_BETWEEN_DOCKER_IMAGE_REFRESH, ChronoUnit.MINUTES).toEpochMilli());
        LOGGER.debug("Find the following docker image: "+this.dockerImageId);
    }

    public void initExecutorService(int nbThreads) {
        this.nbThreads = nbThreads;
        this.executorService = Executors.newFixedThreadPool(nbThreads);

        // we init a list of waiting builds, when the pool is already full
        // its size it 4 times the size of the pool
        this.waitingBuilds = new LinkedBlockingDeque<>(this.nbThreads*4);
        LOGGER.debug("Executor service initialized for "+nbThreads+" threads.");
    }

    public int getRunning() {
        return this.submittedRunnablePipelineContainers.size();
    }

    public void submitBuild(Build build) {
        if (this.limitDateNextRetrieveDockerImage != null && this.limitDateNextRetrieveDockerImage.before(new Date())) {
            this.refreshDockerImage();
        }
        if (getRunning() < this.nbThreads) {
            LOGGER.info("Build (id: "+build.getId()+") immediately submitted for running.");
            this.executorService.submit(this.submitBuild(this.dockerImageId, new InputBuildId(build.getId())));
        } else {
            LOGGER.info("All threads currently running (Limit: "+this.nbThreads+"). Add build (id: "+build.getId()+") to wait list");
            if (this.waitingBuilds.size() == this.nbThreads) {
                Build b = this.waitingBuilds.removeLast();
                LOGGER.debug("Remove oldest build (id: "+b.getId()+")");
            }
            this.waitingBuilds.push(build);
        }
    }

    public void switchOff() {
        LOGGER.warn("The process will now stop. "+this.getRunning()+" docker containers will be stopped.");
        this.waitingBuilds.clear();
        for (RunnablePipelineContainer container : this.submittedRunnablePipelineContainers) {
            container.serialize("ABORT");
            container.killDockerContainer(this.getDockerClient(), false);
        }

        this.executorService.shutdownNow();
    }

    @Override
    public void removeSubmittedRunnablePipelineContainer(RunnablePipelineContainer pipelineContainer) {
        LOGGER.info("Build (id: "+pipelineContainer.getInputBuildId().getBuggyBuildId()+") has finished.");
        super.removeSubmittedRunnablePipelineContainer(pipelineContainer);
        if (!this.waitingBuilds.isEmpty()) {
            Build build = this.waitingBuilds.pollFirst();
            this.submitBuild(build);
        }
    }
}
