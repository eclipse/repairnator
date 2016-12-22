package fr.inria.spirals.jtravis.helpers;

import com.google.gson.JsonElement;
import fr.inria.spirals.jtravis.entities.Commit;

/**
 * Created by urli on 22/12/2016.
 */
public class CommitHelper extends AbstractHelper {

    public static Commit getCommitFromJsonElement(JsonElement element) {
        return createGson().fromJson(element, Commit.class);
    }
}
