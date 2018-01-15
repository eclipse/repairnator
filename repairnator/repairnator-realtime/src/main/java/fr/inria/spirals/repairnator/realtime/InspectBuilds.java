package fr.inria.spirals.repairnator.realtime;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.BuildStatus;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class InspectBuilds implements Runnable {

	private static final int SLEEP_BETWEEN_LOOPS = 10;
	private Deque<Build> waitingBuilds = new ConcurrentLinkedDeque<>();
	private RTScanner rtScanner;

	public InspectBuilds(RTScanner rtScanner) {
		this.rtScanner = rtScanner;
	}

	public void submitNewBuild(Build build) {
		this.waitingBuilds.add(build);
	}

	@Override
	public void run() {
		while (true) {
			for (Build build : this.waitingBuilds) {
				build.refreshStatus();
				if (build.getFinishedAt() != null) {
					if (build.getBuildStatus() == BuildStatus.FAILED) {
						this.rtScanner.submitBuildToExecution(build);
					}
					this.waitingBuilds.remove(build);
				}
			}

			try {
				Thread.sleep(SLEEP_BETWEEN_LOOPS * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
