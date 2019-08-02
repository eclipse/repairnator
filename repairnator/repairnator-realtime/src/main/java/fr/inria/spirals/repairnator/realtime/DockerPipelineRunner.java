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

    public ExecutorService getExecutorService() {
        return executorService;
    }

    private ExecutorService executorService;
    private String dockerImageId;
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
        this.executorService = Executors.newFixedThreadPool(nbThreads);

        LOGGER.debug("Executor service initialized for "+nbThreads+" threads.");
    }

    public int getRunning() {
        return this.submittedRunnablePipelineContainers.size();
    }

    public void submitBuild(Build build) {
        if (this.limitDateNextRetrieveDockerImage != null && this.limitDateNextRetrieveDockerImage.before(new Date())) {
            this.refreshDockerImage();
        }
		this.executorService.submit(this.submitBuild(this.dockerImageId, new InputBuildId(build.getId())));
    }

    public void switchOff() {
        LOGGER.warn("The process will now stop. "+this.getRunning()+" docker containers will be stopped.");
        for (RunnablePipelineContainer container : this.submittedRunnablePipelineContainers) {
            container.serialize("ABORT");
            container.killDockerContainer(this.getDockerClient(), false);
        }

        this.executorService.shutdownNow();
    }

    @Override
    public void removeSubmittedRunnablePipelineContainer(RunnablePipelineContainer pipelineContainer) {
        LOGGER.info("Build (id: "+pipelineContainer.getInputBuildId().getBuggyBuildId()+") has finished.");
        pipelineContainer.killDockerContainer(this.getDockerClient(), false);
        super.removeSubmittedRunnablePipelineContainer(pipelineContainer);
    }
}
