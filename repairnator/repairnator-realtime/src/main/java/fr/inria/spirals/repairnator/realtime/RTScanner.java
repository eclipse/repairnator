package fr.inria.spirals.repairnator.realtime;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.BuildTool;
import fr.inria.spirals.jtravis.entities.Job;
import fr.inria.spirals.jtravis.entities.Log;
import fr.inria.spirals.jtravis.entities.Repository;
import fr.inria.spirals.jtravis.helpers.BuildHelper;
import fr.inria.spirals.jtravis.helpers.RepositoryHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class RTScanner {


	private final List<Integer> blackListedRepository;
	private final List<Integer> whiteListedRepository;
	private final InspectBuilds inspectBuilds;
	private final InspectJobs inspectJobs;
	private final BuildRunner buildRunner;
	private boolean running;

	public RTScanner() {
		this.blackListedRepository = new ArrayList<>();
		this.whiteListedRepository = new ArrayList<>();
		this.buildRunner = new BuildRunner(this);
		this.inspectBuilds = new InspectBuilds(this);
		this.inspectJobs = new InspectJobs(this);
	}

	public BuildRunner getBuildRunner() {
		return buildRunner;
	}

	public InspectBuilds getInspectBuilds() {
		return inspectBuilds;
	}

	public InspectJobs getInspectJobs() {
		return inspectJobs;
	}

	public void initWhiteListedRepository(File whiteListFile) {
		try {
			List<String> lines = Files.readAllLines(whiteListFile.toPath());
			for (String repoName : lines) {
				Repository repository = RepositoryHelper.getRepositoryFromSlug(repoName);
				if (repository != null) {
					this.whiteListedRepository.add(repository.getId());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void initBlackListedRepository(File blackListFile) {
		try {
			List<String> lines = Files.readAllLines(blackListFile.toPath());
			for (String repoName : lines) {
				Repository repository = RepositoryHelper.getRepositoryFromSlug(repoName);
				if (repository != null) {
					this.blackListedRepository.add(repository.getId());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
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

	public void submitBuildToExecution(Build build) {
		boolean failing = false;
		for (Job job : build.getJobs()) {
			Log jobLog = job.getLog();
			if (jobLog.getTestsInformation() != null && jobLog.getTestsInformation().getErrored() >= 0 || jobLog.getTestsInformation().getFailing() >= 0) {
				failing = true;
				break;
			}
		}
		if (failing) {
			this.buildRunner.submitBuild(build);
		}
	}

	public void submitWaitingBuild(int buildId) {
		Build build = BuildHelper.getBuildFromId(buildId, null);
		this.inspectBuilds.submitNewBuild(build);
	}
}
