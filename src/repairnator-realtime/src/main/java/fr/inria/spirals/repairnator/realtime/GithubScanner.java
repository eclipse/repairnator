package fr.inria.spirals.repairnator.realtime;

import fr.inria.spirals.repairnator.GithubInputBuild;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.step.feedback.sobo.SoboAdapter;
import fr.inria.spirals.repairnator.realtime.githubapi.commits.GithubAPICommitAdapter;
import fr.inria.spirals.repairnator.realtime.githubapi.commits.models.SelectedCommit;
import fr.inria.spirals.repairnator.realtime.utils.SOBOUtils;
import fr.inria.spirals.repairnator.states.LauncherMode;
import org.apache.commons.io.FileUtils;
import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static fr.inria.spirals.repairnator.realtime.Constants.SEQUENCER_NAME;
import static fr.inria.spirals.repairnator.realtime.Constants.SORALD_NAME;

public class GithubScanner {
    private final static Logger logger = LoggerFactory.getLogger(GithubScanner.class);
    static long scanIntervalDelay = 60 * 60 * 1000; // 1 hour
    static long  frequency = 60 * 60 * 1000; // 1 hour

    long lastFetchedTime = -1L;
    long scanStartTime = 0;

    PipelineRunner runner;

    public GithubScanner(){
        setup();
    }

    public static void main(String[] args) throws IOException {
        GithubScanner scanner = new GithubScanner();

        String reposPath = System.getenv("REPOS_PATH");
        if (reposPath != null) {
            // a list of repos to be monitored online is provided
            Set<String> repos = new HashSet<>(FileUtils.readLines(new File(reposPath), "UTF-8"));

            if(System.getenv("launcherMode").equals("FEEDBACK") && System.getenv("command").equals("true") ){

                frequency=Long.parseLong(getEnvOrDefault("commandFrequency","10000"));
                scanIntervalDelay=Long.parseLong(getEnvOrDefault("commandFrequency","10000"));
                scanner.fetchAndProcessCommandsPeriodically(repos);
            }else{
                frequency=Long.parseLong(getEnvOrDefault("commandFrequency","30000"));
                scanIntervalDelay=Long.parseLong(getEnvOrDefault("commandFrequency","30000"));
                FetchMode fetchMode = parseFetchMode();
                // here is how we send the line to check the repos
                scanner.fetchAndProcessCommitsPeriodically(repos, fetchMode);}
        } else {
            List<SelectedCommit> selectedCommits = readSelectedCommitsFromFile();
            scanner.processSelectedCommits(selectedCommits);
        }
    }

    private void fetchAndProcessCommandsPeriodically(Set<String> repos) {
        while (true) {
            try {
                List<GHIssueComment> CommandIssues = fetchCommands( repos);

                logger.info("fetched commands: ");
                for (GHIssueComment commandIssue: CommandIssues) {
                    System.out.println(commandIssue.getBody());
                }

                TimeUnit.MILLISECONDS.sleep(Integer.parseInt(getEnvOrDefault("commandFrequency","10000")));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private List<GHIssueComment> fetchCommands(Set<String> repos) throws IOException {
        List<GHIssueComment> issueComments = new ArrayList<>();
        int i=0;
        for (String repo : repos) {
            String user = SOBOUtils.getUserName(repo);
            String task = SOBOUtils.getTask(repo);
            GHIssue issue= SoboAdapter.getInstance("").getCommandIssue(repo, user, logger);
            if (issue!= null) {
                try {
                    GHIssueComment lastComment= SoboAdapter.getInstance("").getLastCommand(issue.getComments());
                    logger.info(i+" "+issue.getRepository().getName());
                    if (lastComment!=null){
                        SoboAdapter.getInstance("").analyzeCommand(user,repo,task,logger,lastComment,issue);
                    }
                }catch (Exception e) {
                    logger.info(i+" "+"Unable to get the last Comment - "+issue.getRepository().getFullName());
                }
            }
            i++;
        }
        return issueComments;
    }


    private void fetchAndProcessCommitsPeriodically(Set<String> repos, FetchMode fetchMode) {
        while (true) {
            try {
                List<SelectedCommit> selectedCommits = fetch(fetchMode, repos);

                logger.info("fetched commits: ");
                selectedCommits.forEach(c -> System.out.println(c.getRepoName() + " " + c.getCommitId()
                        + " " + c.getGithubActionsFailed()));

                processSelectedCommits(selectedCommits);

                TimeUnit.MILLISECONDS.sleep((Integer.parseInt(getEnvOrDefault("commandFrequency","60000"))));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    //TODO: CREATE THE FEEDBACK PATH TO CONNECT THE ELEMENTS
    private void processSelectedCommits(List<SelectedCommit> selectedCommits) {

        for (int i = selectedCommits.size()-1; i >-1 ; i--) {
            SelectedCommit commit = selectedCommits.get(i);
            logger.info("Commit being submitted to the repair pipeline: " + commit.getCommitUrl() + " "
                    + commit.getCommitId() + "; " + (i + 1) + " out of " + selectedCommits.size());
            process(commit);
        }
    }

    private static List<SelectedCommit> readSelectedCommitsFromFile() throws IOException {
        String selectedCommitsPath = System.getenv("SELECTED_COMMITS_PATH");
        return FileUtils.readLines(new File(selectedCommitsPath), "UTF-8").stream()
                .map(c -> new SelectedCommit(false, c.split(",")[1], c.split(",")[0]))
                .collect(Collectors.toList());
    }

    // If you want to monitor... certain commits in a period of time you used this,
    // in SOBO we will not have the list of commits, we will find for the new commits
    public List<SelectedCommit> fetch(FetchMode fetchMode, Set<String> repos) throws Exception {
        long endTime = System.currentTimeMillis() - scanIntervalDelay;
        long startTime = lastFetchedTime < 0 ? scanStartTime : lastFetchedTime;

        List<SelectedCommit> commits = fetch(startTime, endTime, fetchMode, repos);
        lastFetchedTime = endTime;
        return commits;
    }

    public void setup() {
        Set<String> repairTools = new HashSet<>();
        Set<String> feedbackTools = new HashSet<>();
        String launcherMode=getEnvOrDefault("launcherMode", "REPAIR");
        RepairnatorConfig.getInstance().setGithubToken(System.getenv("GITHUB_OAUTH"));

        if (launcherMode.equals("FEEDBACK")){
            String feedbackTool = getEnvOrDefault("FEEDBACK_TOOL", "SoboBot");
            feedbackTools.add(feedbackTool);
            RepairnatorConfig.getInstance().setLauncherMode(LauncherMode.FEEDBACK);
            RepairnatorConfig.getInstance().setFeedbackTools(feedbackTools);
            runner = new SimplePipelineRunner();
            if (!System.getenv("command").equals("true") ) runner.initRunner();
        }else{
            String repairTool = getEnvOrDefault("REPAIR_TOOL", SEQUENCER_NAME);
            repairTools.add(repairTool);
            RepairnatorConfig.getInstance().setRepairTools(repairTools);
            if (repairTool.equals(SORALD_NAME)) {
                runner = new SimplePipelineRunner();
            } else if (repairTool.equals(SEQUENCER_NAME)) {
                RepairnatorConfig.getInstance().setLauncherMode(LauncherMode.SEQUENCER_REPAIR);

                RepairnatorConfig.getInstance().setNbThreads(16);

                RepairnatorConfig.getInstance().setPipelineMode(RepairnatorConfig.PIPELINE_MODE.DOCKER.name());
                RepairnatorConfig.getInstance().setDockerImageName(System.getenv("DOCKER_IMAGE_NAME"));
                runner = new DockerPipelineRunner();
            }

            runner.initRunner();

        }

        try {
            if (System.getenv().containsKey("SCAN_START_TIME"))
                scanStartTime = new SimpleDateFormat("MM/dd/yyyy")
                        .parse(System.getenv("SCAN_START_TIME")).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }


    public List<SelectedCommit> fetch(long startTime, long endTime, FetchMode fetchMode, Set<String> repos)
            throws Exception {
        return GithubAPICommitAdapter.getInstance().getSelectedCommits(startTime, endTime, fetchMode, repos);
    }

    public void process(SelectedCommit commit) {
        String sha = commit.getCommitId();
        if(System.getenv("FEEDBACK_TOOL").equals("SoboBot")){
            String url = "https://gits-15.sys.kth.se/" + commit.getRepoName();
            runner.submitBuild(new GithubInputBuild(url, null, sha));

        }else{
        String url = "https://github.com/" + commit.getRepoName();

        runner.submitBuild(new GithubInputBuild(url, null, sha));}
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
