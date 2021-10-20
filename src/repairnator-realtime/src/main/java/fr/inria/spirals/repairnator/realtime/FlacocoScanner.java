package fr.inria.spirals.repairnator.realtime;

import fr.inria.spirals.repairnator.GithubInputBuild;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.realtime.githubapi.commits.models.SelectedCommit;
import fr.inria.spirals.repairnator.states.LauncherMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class FlacocoScanner implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlacocoScanner.class);
    private final GithubPullRequestScanner scanner;
    private final DockerPipelineRunner runner;

    private final Set<String> attempted = new HashSet<>();

    public static void main(String[] args) {
        setup();
        FlacocoScanner scanner = new FlacocoScanner();
        scanner.run();
    }

    static void setup(){
        //concurrent repair job
        RepairnatorConfig.getInstance().setNbThreads(16);

        //pipeline mode
        RepairnatorConfig.getInstance().setPipelineMode(RepairnatorConfig.PIPELINE_MODE.DOCKER.name());

        //github oauth
        RepairnatorConfig.getInstance().setGithubToken(System.getenv("GITHUB_OAUTH"));

        //pipeline image tag
        RepairnatorConfig.getInstance().setDockerImageName("repairnator/pipeline:latest");

        //launcher mode
        RepairnatorConfig.getInstance().setLauncherMode(LauncherMode.FAULT_LOCALIZATION);

        RepairnatorConfig.getInstance().setOutputPath("/tmp");

    }

    public FlacocoScanner() {
        this.scanner = new GithubPullRequestScanner(GithubScanner.FetchMode.FAILED);
        this.runner = new DockerPipelineRunner(UUID.randomUUID().toString());
        runner.initRunner();
    }

    @Override
    public void run() {
        LOGGER.info("Starting alpha scanner...");

        while (true) {
            LOGGER.info("New scanning iteration");
            try {
                List<SelectedPullRequest> latestJobList = scanner.fetch();

                for (SelectedPullRequest job : latestJobList) {
                    LOGGER.debug("Scanning job: " + job.getRepoName() + " commit: " + job.getCommitId());

                    //switch (job.getGithubActionsFailed()) {
                    if (job.getGithubActionsFailed()) { // build failed
                        if (isListedJob(job, attempted)) {
                            LOGGER.debug("Job fix already attempted, skipping");
                            continue;
                        }
                        attemptJob(job);
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

    protected void attemptJob(SelectedPullRequest job){
        LOGGER.info("===== ATTEMPT REPAIR: " + job.getRepoName() + "-" + job.getCommitId());
        //runner.submitBuild(new GithubInputBuild(job.getRepoName(), null, job.getCommitId()));
        attempted.add(job.getRepoName() + "-" +job.getCommitId());
    }

    private boolean isListedJob(SelectedCommit job, Set<String> set){
        return set.contains(job.getRepoName() + "-" + job.getCommitId());
    }

}
