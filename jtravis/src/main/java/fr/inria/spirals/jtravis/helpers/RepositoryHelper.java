package fr.inria.spirals.jtravis.helpers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.inria.spirals.jtravis.entities.Repository;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The helper to deal with repository objects.
 *
 * @author Simon Urli
 */
public class RepositoryHelper extends AbstractHelper {
    public static final String REPO_ENDPOINT = "repos/";

    private static final int NB_REPO_BEFORE_CLEANING_CACHE = 10000;
    private static final int TIME_BEFORE_DELETION_FROM_CACHE_IN_SECONDS = 2*60*60; // 2 hours
    private static final Map<Integer, Repository> CACHE = new HashMap<>();
    private static RepositoryHelper instance;

    private RepositoryHelper() {
        super();
    }

    private static void cleanCache() {

        getInstance().getLogger().info("Limit of cache size reached ("+CACHE.size()+ "entries). Start the cleaning...");
        Date timeLimit = new Date((new Date().getTime() - (TIME_BEFORE_DELETION_FROM_CACHE_IN_SECONDS*1000)));
        List<Integer> deleteFromCache = new ArrayList<>();
        for (Repository repository : CACHE.values()) {
            if (repository.getLastAccess().before(timeLimit)) {
                deleteFromCache.add(repository.getId());
            }
        }
        for (int id : deleteFromCache) {
            CACHE.remove(id);
        }
        getInstance().getLogger().info("Cache cleaned. New size: "+CACHE.size()+" entries.");
    }

    protected static RepositoryHelper getInstance() {
        if (instance == null) {
            instance = new RepositoryHelper();
        }
        return instance;
    }

    public static Repository getRepositoryFromSlug(String slug) {
        String resourceUrl = getInstance().getEndpoint()+REPO_ENDPOINT+slug;

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
        if (CACHE.containsKey(repoId)) {
            Repository result = CACHE.get(repoId);
            result.updateLastAccess();
            return result;
        } else {
            String resourceUrl = getInstance().getEndpoint()+REPO_ENDPOINT+repoId;

            try {
                String response = getInstance().get(resourceUrl);
                JsonParser parser = new JsonParser();
                JsonObject allAnswer = parser.parse(response).getAsJsonObject();
                JsonObject repoJSON = allAnswer.getAsJsonObject("repo");

                Repository result = createGson().fromJson(repoJSON, Repository.class);
                if (CACHE.size() > NB_REPO_BEFORE_CLEANING_CACHE) {
                    cleanCache();
                }
                result.updateLastAccess();
                CACHE.put(repoId, result);
                return result;
            } catch (IOException e) {
                getInstance().getLogger().warn("Error when getting repo id "+repoId+" : "+e.getMessage());
                return null;
            }
        }

    }


}
