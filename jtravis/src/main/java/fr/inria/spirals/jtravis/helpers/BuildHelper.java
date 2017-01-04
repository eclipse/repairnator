package fr.inria.spirals.jtravis.helpers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.Config;
import fr.inria.spirals.jtravis.entities.Commit;
import fr.inria.spirals.jtravis.entities.Job;
import fr.inria.spirals.jtravis.entities.Repository;
import okhttp3.ResponseBody;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The helper to deal with Build objects
 *
 * @author Simon Urli
 */
public class BuildHelper extends AbstractHelper {

    public static final String BUILD_NAME = "builds";
    public static final String BUILD_ENDPOINT = BUILD_NAME+"/";

    private static BuildHelper instance;

    private BuildHelper() {
        super();
    }

    private static BuildHelper getInstance() {
        if (instance == null) {
            instance = new BuildHelper();
        }
        return instance;
    }

    public static Build getBuildFromId(int id, Repository parentRepo) {
        String resourceUrl = TRAVIS_API_ENDPOINT+BUILD_ENDPOINT+id;

        try {
            String response = getInstance().get(resourceUrl);
            JsonParser parser = new JsonParser();
            JsonObject allAnswer = parser.parse(response).getAsJsonObject();

            JsonObject buildJSON = allAnswer.getAsJsonObject("build");
            Build build = createGson().fromJson(buildJSON, Build.class);

            JsonObject commitJSON = allAnswer.getAsJsonObject("commit");
            Commit commit = CommitHelper.getCommitFromJsonElement(commitJSON);
            build.setCommit(commit);

            if (parentRepo != null) {
                build.setRepository(parentRepo);
            }

            JsonObject configJSON = buildJSON.getAsJsonObject("config");
            Config config = ConfigHelper.getConfigFromJsonElement(configJSON);
            build.setConfig(config);

            JsonArray arrayJobs = allAnswer.getAsJsonArray("jobs");

            for (JsonElement jobJSONElement : arrayJobs) {
                Job job = JobHelper.createJobFromJsonElement((JsonObject)jobJSONElement);
                build.addJob(job);
            }



            return build;
        } catch (IOException e) {
            AbstractHelper.LOGGER.warn("Error when getting build id "+id+" : "+e.getMessage());
            return null;
        }
    }

    private static void getBuildsFromSlugRecursively(String slug, List<Build> result, Date limitDate, int after_number) {
        Map<Integer, Commit> commits = new HashMap<Integer,Commit>();

        String resourceUrl = TRAVIS_API_ENDPOINT+RepositoryHelper.REPO_ENDPOINT+slug+"/"+BUILD_NAME;

        if (after_number > 0) {
            resourceUrl += "?after_number="+after_number;
        }

        try {
            String response = getInstance().get(resourceUrl);
            JsonParser parser = new JsonParser();
            JsonObject allAnswer = parser.parse(response).getAsJsonObject();

            JsonArray commitsArray = allAnswer.getAsJsonArray("commits");

            for (JsonElement commitJson : commitsArray) {
                Commit commit = createGson().fromJson(commitJson, Commit.class);
                commits.put(commit.getId(), commit);
            }

            JsonArray buildArray = allAnswer.getAsJsonArray("builds");
            int lastBuildId = 0;

            for (JsonElement buildJson : buildArray) {
                Build build = createGson().fromJson(buildJson, Build.class);

                if ((limitDate == null) || (build.getFinishedAt().compareTo(limitDate) < 0)) {
                    int commitId = build.getCommitId();

                    if (commits.containsKey(commitId)) {
                        build.setCommit(commits.get(commitId));
                    }

                    if (lastBuildId < build.getId()) {
                        lastBuildId = build.getId();
                    }

                    result.add(build);
                } else {
                    return;
                }
            }

            if (limitDate != null) {
                getBuildsFromSlugRecursively(slug, result, limitDate, lastBuildId);
            }
        } catch (IOException e) {
            AbstractHelper.LOGGER.warn("Error when getting list of builds from slug "+slug+" : "+e.getMessage());
        }
    }

    public static List<Build> getBuildsFromSlugWithLimitDate(String slug, Date limitDate) {
        List<Build> result = new ArrayList<Build>();
        getBuildsFromSlugRecursively(slug, result, limitDate, 0);
        return result;
    }

    public static List<Build> getBuildsFromSlug(String slug) {
        return getBuildsFromSlugWithLimitDate(slug, null);
    }

    public static List<Build> getBuildsFromRepositoryWithLimitDate(Repository repository, Date limitDate) {
        List<Build> result = new ArrayList<Build>();
        getBuildsFromSlugRecursively(repository.getSlug(), result, limitDate, 0);

        for (Build b : result) {
            b.setRepository(repository);
        }
        return result;
    }

    public static List<Build> getBuildsFromRepositoryWithLimitDate(Repository repository) {
        return getBuildsFromRepositoryWithLimitDate(repository, null);
    }
}
