package fr.inria.spirals.repairnator.process.step.push;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.inspectors.GitRepositoryProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.states.PushState;
import fr.spoonlabs.flacoco.api.result.FlacocoResult;
import fr.spoonlabs.flacoco.api.result.Location;
import fr.spoonlabs.flacoco.api.result.Suspiciousness;
import org.kohsuke.github.*;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PushFaultLocalizationSuggestions extends AbstractStep {

    private PushState pushState = null;
    private String pushSkippedReason = null;

    public PushFaultLocalizationSuggestions(ProjectInspector inspector, boolean blockingStep) {
        super(inspector, blockingStep);
    }

    public PushFaultLocalizationSuggestions(ProjectInspector inspector, boolean blockingStep, String name) {
        super(inspector, blockingStep, name);
    }

    @Override
    protected StepStatus businessExecute() {
        try {
            pushReviewComments(this.getInspector().getJobStatus().getFlacocoResult());
        } catch (IOException e) {
            this.getLogger().error(e.getLocalizedMessage());
            return StepStatus.buildSkipped(this, "There was an error while publishing fault localization results: " + e);
        }

        if (pushState == PushState.REPO_PUSHED) {
            return StepStatus.buildSuccess(this);
        } else {
            return StepStatus.buildSkipped(this, pushSkippedReason);
        }
    }

    private void pushReviewComments(FlacocoResult result) throws IOException {
        GitRepositoryProjectInspector githubInspector = (GitRepositoryProjectInspector) getInspector();
        GitHub gitHub = RepairnatorConfig.getInstance().getGithub();
        GHRepository originalRepository = gitHub.getRepository(githubInspector.getRepoSlug());
        GHPullRequest pullRequest = originalRepository.getPullRequest(githubInspector.getGitRepositoryPullRequest());

        Map<String, Map<Integer, Integer>> diffMapping = computeDiffMapping(pullRequest.getDiffUrl());
        GHPullRequestReviewBuilder reviewBuilder = pullRequest.createReview();

        int lines = 0;
        for (Map.Entry<Location, Suspiciousness> entry : result.getDefaultSuspiciousnessMap().entrySet()) {
            String partialFileName = entry.getKey().getClassName().replace(".", "/");
            Integer line = entry.getKey().getLineNumber();

            for (String fileName : diffMapping.keySet()) {

                // Since we don't have an exact mapping, we need to partially match them
                if (fileName.contains(partialFileName)) {

                    // We only consider the lines that are in the diff (i.e. that are mapped to a position in the diffMapping)
                    if (diffMapping.get(fileName).containsKey(line)) {
                        lines++;
                        reviewBuilder.comment(
                                String.format(
                                        "This line (%d) has been identified with a suspiciousness value of %,.2f%%.\n\n" +
                                                "<details>\n" +
                                                "     <summary>Failing tests that cover this line</summary>\n\n" +
                                                entry.getValue().getFailingTestCases().stream()
                                                        .map(x -> "- `" + x.getFullyQualifiedMethodName() + "`\n")
                                                        .reduce((x, y) -> x + y).orElse("{}") +
                                                "</details>"
                                        ,
                                        line,
                                        entry.getValue().getScore() * 100
                                ),
                                fileName,
                                diffMapping.get(fileName).get(line)
                        );
                    }

                    break;
                }
            }

            // Break if we have reached the number of requested lines
            if (lines >= RepairnatorConfig.getInstance().getFlacocoTopK()) {
                break;
            }
        }

        if (lines > 0) {
            reviewBuilder.body("[flacoco](https://github.com/SpoonLabs/flacoco) flags " + lines + " suspicious lines as the potential root cause of the test failure.");
            reviewBuilder.event(GHPullRequestReviewEvent.COMMENT);

            if (pullRequest.getState().equals(GHIssueState.OPEN)) {
                reviewBuilder.create();
                pushState = PushState.REPO_PUSHED;
                this.setPushState(pushState);
            } else {
                // Check again to avoid replying to pull requests which have been closed during the fault localization process.
                this.getLogger().warn("The Pull Request #" + githubInspector.getGitRepositoryPullRequest() + " is not open anymore.");
                pushSkippedReason = "The Pull Request #" + githubInspector.getGitRepositoryPullRequest() + " is not open anymore.";
                pushState = PushState.REPO_NOT_PUSHED;
                this.setPushState(pushState);
            }
        } else {
            this.getLogger().warn("Flacoco has found " + result.getDefaultSuspiciousnessMap().size() + " suspicious lines, but none were matched to the diff");
            pushSkippedReason = "Flacoco has found " + result.getDefaultSuspiciousnessMap().size() + " suspicious lines, but none were matched to the diff";
            pushState = PushState.REPO_NOT_PUSHED;
            this.setPushState(pushState);
        }
    }

    /**
     * Computes the mapping between source code lines and diff positions
     *
     * @param diffUrl URL to the diff
     * @return A mapping from file path to a mapping from source code lines to diff positions
     * @throws IOException
     */
    private Map<String, Map<Integer, Integer>> computeDiffMapping(URL diffUrl) throws IOException {

        Scanner scanner = new Scanner(diffUrl.openStream(), "UTF-8");
        scanner.useDelimiter("\\n");

        Map<String, Map<Integer, Integer>> diffMapping = new HashMap<>();
        String currentFile = null;
        Integer currentPosition = null;
        Integer currentLine = null;
        while (scanner.hasNext()) {
            String line = scanner.next();

            // We don't need the information from these lines
            if (line.startsWith("diff --git") || line.startsWith("index") || line.startsWith("---"))
                continue;

            // We get the file name, so we reset the position and null the line
            if (line.startsWith("+++")) {
                currentFile = line.replace("+++ b/", "");
                currentPosition = 0;
                currentLine = null;
            }

            // Marks the beggining of a new hunk
            // e.g.: @@ -29,4 +30,4
            // in this case, the line where the hunk starts is the 29th (we consider the new file version)
            else if (line.startsWith("@@")) {
                Pattern p = Pattern.compile("@@ .*?\\-(\\d+),?(\\d+)?.*?\\+(\\d+),?(\\d+)? @@.*$?");
                Matcher matcher = p.matcher(line);
                currentPosition = currentPosition != null && currentPosition != 0 ? currentPosition + 1 : currentPosition;
                if (matcher.matches())
                    currentLine = Integer.parseInt(matcher.group(3)) - 1;
            }

            // In this case we increment the position but don't store any mapping as we only care about the new file
            else if (line.startsWith("-") || line.startsWith("\\")) {
                currentPosition = currentPosition != null ? currentPosition + 1 : 1;
                continue;
            }

            // In this case we increment both the position and line, since we have advanced one line in the hunk
            else {
                currentPosition = currentPosition != null ? currentPosition + 1 : 1;
                currentLine = currentLine != null ? currentLine + 1 : 1;
            }

            // We record the mapping for the current file, between the line and position
            if (currentFile != null && currentPosition != null && currentLine != null) {
                diffMapping.putIfAbsent(currentFile, new HashMap<>());
                diffMapping.get(currentFile).put(currentLine, currentPosition);
            }
        }

        scanner.close();

        return diffMapping;
    }

}
