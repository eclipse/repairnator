package fr.inria.spirals.jtravis.helpers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.inria.spirals.jtravis.entities.Repository;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.net.URLEncoder;

/**
 * The helper to deal with repository objects.
 *
 * @author Simon Urli
 */
public class RepositoryHelper extends AbstractHelper {
    public static final String REPO_ENDPOINT = "repos/";
    public static final String REPO_ENDPOINTV3 = "repo/";

    private static RepositoryHelper instance;

    private RepositoryHelper() {
        super();
    }

    protected static RepositoryHelper getInstance() {
        if (instance == null) {
            instance = new RepositoryHelper();
        }
        return instance;
    }

    public static Repository getRepositoryFromSlug(String slug) {
        String resourceUrl ="";
        if(Version.getVersionV3()) {
            String urlSlug = "";
            try {
                urlSlug = URLEncoder.encode(slug, "UTF-8");
                resourceUrl = getInstance().getEndpoint() + REPO_ENDPOINTV3 + urlSlug;
            } catch (IOException e) {
                getInstance().getLogger().warn("Error when getting build from slug " + urlSlug + " : " + e.getMessage());
            }
        }
        else {
            resourceUrl = getInstance().getEndpoint() + REPO_ENDPOINT + slug;
        }
        try {
            String response = getInstance().get(resourceUrl);
            JsonParser parser = new JsonParser();
            JsonObject allAnswer = parser.parse(response).getAsJsonObject();

            if(Version.getVersionV3())
                return createGson().fromJson(allAnswer, Repository.class);
            else {
                JsonObject repoJSON = allAnswer.getAsJsonObject("repo");
                return createGson().fromJson(repoJSON, Repository.class);
            }
        } catch (IOException e) {
            getInstance().getLogger().warn("Error when getting repo from slug "+slug+" : "+e.getMessage());
            return null;
        }
    }

    public static Repository getRepositoryFromId(int repoId) {
        String resourceUrl = getInstance().getEndpoint()+REPO_ENDPOINTV3+repoId;

        try {
            String response = getInstance().get(resourceUrl);
            JsonParser parser = new JsonParser();
            JsonObject allAnswer = parser.parse(response).getAsJsonObject();
            if(Version.getVersionV3())
                return createGson().fromJson(allAnswer, Repository.class);
            else {
                JsonObject repoJSON = allAnswer.getAsJsonObject("repo");
                return createGson().fromJson(repoJSON, Repository.class);
            }
        } catch (IOException e) {
            getInstance().getLogger().warn("Error when getting repo id "+repoId+" : "+e.getMessage());
            return null;
        }
    }


}
