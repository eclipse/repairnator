package fr.inria.spirals.jtravis.helpers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.inria.spirals.jtravis.entities.Repository;
import okhttp3.ResponseBody;

import java.io.IOException;

/**
 * Created by urli on 21/12/2016.
 */
public class RepositoryHelper extends AbstractHelper {
    public static final String REPO_ENDPOINT = "repos/";

    private static RepositoryHelper instance;

    private RepositoryHelper() {
        super();
    }

    private static RepositoryHelper getInstance() {
        if (instance == null) {
            instance = new RepositoryHelper();
        }
        return instance;
    }

    public static Repository getRepositoryFromSlug(String slug) {
        String resourceUrl = TRAVIS_API_ENDPOINT+REPO_ENDPOINT+slug;

        try {
            ResponseBody response = getInstance().get(resourceUrl);
            JsonParser parser = new JsonParser();
            JsonObject allAnswer = parser.parse(response.string()).getAsJsonObject();
            JsonObject repoJSON = allAnswer.getAsJsonObject("repo");

            Gson gson = new Gson();
            return gson.fromJson(repoJSON, Repository.class);
        } catch (IOException e) {
            return null;
        }
    }

    public static Repository getRepositoryFromId(int repoId) {
        String resourceUrl = TRAVIS_API_ENDPOINT+REPO_ENDPOINT+repoId;

        try {
            ResponseBody response = getInstance().get(resourceUrl);
            JsonParser parser = new JsonParser();
            JsonObject allAnswer = parser.parse(response.string()).getAsJsonObject();
            JsonObject repoJSON = allAnswer.getAsJsonObject("repo");

            return createGson().fromJson(repoJSON, Repository.class);
        } catch (IOException e) {
            return null;
        }
    }


}
