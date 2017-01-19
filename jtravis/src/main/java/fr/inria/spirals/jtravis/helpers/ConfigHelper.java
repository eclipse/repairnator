package fr.inria.spirals.jtravis.helpers;

import com.google.gson.JsonElement;
import fr.inria.spirals.jtravis.entities.Config;

/**
 * The helper to deal with config objects (both JobConfig and BuildConfig)
 *
 * @author Simon Urli
 */
public class ConfigHelper extends AbstractHelper {
    public static Config getConfigFromJsonElement(JsonElement jsonConfig) {
        return createGson().fromJson(jsonConfig, Config.class);
    }
}
