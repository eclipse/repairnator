package fr.inria.spirals.jtravis.helpers;

import com.google.gson.JsonElement;
import fr.inria.spirals.jtravis.entities.BuildConfig;
import fr.inria.spirals.jtravis.entities.JobConfig;

/**
 * Created by urli on 22/12/2016.
 */
public class ConfigHelper extends AbstractHelper {
    public static BuildConfig getBuildConfigFromJsonElement(JsonElement jsonConfig) {
        return createGson().fromJson(jsonConfig, BuildConfig.class);
    }

    public static JobConfig getJobConfigFromJsonElement(JsonElement jsonConfig) {
        return createGson().fromJson(jsonConfig, JobConfig.class);
    }
}
