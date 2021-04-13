package fr.inria.spirals.repairnator.realtime.githubapi.commits;

import fr.inria.spirals.repairnator.realtime.GithubScanner;
import org.kohsuke.github.*;
import org.kohsuke.github.GHCheckRun.Conclusion;
import fr.inria.spirals.repairnator.realtime.githubapi.GAA;
import fr.inria.spirals.repairnator.realtime.githubapi.commits.models.SelectedCommit;
import fr.inria.spirals.repairnator.realtime.githubapi.repositories.GithubAPIRepoAdapter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GithubAPICommitAdapter {
    private static GithubAPICommitAdapter _instance;

    public static GithubAPICommitAdapter getInstance() {
        if (_instance == null)
            _instance = new GithubAPICommitAdapter();
        return _instance;
    }

    public List<SelectedCommit> getSelectedCommits
            (
                    GHRepository repo,
                    long since,
                    long until,
                    GithubScanner.FetchMode fetchMode
            ) throws IOException {
        List<SelectedCommit> res = new ArrayList<>();

        GHCommitQueryBuilder query = repo.queryCommits().since(since).until(until);
        List<GHCommit> commits = query.list().toList();
        for (GHCommit commit : commits) {
            boolean isGithubActionsFailed = false;
            PagedIterable<GHCheckRun> checkRuns = commit.getCheckRuns();
            for (GHCheckRun check : checkRuns) {
                if (check.getApp().getName().equals("GitHub Actions")) {
                    Conclusion conclusion = check.getConclusion();
                    if (conclusion != null && (conclusion != Conclusion.SUCCESS
                            && conclusion != Conclusion.NEUTRAL && conclusion != Conclusion.SKIPPED)) {
                        isGithubActionsFailed = true;
                        break;
                    }
                }
            }

            switch(fetchMode) {
                case ALL:
                    res.add(new SelectedCommit(isGithubActionsFailed, commit.getSHA1(), repo.getFullName()));
                    break;
                case PASSING:
                    if(!isGithubActionsFailed)
                        res.add(new SelectedCommit(isGithubActionsFailed, commit.getSHA1(), repo.getFullName()));
                    break;
                case FAILED:
                default:
                    if(isGithubActionsFailed)
                        res.add(new SelectedCommit(isGithubActionsFailed, commit.getSHA1(), repo.getFullName()));
                    break;
            }
        }

        return res;
    }

    public List<SelectedCommit> getSelectedCommits
            (
                    long intervalStart,
                    long intervalEnd,
                    GithubScanner.FetchMode fetchMode,
                    Set<String> fixedRepos
            ) throws IOException {
         final Set<String> repositories = fixedRepos == null ? GithubAPIRepoAdapter.getInstance()
                .listJavaRepositories(intervalStart, 0, GithubAPIRepoAdapter.MAX_STARS) : fixedRepos;


        AtomicInteger cnt = new AtomicInteger(0);
        List<SelectedCommit> selectedCommits = Collections.synchronizedList(new ArrayList<>());
        repositories.parallelStream().forEach( repoName -> {
            try {
                GHRepository repo = GAA.g().getRepository(repoName);
                System.out.println("Checking commits for: " + repo.getName() + " " + cnt.incrementAndGet() + " " + repositories.size()
                        + " " + new Date(intervalStart));
                boolean isMaven = false;
                for (GHTreeEntry treeEntry : repo.getTree("HEAD").getTree()) {
                    if (treeEntry.getPath().equals("pom.xml")) {
                        isMaven = true;
                        break;
                    }
                }

                if (!isMaven) {
                    return;
                }

                selectedCommits.addAll(GithubAPICommitAdapter.getInstance()
                        .getSelectedCommits(repo, intervalStart, intervalEnd, fetchMode));

            } catch (Exception e) {
                System.err.println("error occurred for: " + repoName);
                e.printStackTrace();
            }
        });

        return selectedCommits;
    }
}
