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
        expectedConfig.setSudo("required");
        expectedConfig.setJdk(Arrays.asList(new String[]{"oraclejdk8"}));
        expectedConfig.setInstall(Arrays.asList(new String[]{ null, "mvn dependency:resolve", "pip install --user CommonMark requests", "sudo apt-get install xmlstarlet"}));

        String configScript = "# compiles and install\n" +
                "mvn install -DskipTests &&\n" +
                "\n" +
                "# checks that it works with spoon-maven-pluging\n" +
                "git clone https://github.com/square/javawriter.git &&\n" +
                "cd javawriter &&  \n" +
                "git checkout d39761f9ec25ca5bf3b7bf15d34fa2b831fed9c1 &&\n" +
                "bash ../doc/jenkins/build.sh &&\n" +
                "cd .. &&\n" +
                "rm -rf javawriter &&\n" +
                "\n" +
                "# checkstyle, license, javadoc, animal sniffer.\n" +
                "mvn verify site -DskipTests &&\n" +
                "\n" +
                "# the unit tests\n" +
                "mvn test jacoco:report  &&\n" +
                "\n" +
                "# uploading coverage, but not failing\n" +
                "mvn coveralls:report -Pcoveralls --fail-never &&\n" +
                "\n" +
                "# documentation\n" +
                "python chore/check-links-in-doc.py\n";

        expectedConfig.setScript(configScript);
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
        expectedConfig.setSudo("required");
        expectedConfig.setJdk("oraclejdk8");
        expectedConfig.setInstall(Arrays.asList(new String[]{ null, "mvn dependency:resolve", "pip install --user CommonMark requests", "sudo apt-get install xmlstarlet"}));

        String configScript = "# compiles and install\n" +
                "mvn install -DskipTests &&\n" +
                "\n" +
                "# checks that it works with spoon-maven-pluging\n" +
                "git clone https://github.com/square/javawriter.git &&\n" +
                "cd javawriter &&  \n" +
                "git checkout d39761f9ec25ca5bf3b7bf15d34fa2b831fed9c1 &&\n" +
                "bash ../doc/jenkins/build.sh &&\n" +
                "cd .. &&\n" +
                "rm -rf javawriter &&\n" +
                "\n" +
                "# checkstyle, license, javadoc, animal sniffer.\n" +
                "mvn verify site -DskipTests &&\n" +
                "\n" +
                "# the unit tests\n" +
                "mvn test jacoco:report  &&\n" +
                "\n" +
                "# uploading coverage, but not failing\n" +
                "mvn coveralls:report -Pcoveralls --fail-never &&\n" +
                "\n" +
                "# documentation\n" +
                "python chore/check-links-in-doc.py\n";

        expectedConfig.setScript(configScript);
        expectedConfig.setGroup("stable");
        expectedConfig.setDist("precise");
        expectedConfig.setOs("linux");

        Helper helper = new Helper();
        JsonElement jsonConfig = helper.getJsonJobConfigElement(buildId);

        JobConfig obtainedConfig = ConfigHelper.getJobConfigFromJsonElement(jsonConfig);
        assertEquals(expectedConfig, obtainedConfig);
    }
}
