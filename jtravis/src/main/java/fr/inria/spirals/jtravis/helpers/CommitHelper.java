package fr.inria.spirals.jtravis.helpers;

import com.google.gson.JsonElement;
import fr.inria.spirals.jtravis.entities.Commit;

/**
 * The helper to deal with Commit objects
 *
 * @author Simon Urli
 */
public class CommitHelper extends AbstractHelper {

    public static Commit getCommitFromJsonElement(JsonElement element) {
        return createGson().fromJson(element, Commit.class);
    }
}
