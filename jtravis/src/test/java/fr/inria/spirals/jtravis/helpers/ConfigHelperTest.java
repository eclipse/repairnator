package fr.inria.spirals.jtravis.helpers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.inria.spirals.jtravis.entities.BuildConfig;
import fr.inria.spirals.jtravis.entities.JobConfig;
import okhttp3.ResponseBody;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Created by urli on 22/12/2016.
 */
public class ConfigHelperTest {
    private class Helper extends AbstractHelper {
        public Helper() {
            super();
        }

        public JsonElement getJsonBuildConfigElement(int buildId) throws IOException {
            ResponseBody response = this.get(TRAVIS_API_ENDPOINT+BuildHelper.BUILD_ENDPOINT+buildId);
            JsonParser parser = new JsonParser();
            JsonObject allAnswer = parser.parse(response.string()).getAsJsonObject();
            JsonObject jsonBuild = allAnswer.getAsJsonObject("build");
            return jsonBuild.get("config");
        }

        public JsonElement getJsonJobConfigElement(int buildId) throws IOException {
            ResponseBody response = this.get(TRAVIS_API_ENDPOINT+BuildHelper.BUILD_ENDPOINT+buildId);
            JsonParser parser = new JsonParser();
            JsonObject allAnswer = parser.parse(response.string()).getAsJsonObject();
            JsonArray jsonJobs = allAnswer.getAsJsonArray("jobs");
            JsonObject jsonJob = jsonJobs.get(0).getAsJsonObject();
            return jsonJob.get("config");
        }
    }

    @Test
    public void testConfigHelperBuildAProperBuildConfigObject() throws IOException {
        int buildId = 185719843;

        BuildConfig expectedConfig = new BuildConfig();
        expectedConfig.setLanguage("java");
        expectedConfig.setJdk(Arrays.asList(new String[]{"oraclejdk8"}));
        expectedConfig.setGroup("stable");
        expectedConfig.setDist("precise");

        Helper helper = new Helper();
        JsonElement jsonConfig = helper.getJsonBuildConfigElement(buildId);

        BuildConfig obtainedConfig = ConfigHelper.getBuildConfigFromJsonElement(jsonConfig);
        assertEquals(expectedConfig, obtainedConfig);
    }

    @Test
    public void testConfigHelperBuildAProperJobConfigObject() throws IOException {
        int buildId = 185719843;

        JobConfig expectedConfig = new JobConfig();
        expectedConfig.setLanguage("java");
        expectedConfig.setJdk("oraclejdk8");

        expectedConfig.setGroup("stable");
        expectedConfig.setDist("precise");
        expectedConfig.setOs("linux");

        Helper helper = new Helper();
        JsonElement jsonConfig = helper.getJsonJobConfigElement(buildId);

        JobConfig obtainedConfig = ConfigHelper.getJobConfigFromJsonElement(jsonConfig);
        assertEquals(expectedConfig, obtainedConfig);
    }
}
