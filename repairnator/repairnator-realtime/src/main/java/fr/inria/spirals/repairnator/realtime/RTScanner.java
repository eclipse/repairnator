package fr.inria.spirals.repairnator.realtime;

import fr.inria.jtravis.JTravis;
import fr.inria.jtravis.entities.Build;
import fr.inria.jtravis.entities.BuildTool;
import fr.inria.jtravis.entities.Job;
import fr.inria.jtravis.entities.Log;
import fr.inria.jtravis.entities.Repository;
import fr.inria.jtravis.entities.StateType;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.EndProcessNotifier;
import fr.inria.spirals.repairnator.realtime.serializer.BlacklistedSerializer;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This class is the backbone for the realtime scanner.
 */
public class RTScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(RTScanner.class);
    private static final int DURATION_IN_TEMP_BLACKLIST = 600; // in seconds

    // lists are using repository ID: that's why they're typed with long
    private final List<Long> blackListedRepository;
    private final List<Long> whiteListedRepository;
    private final Map<Long,Date> tempBlackList;


    private final InspectBuilds inspectBuilds;
    private final InspectJobs inspectJobs;
    private final BuildRunner buildRunner;
    private FileWriter blacklistWriter;
    private FileWriter whitelistWriter;
    private boolean running;
    private String runId;
    private List<SerializerEngine> engines;
    private BlacklistedSerializer blacklistedSerializer;
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

    public void setEndProcessNotifier(EndProcessNotifier endProcessNotifier) {
        this.endProcessNotifier = endProcessNotifier;
    }

    public List<SerializerEngine> getEngines() {
        return engines;
    }

    public String getRunId() {
        return runId;
    }

    public InspectBuilds getInspectBuilds() {
        return inspectBuilds;
    }

    public void initWhiteListedRepository(File whiteListFile) {
        LOGGER.info("Init whitelist repository...");
        try {
            List<String> lines = Files.readAllLines(whiteListFile.toPath());
            for (String repoId : lines) {
                if (!repoId.trim().isEmpty()) {
                    this.whiteListedRepository.add(Long.parseLong(repoId));
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
                    this.blackListedRepository.add(Long.parseLong(repoId));
                }
            }

            this.blacklistWriter = new FileWriter(blackListFile, true);
        } catch (IOException e) {
            LOGGER.error("Error while initializing blacklist", e);
        }
        LOGGER.info("Blacklist initialized with: "+this.blackListedRepository.size()+" entries");
    }

    /**
     * This method can only be called once.
     * It starts two or three new thread:
     *   - one thread for build inspection
     *   - one thread for job inspection
     *
     * If a duration has been given it also starts a thread for respecting the duration.
     */
    public void launch() {
        if (!this.running) {
            LOGGER.info("Start running RTScanner...");
            this.buildRunner.initRunner();
            new Thread(this.inspectBuilds).start();
            new Thread(this.inspectJobs).start();
            this.running = true;

            if (RepairnatorConfig.getInstance().getDuration() != null) {
                InspectProcessDuration inspectProcessDuration;
                if (this.endProcessNotifier != null) {
                    inspectProcessDuration = new InspectProcessDuration(this.inspectBuilds, this.inspectJobs, this.buildRunner, this.endProcessNotifier);
                } else {
                    inspectProcessDuration = new InspectProcessDuration(this.inspectBuilds, this.inspectJobs, this.buildRunner);
                }

                new Thread(inspectProcessDuration).start();
            }
        }
    }

    private void addInBlacklistRepository(Repository repository, BlacklistedSerializer.Reason reason, String comment) {
        LOGGER.info("Repository "+repository.getSlug()+" (id: "+repository.getId()+") is blacklisted. Reason: "+reason.name()+" Comment: "+comment);
        this.blacklistedSerializer.serialize(repository, reason, comment);
        this.blackListedRepository.add(repository.getId());

        if (this.blacklistWriter != null) {
            try {
                this.blacklistWriter.append(repository.getId()+"");
                this.blacklistWriter.append("\n");
                this.blacklistWriter.flush();
            } catch (IOException e) {
                LOGGER.error("Error while writing entry in blacklist");
            }
        } else {
            LOGGER.warn("Blacklist file not initialized: the entry won't be written.");
        }

    }

    private void addInWhitelistRepository(Repository repository) {
        this.whiteListedRepository.add(repository.getId());

        if (this.whitelistWriter != null) {
            try {
                this.whitelistWriter.append(String.valueOf(repository.getId()));
                this.whitelistWriter.append("\n");
                this.whitelistWriter.flush();
            } catch (IOException e) {
                LOGGER.error("Error while writing entry in whitelist");
            }
        } else {
            LOGGER.warn("Whitelist file not initialized: the entry won't be written.");
        }

    }

    private void addInTempBlackList(Repository repository, String comment) {
        Date expirationDate = new Date(new Date().toInstant().plusSeconds(DURATION_IN_TEMP_BLACKLIST).toEpochMilli());
        LOGGER.info("Repository "+repository.getSlug()+" (id: "+repository.getId()+") is temporary blacklisted (expiration date: "+expirationDate.toString()+"). Reason: "+comment);
        this.tempBlackList.put(repository.getId(), expirationDate);
    }

    /**
     * Main method for specifying if a repositoryId is interesting or not.
     * It checks on the whitelist and blacklists first, and then apply the following criteria:
     *   - check if the repo already has a successful build, (tempblacklist if not the case)
     *   - check if the successful build was in java (perm blacklist if not the case)
     *   - check if the build tool used was Maven (perm blacklist if not the case)
     *   - check if unit tests were found (temp blacklist if not the case)
     *
     * If those critera are respected, then the repo is whitelisted.
     *
     * @return true if the repository is whitelisted.
     */
    public boolean isRepositoryInteresting(long repositoryId) {
        if (this.blackListedRepository.contains(repositoryId)) {
            return false;
        }

        if (this.whiteListedRepository.contains(repositoryId)) {
            return true;
        }

        if (this.tempBlackList.containsKey(repositoryId)) {
            if (this.tempBlackList.get(repositoryId).after(new Date())) {
                return false;
            } else {
                this.tempBlackList.remove(repositoryId);
            }
        }

        JTravis jTravis = RepairnatorConfig.getInstance().getJTravis();
        Optional<Repository> repositoryOptional = jTravis.repository().fromId(repositoryId);
        if (repositoryOptional.isPresent()) {
            Repository repository = repositoryOptional.get();

            Optional<Build> optionalBuild = jTravis.build().lastBuildFromMasterWithState(repository, StateType.PASSED);
            if (!optionalBuild.isPresent()) {
                this.addInTempBlackList(repository, "No successful build found.");
                return false;
            } else {
                Build masterBuild = optionalBuild.get();
                if (masterBuild.getLanguage() == null || !masterBuild.getLanguage().equals("java")) {
                    this.addInBlacklistRepository(repository, BlacklistedSerializer.Reason.OTHER_LANGUAGE, masterBuild.getLanguage());
                    return false;
                }

                Optional<BuildTool> optionalBuildTool = masterBuild.getBuildTool();
                if (!optionalBuildTool.isPresent()) {
                    this.addInBlacklistRepository(repository, BlacklistedSerializer.Reason.UNKNOWN_BUILD_TOOL, "");
                    return false;
                }

                BuildTool buildTool = optionalBuildTool.get();
                if (buildTool == BuildTool.GRADLE) {
                    this.addInBlacklistRepository(repository, BlacklistedSerializer.Reason.USE_GRADLE, "");
                    return false;
                } else if (buildTool == BuildTool.UNKNOWN) {
                    this.addInBlacklistRepository(repository, BlacklistedSerializer.Reason.UNKNOWN_BUILD_TOOL, "");
                    return false;
                }

                if (!masterBuild.getJobs().isEmpty()) {
                    Job firstJob = masterBuild.getJobs().get(0);
                    Optional<Log> optionalLog = firstJob.getLog();
                    if (optionalLog.isPresent()) {
                        Log jobLog = optionalLog.get();
                        if (jobLog.getTestsInformation() != null && jobLog.getTestsInformation().getRunning() > 0) {
                            LOGGER.info("Tests has been found in repository "+repository.getSlug()+" (id: "+repositoryId+") build (id: "+masterBuild.getId()+"). The repo is now whitelisted.");
                            this.addInWhitelistRepository(repository);
                            return true;
                        } else {
                            this.addInTempBlackList(repository, "No test found");
                        }
                    } else {
                        LOGGER.error("Error while getting log for job "+firstJob.getId());
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

    /**
     * This method submits a build to the build runner if and only if the build contained failing tests.
     */
    public void submitBuildToExecution(Build build) {
        boolean failing = false;
        List<Job> jobs = build.getJobs();
        if (jobs != null) {
            for (Job job : jobs) {
                Optional<Log> optionalLog = job.getLog();
                if (optionalLog.isPresent()) {
                    Log jobLog = optionalLog.get();
                    if (jobLog.getTestsInformation() != null && (jobLog.getTestsInformation().getErrored() >= 0 || jobLog.getTestsInformation().getFailing() >= 0)) {
                        failing = true;
                        break;
                    }
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

    /**
     * Use this method to submit a build to the thread which refresh their status.
     */
    public void submitWaitingBuild(int buildId) {
        Optional<Build> optionalBuild = RepairnatorConfig.getInstance().getJTravis().build().fromId(buildId);
        if (optionalBuild.isPresent()) {
            this.inspectBuilds.submitNewBuild(optionalBuild.get());
        }
    }
}
