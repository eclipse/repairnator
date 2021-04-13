package fr.inria.spirals.repairnator.realtime;

import fr.inria.spirals.repairnator.GithubInputBuild;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.config.SequencerConfig;
import fr.inria.spirals.repairnator.realtime.githubapi.commits.models.SelectedCommit;
import fr.inria.spirals.repairnator.states.LauncherMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 *  Scanner based on FastScanner.java and SequencerLearnerScanner.java
 *  The purpose is to both collect one-line changes from passing builds
 *  and try to repair failing builds.
 *
 *  @author Javier Ron
 */

public class ZeroScanner implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZeroScanner.class);
    private GithubScanner scanner;
    private SequencerCollector collector;
    private DockerPipelineRunner runner;

    private Set<String> collected = new HashSet<>();
    private Set<String> attempted = new HashSet<>();

    public static void main(String[] args) {
        setup();
        ZeroScanner scanner = new ZeroScanner();
        scanner.run();
    }

    static void setup(){
        //Setup repairnator config
        //repair tools
        HashSet<String> repairTools = new HashSet<>();
        repairTools.add("SequencerRepair");
        RepairnatorConfig.getInstance().setRepairTools(repairTools);

        //concurrent repair job
        RepairnatorConfig.getInstance().setNbThreads(16);

        //pipeline mode
        RepairnatorConfig.getInstance().setPipelineMode(RepairnatorConfig.PIPELINE_MODE.DOCKER.name());

        //github oauth
        RepairnatorConfig.getInstance().setGithubToken(System.getenv("GITHUB_OAUTH"));

        //pipeline image tag
        RepairnatorConfig.getInstance().setDockerImageName("repairnator/pipeline:latest");

        //launcher mode
        RepairnatorConfig.getInstance().setLauncherMode(LauncherMode.SEQUENCER_REPAIR);

        RepairnatorConfig.getInstance().setOutputPath("/tmp");

    }

    public ZeroScanner() {
        this.scanner = new GithubScanner(GithubScanner.FetchMode.ALL);
        this.collector = new SequencerCollector(SequencerConfig.getInstance().contextSize);
        this.runner = new DockerPipelineRunner(UUID.randomUUID().toString());
        runner.initRunner();
    }

    @Override
    public void run() {
        LOGGER.info("Starting alpha scanner...");

        try {
            collector.initialize();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        while (true) {
            LOGGER.info("New scanning iteration");
            try {
                List<SelectedCommit> latestJobList = scanner.fetch();
                
                for (SelectedCommit job : latestJobList) {
                    LOGGER.debug("Scanning job: " + job.getRepoName() + " commit: " + job.getCommitId());

                    //switch (job.getGithubActionsFailed()) {
                    if (job.getGithubActionsFailed()) { // build failed
                        if (isListedJob(job, attempted)) {
                            LOGGER.debug("Job fix already attempted, skipping");
                            continue;
                        }
                        attemptJob(job);
                    } else { // build passed
                        if (isListedJob(job, collected)) {
                            LOGGER.debug("Job's changes already collected, skipping");
                            continue;
                        }
                        collectJob(job);
                    }
                }
            } catch (OutOfMemoryError oom){
                LOGGER.error("Out of memory error: "  + oom.toString());
                runner.switchOff();
                System.exit(-1);
            } catch (Exception e) {
                LOGGER.error("failed to get commit: "  + e.toString());
            }
        } // end while loop
    }


    protected void collectJob(SelectedCommit job){
        LOGGER.info("===== COLLECTING FOR DATA: " + job.getRepoName() + "-" + job.getCommitId());

        collector.handle(job.getRepoName(), job.getCommitId());
        collected.add(job.getRepoName() + "-" + job.getCommitId());
    }

    protected void attemptJob(SelectedCommit job){
        LOGGER.info("===== ATTEMPT REPAIR: " + job.getRepoName() + "-" + job.getCommitId());
        runner.submitBuild(new GithubInputBuild(job.getRepoName(), null, job.getCommitId()));
        attempted.add(job.getRepoName() + "-" +job.getCommitId());
    }

    private boolean isListedJob(SelectedCommit job, Set<String> set){
        return set.contains(job.getRepoName() + "-" + job.getCommitId());
    }
}
