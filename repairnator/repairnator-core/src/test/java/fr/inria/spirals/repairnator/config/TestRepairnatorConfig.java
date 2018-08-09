package fr.inria.spirals.repairnator.config;

import fr.inria.spirals.repairnator.Utils;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Created by urli on 08/03/2017.
 */
public class TestRepairnatorConfig {

    private static final String CURRENT_USERDIR = System.getProperty("user.dir");

    @After
    public void tearDown() {
        System.setProperty("user.dir", CURRENT_USERDIR);
    }

    @Ignore
    @Test
    public void testReadConfigFromIniInResources() throws RepairnatorConfigException {
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.readFromFile();

        assertThat(config.getOutputPath(), is("/tmp"));
        assertThat(config.isClean(), is(true));
        assertThat(config.isPush(), is(false));
        assertThat(config.isSerializeJson(), is(true));
        assertThat(config.getLauncherMode(), nullValue());
        assertThat(config.getWorkspacePath(), is("./workspace"));
        assertThat(config.getZ3solverPath(), is(""));
    }

    @Ignore
    @Test
    public void testReadConfigFromUserDir() throws IOException, RepairnatorConfigException {
        Path tempUserDir = Files.createTempDirectory("temp-user-dir");

        File resourceConfig = new File("src/test/resources/example_config.ini");
        File dest = new File(tempUserDir.toFile().getAbsolutePath()+"/"+RepairnatorConfigReader.FILENAME);
        Files.copy(resourceConfig.toPath(), dest.toPath());

        System.setProperty("user.dir", tempUserDir.toFile().getAbsolutePath());

        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.readFromFile();

        assertThat(config.getOutputPath(), is(""));
        assertThat(config.isClean(), is(true));
        assertThat(config.isPush(), is(true));
        assertThat(config.isSerializeJson(), is(false));
        assertThat(config.getLauncherMode(), nullValue());
        assertThat(config.getWorkspacePath(), is("/tmp"));
        assertThat(config.getZ3solverPath(), is("/tmp/z3/z3_for_linux"));

        System.setProperty("user.dir", CURRENT_USERDIR);
    }

    /**
     * This test is not needed anymore: all github OAuth token are given directly in the command line.
     * @throws IOException
     */
    @Ignore
    @Test
    public void testGithubOauth() throws IOException {
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setGithubToken(System.getenv(Utils.GITHUB_OAUTH));
        GitHub gitHub = config.getGithub();
        GHRateLimit ghRateLimit = gitHub.rateLimit();

        assertEquals("OAuth is not working", 5000, ghRateLimit.limit);
    }

    @Test
    public void testToString() {
        String s = RepairnatorConfig.getInstance().toString();
        assertNotNull(s);
    }
}
