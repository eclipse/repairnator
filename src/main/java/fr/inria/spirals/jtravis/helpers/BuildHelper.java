package fr.inria.spirals.jtravis.helpers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.BuildStatus;
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

    public static Build getBuildFromId(int id, Repository parentRepo) {
        String resourceUrl = TRAVIS_API_ENDPOINT+BUILD_ENDPOINT+id;

        try {
            ResponseBody response = getInstance().get(resourceUrl);
            JsonParser parser = new JsonParser();
            JsonObject allAnswer = parser.parse(response.string()).getAsJsonObject();

            JsonObject buildObject = allAnswer.getAsJsonObject("build");
            Build build = new Build();
            build.setId(buildObject.get("id").getAsInt());
            build.setDuration(buildObject.get("duration").getAsInt());
            build.setFromPullRequest(buildObject.get("pull_request").getAsBoolean());
            build.setPullRequestNumber(buildObject.get("pull_request_number").getAsInt());
            build.setPullRequestTitle(buildObject.get("pull_request_title").getAsString());
            build.setStatus(BuildStatus.valueOf(buildObject.get("state").getAsString().toUpperCase()));
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
            try {
                build.setStartedAt(format.parse(buildObject.get("started_at").getAsString()));
                build.setFinishedAt(format.parse(buildObject.get("finished_at").getAsString()));
            } catch (ParseException e) {
            }

            if (parentRepo != null) {
                build.setRepository(parentRepo);
            } else {
                Repository repo = RepositoryHelper.getRepositoryFromId(buildObject.get("repository_id").getAsInt());
                build.setRepository(repo);
            }

            return build;
        } catch (IOException e) {
            return null;
        }
    }
}
