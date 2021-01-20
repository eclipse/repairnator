package fr.inria.spirals.repairnator.realtime.githubapi.repositories;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHRepositorySearchBuilder;
import fr.inria.spirals.repairnator.realtime.githubapi.GAA;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class GithubAPIRepoAdapter {
    public static final int MAX_STARS = 200000;
    private static GithubAPIRepoAdapter _instance;
    private static final int MAX_RESULTS = 1000;

    public static GithubAPIRepoAdapter getInstance() {
        if (_instance == null)
            _instance = new GithubAPIRepoAdapter();
        return _instance;
    }

    public Set<String> listJavaRepositories(String pushedAfter) throws IOException { // ex., pushedAfter = "2020-11-20T08:10:00Z"
        return listJavaRepositories(pushedAfter, 0, MAX_STARS);
    }

    public Set<String> listJavaRepositories(String pushedAfter, int min, int max) throws IOException {
        Set<String> res = new HashSet<>();

        GHRepositorySearchBuilder searchQuery = GAA.g().searchRepositories().language("java")
                .pushed(">" + pushedAfter).stars(min + ".." + max);

        int totalCount = searchQuery.list().getTotalCount();

        if (totalCount <= MAX_RESULTS || min == max) {
            System.out.println("Adding results for: " + pushedAfter + " " + min + " " + max);
            for (GHRepository repo : searchQuery.list().withPageSize(MAX_RESULTS)) {
                res.add(repo.getFullName());
            }
            if (totalCount > MAX_RESULTS) {
                System.err.println("too many results. returned since min==max: " + pushedAfter
                        + " " + min + " " + max + " " + System.currentTimeMillis());
            }
        } else {
            int middle = (min + max) / 2;
            res.addAll(listJavaRepositories(pushedAfter, min, middle));
            res.addAll(listJavaRepositories(pushedAfter, middle + 1, max));
        }

        return res;
    }

    public Set<String> listJavaRepositories(long intervalStart, int min, int max) throws IOException {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        String intervalStartStr = df.format(new Date(intervalStart));
        intervalStartStr = intervalStartStr.substring(0, intervalStartStr.length() - 5) + "Z";

        return listJavaRepositories(intervalStartStr, min, max);
    }
}
