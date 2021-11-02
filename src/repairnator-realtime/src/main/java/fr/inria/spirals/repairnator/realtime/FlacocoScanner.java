package fr.inria.spirals.repairnator.realtime;

import fr.inria.spirals.repairnator.GithubInputBuild;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.realtime.githubapi.commits.models.SelectedPullRequest;
import fr.inria.spirals.repairnator.states.LauncherMode;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Scanner that collects open pull requests associated with
 * a list of given GitHub repositories.
 */
public class FlacocoScanner implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlacocoScanner.class);
    private static final Long SCAN_INTERVAL = 15L; // 15 minutes
    private static final int EXECUTION_TIME = 14; // 14 days
    private GithubPullRequestScanner scanner;
    private DockerPipelineRunner runner;
    private static ScheduledExecutorService executor;
    private static Date scannerEndsAt;
    private boolean firstIteration = true;

    public static void main(String[] args) {
        setup();
        FlacocoScanner scanner = new FlacocoScanner();
        Date scannerStartedAt = new Date();
        scannerEndsAt = DateUtils.addDays(scannerStartedAt, EXECUTION_TIME);

        LOGGER.info("Starting Flacoco scanner...");
        executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate (scanner, 0L, SCAN_INTERVAL, TimeUnit.MINUTES);
    }

    static void setup(){
        //concurrent repair job
        RepairnatorConfig.getInstance().setNbThreads(16);

        //pipeline mode
        RepairnatorConfig.getInstance().setPipelineMode(RepairnatorConfig.PIPELINE_MODE.DOCKER.name());

        //github oauth
        RepairnatorConfig.getInstance().setGithubToken(System.getenv("GITHUB_OAUTH"));

        //pipeline image tag
        RepairnatorConfig.getInstance().setDockerImageName("repairnator/pipeline:3.4");

        //launcher mode
        RepairnatorConfig.getInstance().setLauncherMode(LauncherMode.FAULT_LOCALIZATION);

        RepairnatorConfig.getInstance().setOutputPath("/tmp");
    }

    public FlacocoScanner() {
        ClassLoader classLoader = FlacocoScanner.class.getClassLoader();
        // TODO: parametrize this path?
        File file = new File("flacocobot_projects_to_scan.txt");

        try {
            Set<String> repos = getFileContent(file);
            this.scanner = new GithubPullRequestScanner(GithubPullRequestScanner.FetchMode.FAILED, repos);
            this.runner = new DockerPipelineRunner(UUID.randomUUID().toString());
            runner.initRunner();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Date currentDate = new Date();

        if (currentDate.before(scannerEndsAt)) {
            LOGGER.info("New scanning iteration");
            try {
                List<SelectedPullRequest> latestJobList;
                if (firstIteration) {
                    latestJobList = scanner.fetch(0);
                    firstIteration = false;
                } else {
                    latestJobList = scanner.fetch(DateUtils.addMinutes(currentDate, -SCAN_INTERVAL.intValue()).getTime());
                }

                for (SelectedPullRequest job : latestJobList) {
                    LOGGER.debug("Scanning job: " + job.getRepoName() + " commit: " + job.getHeadCommitSHA1());
                    System.out.println(job);
                    attemptJob(job);
                }
            } catch (OutOfMemoryError oom) {
                LOGGER.error("Out of memory error: " + oom);
                //runner.switchOff();
                System.exit(-1);
            } catch (Exception e) {
                LOGGER.error("Failed to get commit: " + e);
            }
        } else {
            executor.shutdown();
            System.exit(0);
        }
    }

    protected void attemptJob(SelectedPullRequest job){
        LOGGER.info("===== ATTEMPT LOCALIZATION: " + job.getRepoName() + " - #" + job.getNumber());
        runner.submitBuild(new GithubInputBuild("https://github.com/" + job.getRepoName(), null, job.getHeadCommitSHA1(), job.getNumber()));
    }

    private Set<String> getFileContent(File file) throws IOException {
        HashSet<String> result = new HashSet<>();

        BufferedReader reader = new BufferedReader(new FileReader(file));
        while (reader.ready()) {
            result.add(reader.readLine().trim());
        }
        return result;
    }
}
