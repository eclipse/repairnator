import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.HttpException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by urli on 01/09/2017.
 */
public class EvaluatePotentialBug {
    private static final String[] potentialBugEvidence = new String[] { "bug", "fix", "patch", "error", "exception" };
    private static final String[] potentialRefactoringEvidence = new String[] { "feature", "refactor", "typo", "add" };
    private static final Pattern ISSUE_PATTERN = Pattern.compile("#[0-9]+");

    private String jsonLocation;
    private String githubLogin;
    private String githubToken;

    public EvaluatePotentialBug(String jsonLocation, String githubLogin, String githubToken) {
        this.jsonLocation = jsonLocation;
        this.githubLogin = githubLogin;
        this.githubToken = githubToken;
    }

    private String getBranchnameFromFile(File f) {
        String name = f.getName();
        String[] parts = name.split("_repairnator");
        return parts[0];
    }

    public Map<Integer, Collection<RepairInfo>> computeAllScores(List<String> interestingBranches) {
        try {
            Collection<File> allJson = getJsonFromDirectory(this.jsonLocation);
            Collection<File> jsonToInspect;

            if (interestingBranches != null) {
                jsonToInspect = new ArrayList<>();

                for (File file : allJson) {
                    String branchName = getBranchnameFromFile(file);
                    if (interestingBranches.contains(branchName)) {
                        jsonToInspect.add(file);
                    }
                }
            } else {
                jsonToInspect = allJson;
            }
            Collection<RepairInfo> allRepairInfo = getRepairInfoFromFiles(jsonToInspect);
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

    private int computeScoreForMessage(String message, int gain, int loose) {
        int score = 0;
        String lowerCaseMessage = message.toLowerCase();

        for (String s : potentialBugEvidence) {
            if (lowerCaseMessage.contains(s)) {
                score += gain;
            }
        }

        for (String s : potentialRefactoringEvidence) {
            if (lowerCaseMessage.contains(s)) {
                score -= loose;
            }
        }

        return score;
    }

    private int computeScore(RepairInfo repairInfo) throws IOException {
        int score = 0;
        GitHub gitHub = GitHubBuilder.fromEnvironment().withOAuthToken(this.githubToken, this.githubLogin).build();

        GHRepository ghRepo = gitHub.getRepository(repairInfo.getGithubProject());
        String commitMsg = ghRepo.getCommit(repairInfo.getPatchCommit()).getCommitShortInfo().getMessage();

        score += this.computeScoreForMessage(commitMsg, 10, 20);

        if (repairInfo.getPrId() != null) {
            GHPullRequest pullRequest = ghRepo.getPullRequest(Integer.parseInt(repairInfo.getPrId()));
            score += this.computeScoreForMessage(pullRequest.getTitle(), 100, 120);

            try {
                for (GHLabel label : pullRequest.getLabels()) {
                    score += this.computeScoreForMessage(label.getName(), 100, 120);
                }
            } catch (HttpException e) {
            }


            for (GHIssueComment comment : pullRequest.getComments()) {
                if (comment.getUser().equals(pullRequest.getUser())) {
                    score += this.computeScoreForMessage(commitMsg, 10, 20);
                } else {
                    score += this.computeScoreForMessage(commitMsg, 1, 2);
                }
            }
        }

        Matcher matcher = ISSUE_PATTERN.matcher(commitMsg);
        List<Integer> issuesOrPr = new ArrayList<>();

        while (matcher.find()) {
            int newIssueOrPRId = Integer.parseInt(matcher.group().substring(1));
            issuesOrPr.add(newIssueOrPRId);
        }

        for (int issueOrPRId : issuesOrPr) {
            GHIssue prOrIssue;
            try {
                prOrIssue = ghRepo.getPullRequest(issueOrPRId);
                if (prOrIssue == null) {
                    prOrIssue = ghRepo.getIssue(issueOrPRId);
                }
            } catch (Exception e) {
                prOrIssue = ghRepo.getIssue(issueOrPRId);
            }

            if (prOrIssue != null) {
                score += this.computeScoreForMessage(prOrIssue.getTitle(), 80, 100);

                for (GHIssueComment comment : prOrIssue.getComments()) {
                    if (comment.getUserName().equals(prOrIssue.getUser().getLogin())) {
                        score += this.computeScoreForMessage(commitMsg, 10, 20);
                    } else {
                        score += this.computeScoreForMessage(commitMsg, 1, 2);
                    }
                }
                try {
                    for (GHLabel label : prOrIssue.getLabels()) {
                        score += this.computeScoreForMessage(label.getName(), 100, 120);
                    }
                } catch (HttpException e) {
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
                repairInfo.setPushBranchName(this.getBranchnameFromFile(f));
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

    public static void main(String[] args) throws IOException {
        String jsonLocation = args[0];
        String githubLogin = args[1];
        String githubOauth = args[2];

        List<String> interestingBranches = null;
        if (args.length > 3) {
           interestingBranches = Files.readAllLines(new File(args[3]).toPath());
        }

        EvaluatePotentialBug evaluatePotentialBug = new EvaluatePotentialBug(jsonLocation, githubLogin, githubOauth);
        Map<Integer, Collection<RepairInfo>> results = evaluatePotentialBug.computeAllScores(interestingBranches);
        evaluatePotentialBug.printScoreResults(results);

        if (args.length > 4) {
            File outputFile = new File(args[4]);
            BufferedWriter buffer = new BufferedWriter(new FileWriter(outputFile));

            for (int score : results.keySet()) {
                Collection<RepairInfo> repairInfos = results.get(score);
                for (RepairInfo repairInfo : repairInfos) {
                    buffer.write(repairInfo.getPushBranchName()+","+score+"\n");
                    buffer.flush();
                }
            }
            buffer.close();
        }
    }
}
