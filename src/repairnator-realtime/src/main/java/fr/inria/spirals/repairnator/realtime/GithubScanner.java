package fr.inria.spirals.repairnator.realtime;

import fr.inria.spirals.repairnator.GithubInputBuild;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.realtime.githubapi.commits.GithubAPICommitAdapter;
import fr.inria.spirals.repairnator.realtime.githubapi.commits.models.SelectedCommit;
import fr.inria.spirals.repairnator.states.LauncherMode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GithubScanner {
    static long scanIntervalDelay = 60 * 60 * 1000; // 1 hour
    static long scanIntervalLength = 60 * 60 * 1000; // 1 hour
    static long frequency = 60 * 60 * 1000; // 1 hour

    DockerPipelineRunner runner;

    FetchMode fetchMode;

    public static void main(String[] args) {

        Set<String> repairTools = new HashSet();
        repairTools.add("SequencerRepair");
        RepairnatorConfig.getInstance().setRepairTools(repairTools);
        RepairnatorConfig.getInstance().setNbThreads(16);
        RepairnatorConfig.getInstance().setPipelineMode(RepairnatorConfig.PIPELINE_MODE.DOCKER.name());
        RepairnatorConfig.getInstance().setGithubToken(System.getenv("GITHUB_OAUTH"));
        RepairnatorConfig.getInstance().setDockerImageName("repairnator/pipeline:latest");
        RepairnatorConfig.getInstance().setLauncherMode(LauncherMode.SEQUENCER_REPAIR);
        String fetchModeStr = System.getenv("FETCH_MODE");

        FetchMode fetchMode = FetchMode.FAILED; // failed is the default mode
        if(fetchModeStr.equals(FetchMode.ALL.toString())){
            fetchMode = FetchMode.ALL;
        }

        GithubScanner scanner = new GithubScanner(fetchMode);

        while (true){
            try {
                List<SelectedCommit> selectedCommits = scanner.fetch();

                selectedCommits.forEach(scanner::process);

                TimeUnit.MILLISECONDS.sleep(frequency);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public GithubScanner(FetchMode fetchMode){
        this.fetchMode = fetchMode;

        runner = new DockerPipelineRunner();
        runner.initRunner();
    }

    public List<SelectedCommit> fetch() throws Exception{
        long endTime = System.currentTimeMillis() - scanIntervalDelay;
        long startTime = endTime - scanIntervalLength;

        List<SelectedCommit> commits =
                GithubAPICommitAdapter.getInstance().getSelectedCommits(startTime, endTime, fetchMode);

        return commits.stream().filter(commit -> !commit.getTravisFailed()).collect(Collectors.toList());
    }

    public void process(SelectedCommit commit){
        String url = "https://github.com/" + commit.getRepoName();
        String sha = commit.getCommitId();

        System.out.println(url);
        System.out.println(sha);

        runner.submitBuild(new GithubInputBuild(url, sha));
    }

    public enum FetchMode{
        FAILED, ALL;
    }
}
