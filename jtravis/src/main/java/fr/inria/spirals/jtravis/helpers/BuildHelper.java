package fr.inria.spirals.jtravis.helpers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.BuildStatus;
import fr.inria.spirals.jtravis.entities.Config;
import fr.inria.spirals.jtravis.entities.Commit;
import fr.inria.spirals.jtravis.entities.Job;
import fr.inria.spirals.jtravis.entities.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
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

    private static boolean isAcceptedBuild(Build build, int prNumber, BuildStatus status, String previousBranch) {
        if (prNumber != -1 && build.getPullRequestNumber() != prNumber) {
            return false;
        }
        if (previousBranch != null && !previousBranch.equals("") && !previousBranch.equals(build.getCommit().getBranch())) {
            return false;
        }
        if (status != null && build.getBuildStatus() != status) {
            return false;
        }
        return true;
    }

    private static String getResourceUrl(String slug, String eventType, int after_number) {
        String resourceUrl = getInstance().getEndpoint()+RepositoryHelper.REPO_ENDPOINT+slug+"/"+BUILD_NAME;

        if (eventType != null) {
            resourceUrl += "?event_type=" + eventType;
        }
        if (after_number > 0) {
            if (eventType != null) {
                resourceUrl += "&after_number=" + after_number;
            } else {
                resourceUrl += "?after_number="+after_number;
            }
        }

        return resourceUrl;
    }

    private static JsonArray sortBuildJsonArray(JsonArray buidArray) {
        List<JsonObject> jsonValues = new ArrayList<JsonObject>();

        for (JsonElement element : buidArray) {
            jsonValues.add((JsonObject) element);
        }

        Collections.sort( jsonValues, new Comparator<JsonObject>() {
            @Override
            public int compare(JsonObject a, JsonObject b) {
                return (a.get("number").getAsInt()-b.get("number").getAsInt());
            }
        });

        JsonArray result = new JsonArray();

        for (JsonObject values : jsonValues) {
            result.add(values);
        }

        return result;
    }

    private static JsonArray getBuildsAndCommits(String resourceUrl, Map<Integer, Commit> commits, boolean sortBuilds) {
        try {
            String response = getInstance().get(resourceUrl);
            JsonParser parser = new JsonParser();
            JsonObject allAnswer = parser.parse(response).getAsJsonObject();

            JsonArray buildArray = allAnswer.getAsJsonArray("builds");

            if (sortBuilds) {
                buildArray = sortBuildJsonArray(buildArray);
            }

            JsonArray commitsArray = allAnswer.getAsJsonArray("commits");

            for (JsonElement commitJson : commitsArray) {
                Commit commit = createGson().fromJson(commitJson, Commit.class);
                commits.put(commit.getId(), commit);
            }

            return buildArray;
        } catch (IOException e) {
            getInstance().getLogger().warn("Error when trying to get builds and commits from "+resourceUrl+" : "+e.getMessage());
        }
        return new JsonArray();
    }

    /**
     * This is a recursive method allowing to get build from a slug.
     *
     * @param slug The slug where to get the builds
     * @param result The list result to aggregate
     * @param limitDate If given, the date limit to get builds: all builds *before* this date are considered
     * @param after_number Used for pagination: multiple requests may have to be made to reach the final date
     * @param original_after_number is used to set the value of after_number back to the original value before the first request to an event type be made, as after_number changes when more than one request is made for a same event type (pagination)
     * @param eventTypes Travis support multiple event types for builds like Push, PR or CRON. Those are retrieved individually
     * @param limitNumber Allow to finish early based on the number of builds selected. if 0 passed use only the date to stop searching
     * @param status Allow to only select builds of that status, if null it takes all status
     * @param prNumber Allow to only consider builds of that PR, if -1 given it takes all builds
     * @param onlyAfterNumber Consider only builds after the specified after_number: should be use in conjunction with a specified after_number.
     */
    private static void getBuildsFromSlugRecursively(String slug, List<Build> result, Date limitDate, int after_number, int original_after_number, List<String> eventTypes, int limitNumber, BuildStatus status, int prNumber, boolean onlyAfterNumber, String previousBranch) {
        if ((eventTypes.isEmpty()) || (after_number <= 0 && onlyAfterNumber)) {
            return;
        }

        String resourceUrl = getResourceUrl(slug, eventTypes.get(0), after_number);

        boolean dateReached = false;

        Map<Integer, Commit> commits = new HashMap<Integer, Commit>();

        JsonArray buildArray = getBuildsAndCommits(resourceUrl, commits, false);

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

                    // we only accept build with a build number
                    if (isAcceptedBuild(build, prNumber, status, previousBranch)) {
                        result.add(build);
                    }
                }

                // if we reach the limitNumber we can break the loop, and consider the date is reached
                // we don't return now because we may get a more interesting build (closest to the original one) with another event type
                if (limitNumber != 0 && result.size() >= limitNumber) {
                    dateReached = true;
                    break;
                }
            } else {
                dateReached = true;
                break;
            }
        }

        if (buildArray.size() == 0) {
            dateReached = true;
        }

        if (!dateReached) {
            getBuildsFromSlugRecursively(slug, result, limitDate, lastBuildNumber, original_after_number, eventTypes, limitNumber, status, prNumber, onlyAfterNumber, previousBranch);
        } else {
            eventTypes.remove(0);
            if (!onlyAfterNumber) {
                after_number = 0;
            } else {
                after_number = original_after_number;
            }
            getBuildsFromSlugRecursively(slug, result, limitDate, after_number, original_after_number, eventTypes, limitNumber, status, prNumber, onlyAfterNumber, previousBranch);
        }
    }

    /**
     * Compute the last build number for a given slug and event type. Allow to determine a stop condition when iterating recursively towards the present (in future of a build)
     * @param slug
     * @param eventType
     * @return
     */
    private static int computeStopCondition(String slug, String eventType) {
        String resourceUrl = getResourceUrl(slug, eventType, -1);
        Map<Integer, Commit> commits = new HashMap<Integer,Commit>();
        JsonArray buildArray = getBuildsAndCommits(resourceUrl, commits, true);

        if (buildArray.size() == 0) {
            return -1;
        }

        int counter = 1;
        String buildNumber = null;

        while (buildNumber == null && counter <= buildArray.size()) {
            JsonElement lastBuild = buildArray.get(buildArray.size()-counter);
            Build build = createGson().fromJson(lastBuild, Build.class);
            buildNumber = build.getNumber();
            counter++;
        }

        if (buildNumber != null) {
            return Integer.parseInt(buildNumber);
        } else {
            return 0;
        }
    }

    /**
     * Search recursively for a build AFTER a given build, so in its future. Take the same parameters as #getBuildsFromSlugRecursively
     * @param slug
     * @param result
     * @param after_number
     * @param originalAfterNumber
     * @param eventTypes
     * @param status
     * @param prNumber
     * @param previousBranch
     * @param limitNumber
     * @param stop_condition_in_future
     */
    private static void getBuildsFromSlugRecursivelyInFuture(String slug, List<Build> result, int after_number, int originalAfterNumber, List<String> eventTypes, BuildStatus status, int prNumber, String previousBranch, int limitNumber, int stop_condition_in_future) {
        if (eventTypes.isEmpty()) {
            return;
        }

        if (stop_condition_in_future == -1) {
            stop_condition_in_future = computeStopCondition(slug, eventTypes.get(0));
        }

        boolean dateReached;
        int lastBuildNumber = after_number;

        if (stop_condition_in_future >= after_number || stop_condition_in_future == 0) {
            String resourceUrl = getResourceUrl(slug, eventTypes.get(0), after_number+20);
            dateReached = false;

            Map<Integer, Commit> commits = new HashMap<Integer,Commit>();

            JsonArray buildArray = getBuildsAndCommits(resourceUrl, commits, true);

            for (JsonElement buildJson : buildArray) {
                Build build = createGson().fromJson(buildJson, Build.class);
                int commitId = build.getCommitId();

                if (commits.containsKey(commitId)) {
                    build.setCommit(commits.get(commitId));
                }

                if (build.getNumber() != null) {
                    int buildNumber = Integer.parseInt(build.getNumber());
                    if (buildNumber > after_number) {
                        if (isAcceptedBuild(build, prNumber, status, previousBranch)) {
                            result.add(build);
                        }
                        if (buildNumber > lastBuildNumber) {
                            lastBuildNumber = buildNumber;
                        }
                    }


                }

                if (limitNumber != 0 && result.size() >= limitNumber) {
                    dateReached = true;
                    break;
                }
            }

            if (lastBuildNumber == after_number) {
                lastBuildNumber += 20;
            }

            if (stop_condition_in_future == 0) {
                if (lastBuildNumber == after_number) {
                    dateReached = true;
                }
            } else {
                if (lastBuildNumber >= stop_condition_in_future) {
                    dateReached = true;
                }
            }


            if (buildArray.size() == 0) {
                dateReached = true;
            }
        } else {
            dateReached = true;
        }

        if (!dateReached) {
            getBuildsFromSlugRecursivelyInFuture(slug, result, lastBuildNumber, originalAfterNumber, eventTypes, status, prNumber, previousBranch, limitNumber, stop_condition_in_future);
        } else {
            eventTypes.remove(0);
            getBuildsFromSlugRecursivelyInFuture(slug, result, originalAfterNumber, originalAfterNumber, eventTypes, status, prNumber, previousBranch, limitNumber, -1);
        }
    }

    /**
     * This is a recursive method allowing to get the number of the last build before a given date from a slug.
     *
     * @param slug The slug where to get the build
     * @param date The date to get the last build
     * @param after_number Used for pagination: multiple requests may have to be made to reach the interesting build
     * @param onlyAfterNumber Consider only builds after the specified after_number: should be use in conjunction with a specified after_number
     */
    public static int getTheLastBuildNumberOfADate(String slug, Date date, int after_number, boolean onlyAfterNumber) {
        if ((date == null) || (after_number <= 0 && onlyAfterNumber)) {
            return -1;
        }

        String resourceUrl = getResourceUrl(slug, null, after_number);

        boolean dateReached = false;

        Map<Integer, Commit> commits = new HashMap<Integer,Commit>();

        JsonArray buildArray = getBuildsAndCommits(resourceUrl, commits, false);

        int lastBuildNumber = Integer.MAX_VALUE;

        for (JsonElement buildJson : buildArray) {
            Build build = createGson().fromJson(buildJson, Build.class);

            if (build.getNumber() != null) {
                int buildNumber = Integer.parseInt(build.getNumber());
                if (lastBuildNumber > buildNumber) {
                    lastBuildNumber = buildNumber;
                }
            }

            if (build.getFinishedAt() != null && !build.getFinishedAt().after(date)) {
                dateReached = true;
                break;
            }
        }

        if (!dateReached && !(buildArray.size() == 0)) {
            return getTheLastBuildNumberOfADate(slug, date, lastBuildNumber, onlyAfterNumber);
        } else {
            return lastBuildNumber;
        }
    }

    public static List<Build> getBuildsFromSlugWithLimitDate(String slug, Date limitDate) {
        List<Build> result = new ArrayList<Build>();
        getBuildsFromSlugRecursively(slug, result, limitDate, 0, 0, getEventTypes(), 0, null, -1, false, null);
        return result;
    }

    public static List<Build> getBuildsFromSlug(String slug) {
        return getBuildsFromSlugWithLimitDate(slug, null);
    }

    public static List<Build> getBuildsFromRepositoryWithLimitDate(Repository repository, Date limitDate) {
        List<Build> result = new ArrayList<Build>();
        getBuildsFromSlugRecursively(repository.getSlug(), result, limitDate, 0, 0, getEventTypes(), 0, null, -1, false, null);

        for (Build b : result) {
            b.setRepository(repository);
        }
        return result;
    }

    public static List<Build> getBuildsFromRepository(Repository repository) {
        return getBuildsFromRepositoryWithLimitDate(repository, null);
    }

    /**
     * Return the last build before the given build which respect the given status and which is from the same PR if it's a PR build. If given status is null, it will return the last build before.
     * If no build is found, it returns null.
     * @param build
     * @param status
     * @return
     */
    public static Build getLastBuildOfSameBranchOfStatusBeforeBuild(Build build, BuildStatus status) {
        String slug = build.getRepository().getSlug();
        List<Build> results = new ArrayList<Build>();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -1);
        Date limitDate = calendar.getTime();

        int after_number = 0;
        try {
            after_number = Integer.parseInt(build.getNumber());
        } catch (NumberFormatException e) {
            getInstance().getLogger().error("Error while parsing build number for build id: "+build.getId(),e);
            return null;
        }

        int limitNumber = 1;
        List<String> eventTypes = new ArrayList<String>();
        int prNumber;

        if (build.isPullRequest()) {
            eventTypes.add("pull_request");
            prNumber = build.getPullRequestNumber();
        } else {
            eventTypes.add("cron");
            eventTypes.add("push");
            prNumber = -1;
        }

        getBuildsFromSlugRecursively(slug, results, limitDate, after_number, after_number, eventTypes, limitNumber, status, prNumber, true, build.getCommit().getBranch());

        if (results.size() > 0) {
            if (results.size() > 1) {
                Collections.sort(results);
            }
            return results.get(results.size()-1);
        } else {
            return null;
        }
    }

    public static Build getNextBuildOfSameBranchOfStatusAfterBuild(Build build, BuildStatus status) {
        String slug = build.getRepository().getSlug();
        List<Build> results = new ArrayList<Build>();

        int after_number = 0;
        try {
            after_number = Integer.parseInt(build.getNumber());
        } catch (NumberFormatException e) {
            getInstance().getLogger().error("Error while parsing build number for build id: "+build.getId(),e);
            return null;
        }

        List<String> eventTypes = new ArrayList<String>();
        int prNumber;

        if (build.isPullRequest()) {
            eventTypes.add("pull_request");
            prNumber = build.getPullRequestNumber();
        } else {
            eventTypes.add("cron");
            eventTypes.add("push");
            prNumber = -1;
        }

        int limitNumber = 1;

        getBuildsFromSlugRecursivelyInFuture(slug, results, after_number, after_number, eventTypes, status, prNumber, build.getCommit().getBranch(), limitNumber, -1);
        if (results.size() > 0) {
            if (results.size() > 1) {
                Collections.sort(results);
            }
            return results.get(0);
        } else {
            return null;
        }
    }



    public static Build getLastBuildFromMaster(Repository repository) {
        String slug = repository.getSlug();
        List<Build> results = new ArrayList<Build>();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -10);
        Date limitDate = calendar.getTime();

        int limitNumber = 1;
        List<String> eventTypes = new ArrayList<String>();
        eventTypes.add("cron");
        eventTypes.add("push");
        int prNumber = -1;

        getBuildsFromSlugRecursively(slug, results, limitDate, 0, 0, eventTypes, limitNumber, null, prNumber, false, null);


        if (results.size() > 0) {
            return results.get(0);
        } else {
            return null;
        }
    }

    public static List<Build> getBuildsFromRepositoryInTimeInterval(Repository repository, Date initialDate, Date finalDate) {
        int lastBuildNumber = getTheLastBuildNumberOfADate(repository.getSlug(), finalDate, 0, false);
        if (lastBuildNumber != -1) {
            List<Build> results = new ArrayList<Build>();

            int after_number = lastBuildNumber + 1;

            getBuildsFromSlugRecursively(repository.getSlug(), results, initialDate, after_number, after_number, getEventTypes(), 0, null, -1, true, null);

            for (Build b : results) {
                b.setRepository(repository);
            }
            if (results.size() > 1) {
                Collections.sort(results);
            }
            return results;
        }
        return null;
    }
}
