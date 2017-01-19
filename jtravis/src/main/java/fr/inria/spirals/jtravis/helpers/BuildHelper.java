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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

    private static List<String> getEventTypes() {
        List<String> result = new ArrayList<String>();
        result.addAll(Arrays.asList(new String[]{ "cron", "push", "pull_request"}));
        return result;
    }

    protected static BuildHelper getInstance() {
        if (instance == null) {
            instance = new BuildHelper();
        }
        return instance;
    }

    public static Build getBuildFromId(int id, Repository parentRepo) {
        String resourceUrl = getInstance().getEndpoint()+BUILD_ENDPOINT+id;

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
            getInstance().getLogger().warn("Error when getting build id "+id+" : "+e.getMessage());
            return null;
        }
    }

    /**
     * This is a recursive method allowing to get build from a slug.
     *
     * @param slug The slug where to get the builds
     * @param result The list result to aggregate
     * @param limitDate If given, the date limit to get builds: all builds *before* this date are considered
     * @param after_number Used for pagination: multiple requests may have to be made to reach the final date
     * @param eventTypes Travis support multiple event types for builds like Push, PR or CRON. Those are retrieved individually
     */
    private static void getBuildsFromSlugRecursively(String slug, List<Build> result, Date limitDate, int after_number, List<String> eventTypes) {
        Map<Integer, Commit> commits = new HashMap<Integer,Commit>();

        String resourceUrl = getInstance().getEndpoint()+RepositoryHelper.REPO_ENDPOINT+slug+"/"+BUILD_NAME;

        if (eventTypes.isEmpty()) {
            return;
        }
        String evenType = eventTypes.get(0);
        resourceUrl += "?event_type="+evenType;

        if (after_number > 0) {
            resourceUrl += "&after_number="+after_number;
        }

        boolean dateReached = false;

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

            int lastBuildNumber = Integer.MAX_VALUE;

            for (JsonElement buildJson : buildArray) {
                Build build = createGson().fromJson(buildJson, Build.class);

                if ((limitDate == null) || (build.getFinishedAt() == null) || (build.getFinishedAt().after(limitDate))) {
                    int commitId = build.getCommitId();

                    if (commits.containsKey(commitId)) {
                        build.setCommit(commits.get(commitId));
                    }


                    if (build.getNumber() != null) {
                        int buildNumber = Integer.parseInt(build.getNumber());
                        if (lastBuildNumber > buildNumber) {
                            lastBuildNumber = buildNumber;
                        }
                    }


                    result.add(build);
                } else {
                    dateReached = true;
                    break;
                }
            }

            if (buildArray.size() == 0) {
                dateReached = true;
            }

            if (limitDate != null && !dateReached) {
                getBuildsFromSlugRecursively(slug, result, limitDate, lastBuildNumber, eventTypes);
            } else {
                eventTypes.remove(0);
                getBuildsFromSlugRecursively(slug, result, limitDate, 0, eventTypes);
            }
        } catch (IOException e) {
            getInstance().getLogger().warn("Error when getting list of builds from slug "+slug+" : "+e.getMessage());
        }
    }

    public static List<Build> getBuildsFromSlugWithLimitDate(String slug, Date limitDate) {
        List<Build> result = new ArrayList<Build>();
        getBuildsFromSlugRecursively(slug, result, limitDate, 0, getEventTypes());
        return result;
    }

    public static List<Build> getBuildsFromSlug(String slug) {
        return getBuildsFromSlugWithLimitDate(slug, null);
    }

    public static List<Build> getBuildsFromRepositoryWithLimitDate(Repository repository, Date limitDate) {
        List<Build> result = new ArrayList<Build>();
        getBuildsFromSlugRecursively(repository.getSlug(), result, limitDate, 0, getEventTypes());

        for (Build b : result) {
            b.setRepository(repository);
        }
        return result;
    }

    public static List<Build> getBuildsFromRepositoryWithLimitDate(Repository repository) {
        return getBuildsFromRepositoryWithLimitDate(repository, null);
    }
}
