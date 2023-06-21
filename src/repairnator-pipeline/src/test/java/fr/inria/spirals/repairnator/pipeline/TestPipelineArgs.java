package fr.inria.spirals.repairnator.pipeline;


import ch.qos.logback.classic.Level;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAPResult;
import fr.inria.spirals.repairnator.LauncherUtils;
import fr.inria.spirals.repairnator.TravisInputBuild;
import fr.inria.spirals.repairnator.TravisLauncherUtils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.utils.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;

public class TestPipelineArgs {

    @After
    public void tearDown() throws IOException {
        RepairnatorConfig.deleteInstance();
    }

    @Test
    public void testDefaultArgsLauncher() throws Exception {
        Launcher launcher = new Launcher(new String[]{""});

        JSAPResult arguments = launcher.defineArgs().parse("");
        RepairnatorConfig config = RepairnatorConfig.getInstance();

        // help is disabled by default
        assertFalse(LauncherUtils.getArgHelp(arguments));

        // debug is disabled by default
        assertFalse(LauncherUtils.getArgDebug(arguments));
        //FIXME: Utils.getLoggersLevel() is null by default
        //assertEquals(Level.INFO, Utils.getLoggersLevel());

        // default output dir
        File defaultOutput = new File("./repairnator-output");
        assertEquals(defaultOutput, LauncherUtils.getArgOutput(arguments));
        assertEquals(defaultOutput.getAbsolutePath(), config.getOutputPath());

        // default mongoDB
        String defaultMongoDBHost = null;
        assertEquals(defaultMongoDBHost, LauncherUtils.getArgMongoDBHost(arguments));
        assertEquals(defaultMongoDBHost, config.getMongodbHost());
        String defaultMongoDBName = "repairnator";
        assertEquals(defaultMongoDBName, LauncherUtils.getArgMongoDBName(arguments));
        assertEquals(defaultMongoDBName, config.getMongodbName());

        // default smtp
        String defaultSmtpServer = null;
        assertEquals(defaultSmtpServer, LauncherUtils.getArgSmtpServer(arguments));
        assertEquals(defaultSmtpServer, config.getSmtpServer());
        int defaultSmtpPort = 25;
        assertEquals(defaultSmtpPort, LauncherUtils.getArgSmtpPort(arguments));
        assertEquals(defaultSmtpPort, config.getSmtpPort());
        boolean defaultSmtpTLS = false;
        assertEquals(defaultSmtpTLS, LauncherUtils.getArgSmtpTLS(arguments));
        assertEquals(defaultSmtpTLS, config.isSmtpTLS());
        String defaultSmtpUsername = null;
        assertEquals(defaultSmtpUsername, LauncherUtils.getArgSmtpUsername(arguments));
        assertEquals(defaultSmtpUsername, config.getSmtpUsername());

        // default notifyTo
        String[] defaultNotifyTo = new String[0];
        assertArrayEquals(defaultNotifyTo, LauncherUtils.getArgNotifyto(arguments));
        assertArrayEquals(defaultNotifyTo, config.getNotifyTo());

        // default push url
        String defaultPushUrl = null;
        assertEquals(defaultPushUrl, LauncherUtils.getArgMongoDBHost(arguments));
        assertEquals(defaultPushUrl, config.getMongodbHost());

        // default github parameters
        String defaultGithubOAuth = System.getenv("GITHUB_OAUTH");
        assertEquals(defaultGithubOAuth, LauncherUtils.getArgGithubOAuth(arguments));
        assertEquals(defaultGithubOAuth, config.getGithubToken());
        String defaultGithubUserName = "repairnator";
        assertEquals(defaultGithubUserName, LauncherUtils.getArgGithubUserName(arguments));
        assertEquals(defaultGithubUserName, config.getGithubUserName());
        String defaultGithubUserEmail = "noreply@github.com";
        assertEquals(defaultGithubUserEmail, LauncherUtils.getArgGithubUserEmail(arguments));
        assertEquals(defaultGithubUserEmail, config.getGithubUserEmail());

        // default z3
        String defaultZ3Path = "./z3_for_linux";
        assertEquals(defaultZ3Path, LauncherUtils.getArgZ3(arguments));
        assertEquals(new File(defaultZ3Path).getAbsolutePath(), config.getZ3solverPath());

        // default maven dirs
        File defaultMavenHome = new File("/usr/share/maven");
        assertEquals(defaultMavenHome, LauncherUtils.getArgMavenHome(arguments));
        assertEquals(defaultMavenHome.getPath(), config.getMavenHome());
        File defaultLocalMavenRepository = new File(System.getProperty("user.home") + "/.m2/repository");
        assertEquals(defaultLocalMavenRepository, LauncherUtils.getArgLocalMavenRepository(arguments));
        assertEquals(defaultLocalMavenRepository.getPath(), config.getLocalMavenRepository());

        // default workspace
        String defaultWorkspace = "./workspace";
        assertEquals(defaultWorkspace, LauncherUtils.getArgWorkspace(arguments));
        assertEquals(defaultWorkspace, config.getWorkspacePath());

        // default projects to ignore
        File defaultProjectsToIgnore = null;
        assertEquals(defaultProjectsToIgnore, LauncherUtils.getArgProjectsToIgnore(arguments));
        assertEquals(defaultProjectsToIgnore, config.getProjectsToIgnoreFilePath());

        // default listener mode
        RepairnatorConfig.LISTENER_MODE defaultListenerMode = RepairnatorConfig.LISTENER_MODE.NOOP;
        assertEquals(defaultListenerMode.name(), LauncherUtils.getArgListenerMode(arguments));
        assertEquals(defaultListenerMode, config.getListenerMode());

        // default ActiveMQ parameters
        String defaultActiveMQUrl = "tcp://localhost:61616";
        assertEquals(defaultActiveMQUrl, LauncherUtils.getArgActiveMQUrl(arguments));
        assertEquals(defaultActiveMQUrl, config.getActiveMQUrl());
        String defaultActiveMQName = "pipeline";
        assertEquals(defaultActiveMQName, LauncherUtils.getArgActiveMQListEnqueueName(arguments));
        assertEquals(defaultActiveMQName, config.getActiveMQListenQueueName());
        String defaultActiveMQUsername = "";
        assertEquals(defaultActiveMQUsername, LauncherUtils.getArgActiveMQUsername(arguments));
        assertEquals(defaultActiveMQUsername, config.getActiveMQUsername());
        String defaultActiveMQPassword = "";
        assertEquals(defaultActiveMQPassword, LauncherUtils.getArgActiveMQPassword(arguments));
        assertEquals(defaultActiveMQPassword, config.getActiveMQPassword());

        // default git parameters
        String defaultGitUrl = null;
        assertEquals(defaultGitUrl, LauncherUtils.getArgGitUrl(arguments));
        assertEquals(defaultGitUrl, config.getGitUrl());
        String defaultGitBranch = null;
        assertEquals(defaultGitBranch, LauncherUtils.getArgGitBranch(arguments));
        assertEquals(defaultGitBranch, config.getGitBranch());
        String defaultGitCommitHash = null;
        assertEquals(defaultGitCommitHash, LauncherUtils.getArgGitCommitHash(arguments));
        assertEquals(defaultGitCommitHash, config.getGitCommitHash());

        // default experimental plugins
        String[] defaultExperimentalPlugins = new String[0];
        assertArrayEquals(defaultExperimentalPlugins, LauncherUtils.getArgExperimentalPluginRepoList(arguments));
        assertArrayEquals(defaultExperimentalPlugins, config.getExperimentalPluginRepoList());

        // default tmpdir as workspace
        boolean defaultTmpDirAsWorkspace = false;
        assertEquals(defaultTmpDirAsWorkspace, LauncherUtils.getArgTmpDirAsWorkSpace(arguments));
        assertEquals(defaultTmpDirAsWorkspace, config.getTempWorkspace());

        // default sonar rules
        String defaultSonarRules = "2116";
        assertEquals(defaultSonarRules, LauncherUtils.getArgSonarRules(arguments));
        assertArrayEquals(Arrays.stream(defaultSonarRules.split(",")).distinct().toArray(String[]::new), config.getSonarRules());

        // default NPEFix rules
        String defaultNPESelection = "exploration";
        assertEquals(defaultNPESelection, LauncherUtils.getArgNPESelection(arguments));
        assertEquals(defaultNPESelection, config.getNPESelection());
        Integer defaultNbIteration = 100;
        assertEquals(defaultNbIteration, LauncherUtils.getArgNPENbIteration(arguments));
        assertEquals(defaultNbIteration, config.getNPENbIteration());
        String defaultNPEScope = "class";
        assertEquals(defaultNPEScope, LauncherUtils.getArgNPEScope(arguments));
        assertEquals(defaultNPEScope, config.getNPEScope());
        String defaultNPERepairStrategy = "default";
        assertEquals(defaultNPERepairStrategy, LauncherUtils.getArgNPERepairStrategy(arguments));
        assertEquals(defaultNPERepairStrategy, config.getNPERepairStrategy());

        // default patch classification parameters
        RepairnatorConfig.PATCH_CLASSIFICATION_MODE defaultPatchClassificationMode = RepairnatorConfig.PATCH_CLASSIFICATION_MODE.NONE;
        assertEquals(defaultPatchClassificationMode, LauncherUtils.getArgPatchClassificationMode(arguments));
        assertEquals(defaultPatchClassificationMode, config.getPatchClassificationMode());
        boolean defaultPatchClassification = false;
        assertEquals(defaultPatchClassification, LauncherUtils.getArgPatchClassification(arguments));
        assertEquals(defaultPatchClassification, config.isPatchClassification());

        // default patch filtering parameters
        RepairnatorConfig.PATCH_FILTERING_MODE defaultPatchFilteringMode = RepairnatorConfig.PATCH_FILTERING_MODE.NONE;
        assertEquals(defaultPatchFilteringMode, LauncherUtils.getArgPatchFilteringMode(arguments));
        assertEquals(defaultPatchFilteringMode, config.getPatchFilteringMode());
        boolean defaultPatchFiltering = false;
        assertEquals(defaultPatchFiltering, LauncherUtils.getArgPatchFiltering(arguments));
        assertEquals(defaultPatchFiltering, config.isPatchFiltering());

        // the default repair tool
        assertEquals(1, ((FlaggedOption) launcher.defineArgs().getByLongFlag("repairTools")).getDefault().length);
        assertEquals("NPEFix", ((FlaggedOption) launcher.defineArgs().getByLongFlag("repairTools")).getDefault()[0]);

        // non default value is accepted
        assertEquals("NopolAllTests", ((FlaggedOption) launcher.defineArgs().getByLongFlag("repairTools")).getStringParser().parse("NopolAllTests"));

        // incorrect values are rejected
        try {
            ((FlaggedOption) launcher.defineArgs().getByLongFlag("repairTools")).getStringParser().parse("garbage");
            fail();
        } catch (Exception expected) {
        }

    }

}