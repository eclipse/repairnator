package fr.inria.spirals.repairnator.realtime;

import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.dockerpool.AbstractPoolManager;
import fr.inria.spirals.repairnator.dockerpool.RunnablePipelineContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Deque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

public class BuildRunner extends AbstractPoolManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(BuildRunner.class);
    private static final int DELAY_BETWEEN_DOCKER_IMAGE_REFRESH = 60; // in minutes
    private int nbThreads;
    private Deque<Build> waitingBuilds;
    private RTScanner rtScanner;
    private ExecutorService executorService;
    private String dockerImageId;
    private String dockerImageName;
    private Date limitDateNextRetrieveDockerImage;

    public BuildRunner(RTScanner rtScanner) {
        this.rtScanner = rtScanner;
    }

    public void setDockerImageName(String dockerImageName) {
        this.dockerImageName = dockerImageName;
        this.refreshDockerImage();
    }

    private void refreshDockerImage() {
        this.dockerImageId = this.findDockerImage(this.dockerImageName);
        this.limitDateNextRetrieveDockerImage = new Date(new Date().toInstant().plus(DELAY_BETWEEN_DOCKER_IMAGE_REFRESH, ChronoUnit.MINUTES).toEpochMilli());
        LOGGER.debug("Find the following docker image: "+this.dockerImageId);
    }

    public void initExecutorService(int nbThreads) {
        this.nbThreads = nbThreads;
        this.executorService = Executors.newFixedThreadPool(nbThreads);
        this.waitingBuilds = new LinkedBlockingDeque<>(this.nbThreads*4);
        LOGGER.debug("Executor service initialized for "+nbThreads+" threads.");
    }

    public int getRunning() {
        return this.submittedRunnablePipelineContainers.size();
    }

    public void submitBuild(Build build) {
        if (this.limitDateNextRetrieveDockerImage.before(new Date())) {
            this.refreshDockerImage();
        }
        if (getRunning() < this.nbThreads) {
            LOGGER.info("Build (id: "+build.getId()+") immediately submitted for running.");
            this.executorService.submit(this.submitBuild(this.dockerImageId, build.getId(), 0));
        } else {
            LOGGER.info("All threads currently running (Limit: "+this.nbThreads+"). Add build (id: "+build.getId()+") to list");
            if (this.waitingBuilds.size() == this.nbThreads) {
                Build b = this.waitingBuilds.removeLast();
                LOGGER.debug("Remove oldest build (id: "+b.getId()+")");
            }
            this.waitingBuilds.push(build);
        }
    }

    @Override
    public void removeSubmittedRunnablePipelineContainer(RunnablePipelineContainer pipelineContainer) {
        LOGGER.info("Build (id: "+pipelineContainer.getBuildId()+") has finished.");
        super.removeSubmittedRunnablePipelineContainer(pipelineContainer);
        if (!this.waitingBuilds.isEmpty()) {
            Build build = this.waitingBuilds.pollFirst();
            this.submitBuild(build);
        }
    }
}
