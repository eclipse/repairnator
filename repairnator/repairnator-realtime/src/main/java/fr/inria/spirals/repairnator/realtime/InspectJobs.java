package fr.inria.spirals.repairnator.realtime;

import fr.inria.spirals.jtravis.entities.Job;
import fr.inria.spirals.jtravis.entities.Repository;
import fr.inria.spirals.jtravis.helpers.JobHelper;
import fr.inria.spirals.jtravis.helpers.RepositoryHelper;

import java.util.List;

public class InspectJobs implements Runnable {
	private static final int SLEEP_TIME = 10;
	private RTScanner rtScanner;

	public InspectJobs(RTScanner scanner) {
		this.rtScanner = scanner;
	}

	@Override
	public void run() {
		while (true) {
			if (this.rtScanner.isReadyToAnalyzeJobs()) {
				List<Job> jobList = JobHelper.getJobList();

				for (Job job : jobList) {
					if (this.rtScanner.isRepositoryInteresting(job.getBuildId())) {
						this.rtScanner.submitWaitingBuild(job.getBuildId());
					}
				}
			}

			try {
				Thread.sleep(SLEEP_TIME * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
