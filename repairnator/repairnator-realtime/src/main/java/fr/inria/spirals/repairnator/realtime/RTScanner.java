package fr.inria.spirals.repairnator.realtime;

import fr.inria.jtravis.entities.Build;
import fr.inria.jtravis.entities.BuildTool;
import fr.inria.jtravis.entities.Job;
import fr.inria.jtravis.entities.Log;
import fr.inria.jtravis.entities.Repository;
import fr.inria.jtravis.helpers.BuildHelper;
import fr.inria.jtravis.helpers.RepositoryHelper;
import fr.inria.spirals.repairnator.notifier.EndProcessNotifier;
import fr.inria.spirals.repairnator.realtime.serializer.BlacklistedSerializer;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RTScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(RTScanner.class);
    private static final int DURATION_IN_TEMP_BLACKLIST = 600; // in seconds
    private final List<Integer> blackListedRepository;
    private final List<Integer> whiteListedRepository;
    private final Map<Integer,Date> tempBlackList;
    private final InspectBuilds inspectBuilds;
    private final InspectJobs inspectJobs;
    private final BuildRunner buildRunner;
    private FileWriter blacklistWriter;
    private FileWriter whitelistWriter;
    private boolean running;
    private String runId;
    private List<SerializerEngine> engines;
    private BlacklistedSerializer blacklistedSerializer;
    private Duration duration;
    private EndProcessNotifier endProcessNotifier;

    public RTScanner(String runId, List<SerializerEngine> engines) {
        this.engines = engines;
        this.blackListedRepository = new ArrayList<>();
        this.whiteListedRepository = new ArrayList<>();
        this.tempBlackList = new HashMap<>();
        this.buildRunner = new BuildRunner(this);
        this.inspectBuilds = new InspectBuilds(this);
        this.inspectJobs = new InspectJobs(this);
        this.runId = runId;
        this.blacklistedSerializer = new BlacklistedSerializer(this.engines, this);
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public void setEndProcessNotifier(EndProcessNotifier endProcessNotifier) {
        this.endProcessNotifier = endProcessNotifier;
    }

    public List<SerializerEngine> getEngines() {
        return engines;
    }

    public String getRunId() {
        return runId;
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
        LOGGER.info("Init whitelist repository...");
        try {
            List<String> lines = Files.readAllLines(whiteListFile.toPath());
            for (String repoId : lines) {
                if (!repoId.trim().isEmpty()) {
                    this.whiteListedRepository.add(Integer.parseInt(repoId));
                }
            }

            this.whitelistWriter = new FileWriter(whiteListFile, true);
        } catch (IOException e) {
            LOGGER.error("Error while initializing whitelist", e);
        }
        LOGGER.info("Whitelist initialized with: "+this.whiteListedRepository.size()+" entries");
    }

    public void initBlackListedRepository(File blackListFile) {
        LOGGER.info("Init blacklist repository...");
        try {
            List<String> lines = Files.readAllLines(blackListFile.toPath());
            for (String repoId : lines) {
                if (!repoId.trim().isEmpty()) {
                    this.blackListedRepository.add(Integer.parseInt(repoId));
                }
            }

            this.blacklistWriter = new FileWriter(blackListFile, true);
        } catch (IOException e) {
            LOGGER.error("Error while initializing blacklist", e);
        }
        LOGGER.info("Blacklist initialized with: "+this.blackListedRepository.size()+" entries");
    }

    public void launch() {
        if (!this.running) {
            LOGGER.info("Start running RTScanner...");
            new Thread(this.inspectBuilds).start();
            new Thread(this.inspectJobs).start();
            this.running = true;

            if (this.duration != null) {
                InspectProcessDuration inspectProcessDuration;
                if (this.endProcessNotifier != null) {
                    inspectProcessDuration = new InspectProcessDuration(this.duration, this.inspectBuilds, this.inspectJobs, this.buildRunner, this.endProcessNotifier);
                } else {
                    inspectProcessDuration = new InspectProcessDuration(this.duration, this.inspectBuilds, this.inspectJobs, this.buildRunner);
                }

                new Thread(inspectProcessDuration).start();
            }
        }
    }

    private void addInBlacklistRepository(Repository repository, BlacklistedSerializer.Reason reason, String comment) {
        LOGGER.info("Repository "+repository.getSlug()+" (id: "+repository.getId()+") is blacklisted. Reason: "+reason.name()+" Comment: "+comment);
        this.blacklistedSerializer.serialize(repository, reason, comment);
        this.blackListedRepository.add(repository.getId());
        try {
            this.blacklistWriter.append(repository.getId()+"");
            this.blacklistWriter.append("\n");
            this.blacklistWriter.flush();
        } catch (IOException e) {
            LOGGER.error("Error while writing entry in blacklist");
        }
    }

    private void addInWhitelistRepository(Repository repository) {
        this.whiteListedRepository.add(repository.getId());
        try {
            this.whitelistWriter.append(repository.getId()+"");
            this.whitelistWriter.append("\n");
            this.whitelistWriter.flush();
        } catch (IOException e) {
            LOGGER.error("Error while writing entry in whitelist");
        }
    }

    private void addInTempBlackList(Repository repository, String comment) {
        Date expirationDate = new Date(new Date().toInstant().plusSeconds(DURATION_IN_TEMP_BLACKLIST).toEpochMilli());
        LOGGER.info("Repository "+repository.getSlug()+" (id: "+repository.getId()+") is temporary blacklisted (expiration date: "+expirationDate.toString()+"). Reason: "+comment);
        this.tempBlackList.put(repository.getId(), expirationDate);
    }

    public boolean isRepositoryInteresting(int repositoryId) {
        if (this.blackListedRepository.contains(repositoryId)) {
            //LOGGER.debug("Repo already blacklisted (id: "+repositoryId+")");
            return false;
        }

        if (this.whiteListedRepository.contains(repositoryId)) {
            //LOGGER.debug("Repo already whitelisted (id: "+repositoryId+")");
            return true;
        }

        if (this.tempBlackList.containsKey(repositoryId)) {
            if (this.tempBlackList.get(repositoryId).after(new Date())) {
                return false;
            } else {
                this.tempBlackList.remove(repositoryId);
            }
        }

        Repository repository = RepositoryHelper.getRepositoryFromId(repositoryId);
        if (repository != null) {
            Build masterBuild = BuildHelper.getLastSuccessfulBuildFromMaster(repository, false, 5);

            if (masterBuild == null) {
                this.addInTempBlackList(repository, "No successful build found.");
                return false;
            } else {
                if (masterBuild.getConfig().getLanguage() == null || !masterBuild.getConfig().getLanguage().equals("java")) {
                    this.addInBlacklistRepository(repository, BlacklistedSerializer.Reason.OTHER_LANGUAGE, masterBuild.getConfig().getLanguage());
                    return false;
                }

                if (masterBuild.getBuildTool() == BuildTool.GRADLE) {
                    this.addInBlacklistRepository(repository, BlacklistedSerializer.Reason.USE_GRADLE, "");
                    return false;
                } else if (masterBuild.getBuildTool() == BuildTool.UNKNOWN) {
                    this.addInBlacklistRepository(repository, BlacklistedSerializer.Reason.UNKNOWN_BUILD_TOOL, "");
                    return false;
                }

                if (!masterBuild.getJobs().isEmpty()) {
                    Job firstJob = masterBuild.getJobs().get(0);
                    Log jobLog = firstJob.getLog();
                    if (jobLog.getTestsInformation() != null && jobLog.getTestsInformation().getRunning() > 0) {
                        LOGGER.info("Tests has been found in repository "+repository.getSlug()+" (id: "+repositoryId+") build (id: "+masterBuild.getId()+"). The repo is now whitelisted.");
                        this.addInWhitelistRepository(repository);
                        return true;
                    } else {
                        this.addInTempBlackList(repository, "No test found");
                    }
                } else {
                    LOGGER.info("No job found in repository "+repository.getSlug()+" (id: "+repositoryId+") build (id: "+masterBuild.getId()+"). It is not considered right now.");
                }
            }
        } else {
            LOGGER.info("Repository not found with the following id: "+repositoryId+" it will be temporary blacklisted");
            Date expirationDate = new Date(new Date().toInstant().plusSeconds(DURATION_IN_TEMP_BLACKLIST).toEpochMilli());
            this.tempBlackList.put(repositoryId, expirationDate);
        }


        return false;
    }

    public void submitBuildToExecution(Build build) {
        boolean failing = false;
        List<Job> jobs = build.getJobs();
        if (jobs != null) {
            for (Job job : jobs) {
                Log jobLog = job.getLog();
                if (jobLog != null && jobLog.getTestsInformation() != null && (jobLog.getTestsInformation().getErrored() >= 0 || jobLog.getTestsInformation().getFailing() >= 0)) {
                    failing = true;
                    break;
                }
            }
        }

        if (failing) {
            LOGGER.info("Failing or erroring tests has been found in build (id: "+build.getId()+")");
            this.buildRunner.submitBuild(build);
        } else {
            LOGGER.info("No failing or erroring test has been found in build (id: "+build.getId()+")");
        }
    }

    public void submitWaitingBuild(int buildId) {
        Build build = BuildHelper.getBuildFromId(buildId, null);
        if (build != null) {
            this.inspectBuilds.submitNewBuild(build);
        }
    }
}
