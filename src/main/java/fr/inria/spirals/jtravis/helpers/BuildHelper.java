package fr.inria.spirals.jtravis.helpers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.BuildStatus;
import fr.inria.spirals.jtravis.entities.Commit;
import fr.inria.spirals.jtravis.entities.Repository;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by urli on 21/12/2016.
 */
public class BuildHelper extends AbstractHelper {

    public static final String BUILD_ENDPOINT = "builds/";

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

    private static Build createBuildFromJSON(JsonObject buildJSON, Repository parentRepo) {
       /* Build build = new Build();
        build.setId(buildJSON.get("id").getAsInt());
        build.setDuration(buildJSON.get("duration").getAsInt());
        build.setFromPullRequest(buildJSON.get("pull_request").getAsBoolean());
        build.setPullRequestNumber(buildJSON.get("pull_request_number").getAsInt());
        build.setPullRequestTitle(buildJSON.get("pull_request_title").getAsString());
        build.setStatus(BuildStatus.valueOf(buildJSON.get("state").getAsString().toUpperCase()));
        build.setStartedAt(DateHelper.getDateFromJSONStringElement(buildJSON.get("started_at")));
        build.setFinishedAt(DateHelper.getDateFromJSONStringElement(buildJSON.get("finished_at")));


        if (parentRepo != null) {
            build.setRepository(parentRepo);
        } else {
            Repository repo = RepositoryHelper.getRepositoryFromId(buildJSON.get("repository_id").getAsInt());
            build.setRepository(repo);
        }

        Commit commit = new Commit();
        commit.setHash(buildJSON.get("commit").getAsString());
        commit.setAuthorEmail(buildJSON.get("author_email").getAsString());
        commit.setAuthorName(buildJSON.get("author_name").getAsString());
        commit.setBranch(buildJSON.get("branch").getAsString());
        commit.setCommitterEmail(buildJSON.get("committer_email").getAsString());
        commit.setCommitterName(buildJSON.get("committer_name").getAsString());
        commit.setCompareUrl(buildJSON.get("compare_url").getAsString());
        commit.setMessage(buildJSON.get("message").getAsString());
        commit.setCommittedAt(DateHelper.getDateFromJSONStringElement(buildJSON.get("committed_at")));


        build.setCommit(commit);*/
        Gson gson = new Gson();
        return gson.fromJson(buildJSON, Build.class);
    }

    public static Build getBuildFromId(int id, Repository parentRepo) {
        String resourceUrl = TRAVIS_API_ENDPOINT+BUILD_ENDPOINT+id;

        try {
            ResponseBody response = getInstance().get(resourceUrl);
            JsonParser parser = new JsonParser();
            JsonObject allAnswer = parser.parse(response.string()).getAsJsonObject();

            JsonObject buildJSON = allAnswer.getAsJsonObject("build");
            return createBuildFromJSON(buildJSON, parentRepo);
        } catch (IOException e) {
            return null;
        }
    }
}
