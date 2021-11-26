package fr.inria.spirals.repairnator.realtime;

import fr.inria.spirals.repairnator.GithubInputBuild;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.realtime.githubapi.commits.GithubAPICommitAdapter;
import fr.inria.spirals.repairnator.realtime.githubapi.commits.models.SelectedCommit;
import fr.inria.spirals.repairnator.states.LauncherMode;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static fr.inria.spirals.repairnator.realtime.Constants.SEQUENCER_NAME;
import static fr.inria.spirals.repairnator.realtime.Constants.SORALD_NAME;

public class GithubScanner {
    static long scanIntervalDelay = 60 * 60 * 1000; // 1 hour
    static long scanIntervalLength = 60 * 60 * 1000; // 1 hour
    static long frequency = 60 * 60 * 1000; // 1 hour

    long lastFetchedTime = -1L;
    long scanStartTime = 0;

    PipelineRunner runner;

    FetchMode fetchMode;
    Set<String> repos;

    public static void main(String[] args) throws IOException {
        Set<String> repos = null;
        String reposPath = System.getenv("REPOS_PATH");
        if (reposPath != null)
            repos = new HashSet<>(FileUtils.readLines(new File(reposPath), "UTF-8"));

        FetchMode fetchMode = parseFetchMode();

        GithubScanner scanner = new GithubScanner(fetchMode, repos);
        scanner.setup();

        while (true) {
            try {
                List<SelectedCommit> selectedCommits = scanner.fetch();

                selectedCommits.forEach(scanner::process);

                TimeUnit.MILLISECONDS.sleep(frequency);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public GithubScanner(FetchMode fetchMode, Set<String> repos) {
        this.fetchMode = fetchMode;
        this.repos = repos;
    }

    public GithubScanner(FetchMode fetchMode) {
        this(fetchMode, null);
    }

    public List<SelectedCommit> fetch() throws Exception {
        long endTime = System.currentTimeMillis() - scanIntervalDelay;
//        long startTime = endTime - scanIntervalLength;
        long startTime = lastFetchedTime < 0 ? scanStartTime : lastFetchedTime;

        List<SelectedCommit> commits =  fetch(startTime, endTime);
        lastFetchedTime = endTime;
        return commits;
    }

    public void setup(){
        Set<String> repairTools = new HashSet();
        String repairTool = getEnvOrDefault("REPAIR_TOOL", SEQUENCER_NAME);
        repairTools.add(repairTool);
        RepairnatorConfig.getInstance().setRepairTools(repairTools);

        RepairnatorConfig.getInstance().setGithubToken(System.getenv("GITHUB_OAUTH"));

        if(repairTool.equals(SORALD_NAME)) {
            runner = new SimplePipelineRunner();
        } else if(repairTool.equals(SEQUENCER_NAME)) {
            RepairnatorConfig.getInstance().setLauncherMode(LauncherMode.SEQUENCER_REPAIR);

            RepairnatorConfig.getInstance().setNbThreads(16);

            RepairnatorConfig.getInstance().setPipelineMode(RepairnatorConfig.PIPELINE_MODE.DOCKER.name());
            RepairnatorConfig.getInstance().setDockerImageName(System.getenv("DOCKER_IMAGE_NAME"));
            runner = new DockerPipelineRunner();
        }

        runner.initRunner();

        try {
            if(System.getenv().containsKey("SCAN_START_TIME"))
                scanStartTime = new SimpleDateFormat("MM/dd/yyyy")
                        .parse(System.getenv("SCAN_START_TIME")).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public List<SelectedCommit> fetch(long startTime, long endTime) throws Exception {
        return GithubAPICommitAdapter.getInstance().getSelectedCommits(startTime, endTime, fetchMode, repos);
    }

    public void process(SelectedCommit commit) {
        String url = "https://github.com/" + commit.getRepoName();
        String sha = commit.getCommitId();
        runner.submitBuild(new GithubInputBuild(url, null, sha));
    }

    private static String getEnvOrDefault(String name, String dfault) {
        String env = System.getenv(name);
        if (env == null || env.equals(""))
            return dfault;

        return env;
    }

    private static FetchMode parseFetchMode() {
        String value = getEnvOrDefault("FETCH_MODE", "failed");
        switch (value) {
            case "all":
                return FetchMode.ALL;
            case "passing":
                return FetchMode.PASSING;
            case "failed":
            default:
                return FetchMode.FAILED;
        }
    }

    public enum FetchMode {
        FAILED, ALL, PASSING
    }
}
