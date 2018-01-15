package fr.inria.spirals.repairnator.realtime;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.BuildTool;
import fr.inria.spirals.jtravis.entities.Job;
import fr.inria.spirals.jtravis.entities.Log;
import fr.inria.spirals.jtravis.entities.Repository;
import fr.inria.spirals.jtravis.helpers.BuildHelper;
import fr.inria.spirals.jtravis.helpers.RepositoryHelper;

import java.util.ArrayList;
import java.util.List;

public class RTScanner {


	private final List<Integer> blackListedRepository;
	private final List<Integer> whiteListedRepository;
	private final InspectBuilds inspectBuilds;
	private final InspectJobs inspectJobs;
	private final RunBuild runBuild;
	private boolean running;

	public RTScanner() {
		this.blackListedRepository = new ArrayList<>();
		this.whiteListedRepository = new ArrayList<>();
		this.runBuild = new RunBuild(this);
		this.inspectBuilds = new InspectBuilds(this);
		this.inspectJobs = new InspectJobs(this);
	}

	public void launch() {
		if (!this.running) {
			new Thread(this.inspectBuilds).run();
			new Thread(this.inspectJobs).run();
			this.running = true;
		}
	}

	public boolean isRepositoryInteresting(int repositoryId) {
		if (this.blackListedRepository.contains(repositoryId)) {
			return false;
		}

		if (this.whiteListedRepository.contains(repositoryId)) {
			return true;
		}

		Repository repository = RepositoryHelper.getRepositoryFromId(repositoryId);
		Build masterBuild = BuildHelper.getLastBuildFromMaster(repository);

		if (masterBuild == null) {
			this.blackListedRepository.add(repositoryId);
			return false;
		} else {
			if (masterBuild.getConfig().getLanguage() == null || !masterBuild.getConfig().getLanguage().equals("java")) {
				this.blackListedRepository.add(repositoryId);
				return false;
			}

			if (masterBuild.getBuildTool() != BuildTool.MAVEN) {
				this.blackListedRepository.add(repositoryId);
				return false;
			}

			if (!masterBuild.getJobs().isEmpty()) {
				Job firstJob = masterBuild.getJobs().get(0);
				Log jobLog = firstJob.getLog();
				if (jobLog.getTestsInformation() != null && jobLog.getTestsInformation().getRunning() > 0) {
					this.whiteListedRepository.add(repositoryId);
					return true;
				}
			}
		}

		return false;
	}

	public boolean isRepositoryWhiteListed(int repositoryId) {
		return this.whiteListedRepository.contains(repositoryId);
	}



	public void submitBuildToExecution(Build build) {

	}

	public void submitWaitingBuild(int buildId) {
		Build build = BuildHelper.getBuildFromId(buildId, null);
		this.inspectBuilds.submitNewBuild(build);
	}

	public boolean isReadyToAnalyzeJobs() {
		return false;
	}
}
