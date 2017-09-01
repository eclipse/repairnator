import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.HttpException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by urli on 01/09/2017.
 */
public class EvaluatePotentialBug {
    private static final String[] potentialBugEvidence = new String[] { "bug", "fix", "patch" };

    private String jsonLocation;
    private String githubLogin;
    private String githubToken;

    public EvaluatePotentialBug(String jsonLocation, String githubLogin, String githubToken) {
        this.jsonLocation = jsonLocation;
        this.githubLogin = githubLogin;
        this.githubToken = githubToken;
    }

    public Map<Integer, Collection<RepairInfo>> computeAllScores() {
        try {
            Collection<File> allJson = getJsonFromDirectory(this.jsonLocation);
            Collection<RepairInfo> allRepairInfo = getRepairInfoFromFiles(allJson);
            return this.computeScoring(allRepairInfo);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void printScoreResults(Map<Integer, Collection<RepairInfo>> results) {
        if (results != null) {
            List<Integer> integers = new ArrayList<>(results.keySet());
            Collections.sort(integers);

            for (int score : integers) {
                System.out.println("Obtained score : "+score);
                System.out.println("Concerned branches: ");

                for (RepairInfo repairInfo : results.get(score)) {
                    System.out.println("\t "+repairInfo.getPushBranchName());
                }
                System.out.println();
            }
        }
    }

    private int computeScore(RepairInfo repairInfo) throws IOException {
        int score = 0;
        GitHub gitHub = GitHubBuilder.fromEnvironment().withOAuthToken(this.githubToken, this.githubLogin).build();

        GHRepository ghRepo = gitHub.getRepository(repairInfo.getGithubProject());
        String commitMsg = ghRepo.getCommit(repairInfo.getPatchCommit()).getCommitShortInfo().getMessage().toLowerCase();

        for (String s : potentialBugEvidence) {
            if (commitMsg.contains(s)) {
                score += 10;
            }
        }

        if (repairInfo.getPrId() != null) {
            GHPullRequest pullRequest = ghRepo.getPullRequest(Integer.parseInt(repairInfo.getPrId()));

            for (String s : potentialBugEvidence) {
                if (pullRequest.getTitle().toLowerCase().contains(s)) {
                    score += 100;
                }
            }

            try {
                for (GHLabel label : pullRequest.getLabels()) {
                    for (String s : potentialBugEvidence) {
                        if (label.getName().toLowerCase().contains(s)) {
                            score += 100;
                        }
                    }
                }
            } catch (HttpException e) {
            }


            for (GHIssueComment comment : pullRequest.getComments()) {
                for (String s : potentialBugEvidence) {
                    if (comment.getBody().toLowerCase().contains(s)) {
                        if (comment.getUser().equals(pullRequest.getUser())) {
                            score += 10;
                        } else {
                            score += 1;
                        }
                    }
                }
            }
        }

        return score;
    }

    private Map<Integer, Collection<RepairInfo>> computeScoring(Collection<RepairInfo> allRepairinfo) {
        Map<Integer, Collection<RepairInfo>> result = new HashMap<>();

        for (RepairInfo repairInfo : allRepairinfo) {
            try {
                int score = this.computeScore(repairInfo);

                if (!result.containsKey(score)) {
                    result.put(score, new ArrayList<>());
                }
                Collection<RepairInfo> infos = result.get(score);
                infos.add(repairInfo);
            } catch (IOException ioe) {
                System.err.println("Error while computing repairInfo score");
                ioe.printStackTrace();
            }
        }

        return result;
    }

    private RepairInfo createRepairInfoFromJson(JSONObject json) {
        RepairInfo result = new RepairInfo();

        result.setGithubProject((String)json.get("repo"));

        JSONObject metrics = (JSONObject) json.get("metrics");
        result.setBugCommit((String)metrics.get("BugCommit"));
        result.setPatchCommit((String)metrics.get("PatchCommit"));
        if (json.containsKey("pr-id")) {
            result.setPrId(json.get("pr-id").toString());
        }

        return result;
    }

    private Collection<RepairInfo> getRepairInfoFromFiles(Collection<File> jsonFiles) throws IOException, ParseException {
        List<RepairInfo> result = new ArrayList<>();

        for (File f : jsonFiles) {
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(new FileReader(f));
            RepairInfo repairInfo = createRepairInfoFromJson(json);
            if (repairInfo.getPatchCommit() != null) {
                repairInfo.setPushBranchName(f.getName());
                result.add(repairInfo);
            }
        }

        return result;
    }

    private Collection<File> getJsonFromDirectory(String path) throws IOException {
        List<File> result = new ArrayList<>();
        File file = new File(path);

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**.json");

        for (Path p : Files.newDirectoryStream(file.toPath())) {
            if (p.toFile().isFile() && matcher.matches(p)) {
                result.add(p.toFile());
            }
        }

        return result;
    }

    public static void main(String[] args) {
        String jsonLocation = args[0];
        String githubLogin = args[1];
        String githubOauth = args[2];

        EvaluatePotentialBug evaluatePotentialBug = new EvaluatePotentialBug(jsonLocation, githubLogin, githubOauth);
        evaluatePotentialBug.printScoreResults(evaluatePotentialBug.computeAllScores());
    }
}
