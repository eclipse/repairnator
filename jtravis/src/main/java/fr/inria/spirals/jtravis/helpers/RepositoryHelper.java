package fr.inria.spirals.jtravis.helpers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.inria.spirals.jtravis.entities.Repository;
import okhttp3.ResponseBody;

import java.io.IOException;

/**
 * The helper to deal with repository objects.
 *
 * @author Simon Urli
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
            String response = getInstance().get(resourceUrl);
            JsonParser parser = new JsonParser();
            JsonObject allAnswer = parser.parse(response).getAsJsonObject();
            JsonObject repoJSON = allAnswer.getAsJsonObject("repo");

            return createGson().fromJson(repoJSON, Repository.class);
        } catch (IOException e) {
            getInstance().getLogger().warn("Error when getting repo from slug "+slug+" : "+e.getMessage());
            return null;
        }
    }

    public static Repository getRepositoryFromId(int repoId) {
        String resourceUrl = TRAVIS_API_ENDPOINT+REPO_ENDPOINT+repoId;

        try {
            String response = getInstance().get(resourceUrl);
            JsonParser parser = new JsonParser();
            JsonObject allAnswer = parser.parse(response).getAsJsonObject();
            JsonObject repoJSON = allAnswer.getAsJsonObject("repo");

            return createGson().fromJson(repoJSON, Repository.class);
        } catch (IOException e) {
            getInstance().getLogger().warn("Error when getting repo id "+repoId+" : "+e.getMessage());
            return null;
        }
    }


}
