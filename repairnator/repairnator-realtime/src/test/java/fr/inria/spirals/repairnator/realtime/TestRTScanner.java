package fr.inria.spirals.repairnator.realtime;

import static fr.inria.spirals.repairnator.config.RepairnatorConfig.PIPELINE_MODE;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import fr.inria.jtravis.entities.Repository;
import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.InputBuildId;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.dockerpool.RunnablePipelineContainer;
import fr.inria.spirals.repairnator.serializer.SerializerType;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.json.JSONFileSerializerEngine;
import fr.inria.spirals.repairnator.states.LauncherMode;

import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class TestRTScanner {

    // a failing build from surli/failingProject
    public final int easyFailingBuild = 564711868;

    @Test
    public void testDockerPipelineRunner() throws Exception {
        RepairnatorConfig.getInstance().setRepairTools(new HashSet<>(Arrays.asList(new String[]{"NPEFix"})));
        DockerPipelineRunner d = new DockerPipelineRunner();
        d.initRunner();
        RunnablePipelineContainer runner = d.submitBuild(DockerPipelineRunner.REPAIRNATOR_PIPELINE_DOCKER_IMAGE_NAME, new InputBuildId(RepairnatorConfig.getInstance().getJTravis().build().fromId(easyFailingBuild).get().getId()));
        runner.run();
        assertEquals(0, runner.getExitStatus().statusCode().longValue());
    }

    @Test
    public void testRepositoryWithoutSuccessfulBuildIsNotInteresting() {
        String slug = "surli/failingProject";
        RepairnatorConfig.getInstance().setLauncherMode(LauncherMode.REPAIR);
        Optional<Repository> repositoryOptional = RepairnatorConfig.getInstance().getJTravis().repository().fromSlug(slug);
        assertTrue(repositoryOptional.isPresent());

        RTScanner rtScanner = new RTScanner("test", new ArrayList<>());
        boolean result = rtScanner.isRepositoryInteresting(repositoryOptional.get().getId());
        assertFalse(result);
    }

    @Test
    public void testRepositoryWithoutCheckstyleIsNotInteresting() {
        String slug = "surli/test-repairnator";
        RepairnatorConfig.getInstance().setLauncherMode(LauncherMode.CHECKSTYLE);
        Optional<Repository> repositoryOptional = RepairnatorConfig.getInstance().getJTravis().repository().fromSlug(slug);
        assertTrue(repositoryOptional.isPresent());

        RTScanner rtScanner = new RTScanner("test", new ArrayList<>());
        boolean result = rtScanner.isRepositoryInteresting(repositoryOptional.get().getId());
        assertFalse(result);
    }

    @Test
    public void testRepositoryWithoutCheckstyleIsInteresting() {
        String slug = "repairnator/embedded-cassandra";
        RepairnatorConfig.getInstance().setLauncherMode(LauncherMode.CHECKSTYLE);
        Optional<Repository> repositoryOptional = RepairnatorConfig.getInstance().getJTravis().repository().fromSlug(slug);
        assertTrue(repositoryOptional.isPresent());

        RTScanner rtScanner = new RTScanner("test", new ArrayList<>());
        boolean result = rtScanner.isRepositoryInteresting(repositoryOptional.get().getId());
        assertTrue(result);
    }

    @Test
    public void testRepositoryWithSuccessfulBuildIsInteresting() {
        String slug = "INRIA/spoon";
        RepairnatorConfig.getInstance().setLauncherMode(LauncherMode.REPAIR);
        Optional<Repository> repositoryOptional = RepairnatorConfig.getInstance().getJTravis().repository().fromSlug(slug);
        assertTrue(repositoryOptional.isPresent());

        RTScanner rtScanner = new RTScanner("test", new ArrayList<>());
        boolean result = rtScanner.isRepositoryInteresting(repositoryOptional.get().getId());
        assertTrue(result);
    }

    @Test
    public void testRepositoryWithoutJavaLanguageIsNotInteresting() {
        String slug = "rails/rails";
        RepairnatorConfig.getInstance().setLauncherMode(LauncherMode.REPAIR);
        Optional<Repository> repositoryOptional = RepairnatorConfig.getInstance().getJTravis().repository().fromSlug(slug);
        assertTrue(repositoryOptional.isPresent());

        RTScanner rtScanner = new RTScanner("test", new ArrayList<>());
        boolean result = rtScanner.isRepositoryInteresting(repositoryOptional.get().getId());
        assertFalse(result);
    }

    /**
     * Note this test might fail locally if you don't have activeMQ
     * In that case this test can be temporarily be commented out
     * Also this build is taken from Tailp/travisplay, so if
     * fetch another fail build from there or from another repo
     * if 560996872 disappears in the future.
     */
    @Test
    public void testActiveMQRunnerConnection()
    {
        int buildId = 560996872;
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setLauncherMode(LauncherMode.REPAIR);
        config.setPipelineMode(PIPELINE_MODE.KUBERNETES.name());
        config.setActiveMQUrl("tcp://localhost:61616");
        config.setActiveMQQueueName("testing");

        Optional<Build> optionalBuild = config.getJTravis().build().fromId(buildId);
        assertTrue(optionalBuild.isPresent());

        ActiveMQPipelineRunner runner = new ActiveMQPipelineRunner();
        RTScanner rtScanner = new RTScanner("test", new ArrayList<>(), runner);
        rtScanner.submitBuildToExecution(optionalBuild.get());
        assertEquals("560996872",runner.receiveBuildFromQueue());
    }

    @Test
    public void testBlacklisting() throws Exception {
      String fileName = "./"+ SerializerType.BLACKLISTED.getName()+".json";
      new File(fileName).delete();

      ArrayList<SerializerEngine> engines = new ArrayList<>();
      engines.add(new JSONFileSerializerEngine("."));
      RTScanner rtScanner = new RTScanner("test", engines);
      rtScanner.initBlackListedRepository(new File("./src/test/resources/blacklist.txt"));

      JsonReader reader = new JsonReader(new FileReader(fileName));
      JsonObject data = new Gson().fromJson(reader, JsonObject.class);
      assertEquals("INRIA/spoon", data.get("repoName").getAsString());

      data = new Gson().fromJson(reader, JsonObject.class);
      assertEquals("rails/rails", data.get("repoName").getAsString());
    }

}
