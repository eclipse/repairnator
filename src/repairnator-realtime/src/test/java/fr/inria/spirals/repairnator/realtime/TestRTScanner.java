package fr.inria.spirals.repairnator.realtime;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import fr.inria.jtravis.entities.Repository;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.serializer.SerializerType;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.json.JSONFileSerializerEngine;
import fr.inria.spirals.repairnator.states.LauncherMode;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestRTScanner {
    
    @Test
    public void testRepositoryWithoutSuccessfulBuildIsNotInteresting() {
        String slug = "repairnator/failingProject";
        RepairnatorConfig.getInstance().setLauncherMode(LauncherMode.REPAIR);
        Optional<Repository> repositoryOptional = getOptionalRepository(slug);

        RTScanner rtScanner = new RTScanner("test", new ArrayList<>());
        boolean result = rtScanner.isRepositoryInteresting(repositoryOptional.get().getId());
        assertFalse(result);
    }

    @Test
    public void testRepositoryWithoutCheckstyleIsNotInteresting() {
        String slug = "repairnator/test-repairnator";
        RepairnatorConfig.getInstance().setLauncherMode(LauncherMode.CHECKSTYLE);
        Optional<Repository> repositoryOptional = getOptionalRepository(slug);

        RTScanner rtScanner = new RTScanner("test", new ArrayList<>());
        boolean result = rtScanner.isRepositoryInteresting(repositoryOptional.get().getId());
        assertFalse(result);
    }

    @Test
    public void testRepositoryWithSuccessfulBuildIsInteresting() {
        String slug = "eclipse/repairnator";
        RepairnatorConfig.getInstance().setLauncherMode(LauncherMode.REPAIR);
        RepairnatorConfig.getInstance().setJTravisEndpoint("https://api.travis-ci.com");
        Optional<Repository> repositoryOptional = getOptionalRepository(slug);

        RTScanner rtScanner = new RTScanner("test", new ArrayList<>());
        boolean result = rtScanner.isRepositoryInteresting(repositoryOptional.get().getId());
        assertTrue(result);
    }

    @Test
    public void testRepositoryWithoutJavaLanguageIsNotInteresting() {
        String slug = "rails/rails";
        RepairnatorConfig.getInstance().setLauncherMode(LauncherMode.REPAIR);
        Optional<Repository> repositoryOptional = getOptionalRepository(slug);

        RTScanner rtScanner = new RTScanner("test", new ArrayList<>());
        boolean result = rtScanner.isRepositoryInteresting(repositoryOptional.get().getId());
        assertFalse(result);
    }

    @Test
    public void testBlacklisting() throws Exception {
      String fileName = "./"+ SerializerType.BLACKLISTED.getName()+".json";
      new File(fileName).delete();

      ArrayList<SerializerEngine> engines = new ArrayList<>();
      engines.add(new JSONFileSerializerEngine("."));
      RTScanner rtScanner = new RTScanner("test", engines);
      rtScanner.initBlackListedRepository(new File("./src/test/resources/blacklist.txt"));
      rtScanner.saveInfoToDisk();

      JsonReader reader = new JsonReader(new FileReader(fileName));
      JsonObject data = new Gson().fromJson(reader, JsonObject.class);
      assertEquals("INRIA/spoon", data.get("repoName").getAsString());

      data = new Gson().fromJson(reader, JsonObject.class);
      assertEquals("rails/rails", data.get("repoName").getAsString());
    }

    private static Optional<Repository> getOptionalRepository(String slug) {
        Optional<Repository> repositoryOptional = RepairnatorConfig.getInstance().getJTravis().repository().fromSlug(slug);
        int retryCount = 0;
        while(!repositoryOptional.isPresent()) {
            if (retryCount > 3) {
                break;
            }
        }
        assertTrue(repositoryOptional.isPresent());
        return repositoryOptional;
    }
}
