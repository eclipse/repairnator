package fr.inria.spirals.repairnator.realtime;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.dockerpool.AbstractPoolManager;
import fr.inria.spirals.repairnator.dockerpool.RunnablePipelineContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

public class BuildRunner extends AbstractPoolManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(BuildRunner.class);
	private int nbThreads;
	private Deque<Build> waitingBuilds;
	private RTScanner rtScanner;
	private ExecutorService executorService;
	private String dockerImage;

	public BuildRunner(RTScanner rtScanner) {
		this.rtScanner = rtScanner;
	}

	public void initDockerImage(String imageName) {
		this.dockerImage = this.findDockerImage(imageName);
		LOGGER.debug("Find the following docker image: "+this.dockerImage);
	}

	public void initExecutorService(int nbThreads) {
		this.nbThreads = nbThreads;
		this.executorService = Executors.newFixedThreadPool(nbThreads);
		this.waitingBuilds = new LinkedBlockingDeque<>(this.nbThreads);
		LOGGER.debug("Executor service initialized for "+nbThreads+" threads.");
	}

	public int getRunning() {
		return this.submittedRunnablePipelineContainers.size();
	}

	public void submitBuild(Build build) {
		if (getRunning() < this.nbThreads) {
			LOGGER.info("Build (id: "+build.getId()+") immediately submitted for running.");
			this.executorService.submit(this.submitBuild(this.dockerImage, build.getId()));
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
