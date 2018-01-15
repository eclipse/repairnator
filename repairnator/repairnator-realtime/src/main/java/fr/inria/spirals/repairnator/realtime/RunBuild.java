package fr.inria.spirals.repairnator.realtime;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.dockerpool.AbstractPoolManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RunBuild extends AbstractPoolManager {

	private RTScanner rtScanner;
	private ExecutorService executorService;
	private String dockerImage;

	public RunBuild(RTScanner rtScanner) {
		this.rtScanner = rtScanner;
	}

	public void initDockerImage(String imageName) {
		this.dockerImage = this.findDockerImage(imageName);
	}

	public void initExecutorService(int nbThreads) {
		this.executorService = Executors.newFixedThreadPool(nbThreads);
	}

	public int getRunningOrPending() {
		return this.submittedRunnablePipelineContainers.size();
	}

	public void submitBuild(Build build) {
		this.executorService.submit(this.submitBuild(this.dockerImage, build.getId()));
	}
}
