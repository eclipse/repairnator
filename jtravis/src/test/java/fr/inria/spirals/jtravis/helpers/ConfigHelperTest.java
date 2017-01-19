package fr.inria.spirals.jtravis.helpers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.inria.spirals.jtravis.entities.Config;
import okhttp3.ResponseBody;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by urli on 22/12/2016.
 */
public class ConfigHelperTest {
    private class Helper extends AbstractHelper {
        public Helper() {
            super();
        }

        public JsonElement getJsonConfigElement(int buildId) throws IOException {
            String response = this.get(getEndpoint()+BuildHelper.BUILD_ENDPOINT+buildId);
            JsonParser parser = new JsonParser();
            JsonObject allAnswer = parser.parse(response).getAsJsonObject();
            JsonObject jsonBuild = allAnswer.getAsJsonObject("build");
            return jsonBuild.get("config");
        }
    }

    @Test
    public void testConfigHelperBuildAProperConfigObject() throws IOException {
        int buildId = 185719843;

        Config expectedConfig = new Config();
        expectedConfig.setLanguage("java");

        Helper helper = new Helper();
        JsonElement jsonConfig = helper.getJsonConfigElement(buildId);

        Config obtainedConfig = ConfigHelper.getConfigFromJsonElement(jsonConfig);
        assertEquals(expectedConfig, obtainedConfig);
    }
}
