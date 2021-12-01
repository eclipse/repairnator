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

    // It is used at the beginning for scanning pull requests between current date and 7 days before
    private static final int DAYS_BEFORE_CURRENT_DATE = 7;

    private GithubPullRequestScanner scanner;
    private DockerPipelineRunner runner;
    private static ScheduledExecutorService executor;

    private HashMap<String, RepositoryScanInformation> reposToScanHashMap;
    private List<String> reposScanEndedList;
    private Date lastScanEnded;

    public static void main(String[] args) {
        setup();
        FlacocoScanner scanner = new FlacocoScanner();

        LOGGER.info("Starting Flacoco scanner...");
        executor = Executors.newScheduledThreadPool(1);
        executor.scheduleWithFixedDelay(scanner, 0L, SCAN_INTERVAL, TimeUnit.MINUTES);
    }

    static void setup() {
        //concurrent repair job
        RepairnatorConfig.getInstance().setNbThreads(16);

        //pipeline mode
        RepairnatorConfig.getInstance().setPipelineMode(RepairnatorConfig.PIPELINE_MODE.DOCKER.name());

        //github oauth
        RepairnatorConfig.getInstance().setGithubToken(System.getenv("GITHUB_OAUTH"));

        //file with the list of projects to scan
        RepairnatorConfig.getInstance().setProjectsToScan(new File(System.getenv("PROJECTS_TO_SCAN_FILE")));

        RepairnatorConfig.getInstance().setFlacocoThreshold(Double.valueOf(System.getenv("FLACOCO_THRESHOLD")));

        //pipeline image tag
        RepairnatorConfig.getInstance().setDockerImageName("repairnator/pipeline:3.4");

        //launcher mode
        RepairnatorConfig.getInstance().setLauncherMode(LauncherMode.FAULT_LOCALIZATION);

        RepairnatorConfig.getInstance().setOutputPath("/tmp");
    }

    public FlacocoScanner() {
        ClassLoader classLoader = FlacocoScanner.class.getClassLoader();
    }

    @Override
    public void run() {

        Date currentDate = new Date();
        LOGGER.info("Current date: " + currentDate);
        File file = RepairnatorConfig.getInstance().getProjectsToScan();

        try {
            List<String> reposFromFile = getFileContent(file);

            // Fist iteration
            if (reposToScanHashMap == null) {
                reposToScanHashMap = new HashMap<>();
                reposScanEndedList = new ArrayList<>();
                reposFromFile.forEach(repo -> {
                    reposToScanHashMap.put(repo,
                            new RepositoryScanInformation(DateUtils.addDays(currentDate, -DAYS_BEFORE_CURRENT_DATE), DateUtils.addDays(currentDate, EXECUTION_TIME), true));
                });
            } else {
                // Detect new projects added to the file PROJECTS_TO_SCAN_FILE
                // after starting the scanner and scan them
                reposFromFile.forEach(repo -> {
                    if (!reposToScanHashMap.containsKey(repo) && !reposScanEndedList.contains(repo)) {
                        reposToScanHashMap.put(repo,
                                new RepositoryScanInformation(DateUtils.addDays(currentDate, -DAYS_BEFORE_CURRENT_DATE), DateUtils.addDays(currentDate, EXECUTION_TIME), true));
                    }
                });
                // Stop scanning projects removed from the file PROJECTS_TO_SCAN_FILE
                reposToScanHashMap.entrySet().removeIf(repo -> !(reposFromFile.contains(repo.getKey())));
            }

            // Stop scanning projects for which the scan end time has been reached
            reposToScanHashMap.forEach((repo, projectInfo) -> {
                LOGGER.info("Project to scan: " + repo + " " + projectInfo);
            });

            this.scanner = new GithubPullRequestScanner(GithubPullRequestScanner.FetchMode.FAILED, reposToScanHashMap.keySet());
            this.runner = new DockerPipelineRunner(UUID.randomUUID().toString());
            runner.initRunner();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (String repoName : reposToScanHashMap.keySet()) {
            if (currentDate.before(reposToScanHashMap.get(repoName).getScanEndsAt())) {
                try {
                    List<SelectedPullRequest> latestJobList;
                    if (reposToScanHashMap.get(repoName).isFirstScan()) {
                        reposToScanHashMap.get(repoName).setFirstScan(false);
                        latestJobList = scanner.fetch(reposToScanHashMap.get(repoName).getStartDateForScanning().getTime(),
                                currentDate.getTime(), repoName, true);
                    } else {
                        Date newDate = DateUtils.addMinutes(currentDate, -SCAN_INTERVAL.intValue());
                        latestJobList = scanner.fetch(reposToScanHashMap.get(repoName).getStartDateForScanning().getTime(),
                                newDate.getTime(), repoName, false);
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
                LOGGER.info("Scan time for the project " + repoName + " has been ended");
                reposToScanHashMap.remove(repoName);
                reposScanEndedList.add(repoName);
            }
        }

        if (reposToScanHashMap.isEmpty()) {
            if (lastScanEnded == null) {
                lastScanEnded = currentDate;
            }
        }

        if (lastScanEnded != null) {
            Date extraTimeDate = DateUtils.addMinutes(lastScanEnded, Math.toIntExact(SCAN_INTERVAL));
            if (currentDate.after(extraTimeDate)) {
                executor.shutdown();
                System.exit(0);
            }
        }
    }

    protected void attemptJob(SelectedPullRequest job){
        LOGGER.info("===== ATTEMPT LOCALIZATION: " + job.getRepoName() + " - #" + job.getNumber());
        runner.submitBuild(new GithubInputBuild("https://github.com/" + job.getRepoName(), null, job.getHeadCommitSHA1(), job.getNumber()));
    }

    private List<String> getFileContent(File file) throws IOException {
        List<String> result = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new FileReader(file));
        while (reader.ready()) {
            String line = reader.readLine();
            if (!line.isEmpty()) {
                result.add(line);
            }
        }
        return result;
    }
}
