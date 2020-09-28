package fr.inria.spirals.repairnator.realtime;

import fr.inria.jtravis.entities.Build;
import fr.inria.jtravis.entities.v2.BuildV2;
import fr.inria.jtravis.entities.v2.JobV2;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
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
    private BuildHelperV2 buildHelper;
    private RTScanner rtScanner;
    private SequencerCollector collector;

    private Set<Integer> collected = new HashSet<>();
    private Set<Integer> attempted = new HashSet<>();

    public static void main(String[] args) {
        setup();
        ZeroScanner scanner = new ZeroScanner();
        scanner.run();
    }

    static void setup(){
        //Setup repairnator config
        //repair tools
        Set<String> repairTools = new HashSet();
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

    }

    public ZeroScanner() {
        this.rtScanner = new RTScanner(UUID.randomUUID().toString());
        this.buildHelper = new BuildHelperV2(RepairnatorConfig.getInstance().getJTravis());
        this.collector = new SequencerCollector(SequencerConfig.getInstance().contextSize);
    }

    @Override
    public void run() {
        LOGGER.info("Starting alpha scanner...");

        try {
            collector.initialize();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        JobHelperv2 jobHelperv2 = new JobHelperv2(RepairnatorConfig.getInstance().getJTravis());

        final int scanBackIterations = 10;
        final int jumpSize = 250;

        while (true) {
            LOGGER.info("New scanning iteration");
            try {
                Optional<List<JobV2>> latestJobListOpt = jobHelperv2.allFromV2();
                List<JobV2> latestJobList = latestJobListOpt.get();
                int latestJobId = latestJobList.get(0).getId();
                for (int it = 0; it < scanBackIterations; ++it) {

                    List<JobV2> jobList = jobHelperv2.allSubSequentJobsFrom(latestJobId - (it*jumpSize));
                    for (JobV2 job : jobList) {
                        LOGGER.debug("Scanning job: " + job.getRepositorySlug() + " : " + job.getBuildId());
                        if (!isLanguage(job, "java")){
                            LOGGER.debug("Job is not in target language, skipping");
                            continue;
                        }

                        switch (job.getState()) {
                            case PASSED: { //collect for data
                                if (isListedJob(job, collected)) {
                                    LOGGER.debug("Job's changes already collected, skipping");
                                    continue;
                                }

                                collectJob(job.getBuildId(), job.getRepositorySlug());
                            }
                            break;
                            case FAILED: { //try to fix it
                                if (isListedJob(job, attempted)) {
                                    LOGGER.debug("Job fix already attempted, skipping");
                                    continue;
                                }

                                attemptJob(job.getBuildId());
                            }
                            break;
                            default:
                                LOGGER.debug("Job's state not handled:" + job.getState());
                            break;
                        }
                    }
                }
            } catch (OutOfMemoryError oom){
                LOGGER.error("Out of memory error: "  + oom.toString());
                rtScanner.stopDockerJobs();
                System.exit(-1);
            } catch (Exception e) {
                LOGGER.error("failed to get commit: "  + e.toString());
            }
        } // end while loop
    }


    protected void collectJob(int id, String slug){
        LOGGER.info("===== COLLECTING FOR DATA: " + id);

        Optional<BuildV2> build = buildHelper.fromIdV2(id);
        if (!build.isPresent()) return;

        String sha = build.get().getCommit().getSha();

        collector.handle(slug, sha);
        collected.add(id);
    }

    protected void attemptJob(int id){
        LOGGER.info("===== ATTEMPT REPAIR: " + id);

        Optional<Build> build = buildHelper.fromId(id);
        if (!build.isPresent()) return;

        this.rtScanner.submitBuildToExecution(build.get());

        attempted.add(id);
    }

    private boolean isListedJob(JobV2 job, Set<Integer> set){
        return set.contains(job.getBuildId());
    }

    private boolean isLanguage(JobV2 job, String language){
        return language.equals(job.getConfig().getLanguage());
    }
}
