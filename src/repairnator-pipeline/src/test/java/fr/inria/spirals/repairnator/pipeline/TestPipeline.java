package fr.inria.spirals.repairnator.pipeline;

import ch.qos.logback.classic.Level;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAPResult;
import fr.inria.spirals.repairnator.InputBuildId;
import fr.inria.spirals.repairnator.LauncherUtils;
import fr.inria.spirals.repairnator.TravisLauncherUtils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.PatchNotifier;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;
import fr.inria.spirals.repairnator.utils.Utils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class TestPipeline {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testDefaultArgsLauncher() throws Exception {
        Launcher launcher = new Launcher();

        JSAPResult arguments = launcher.defineArgs().parse("");
        RepairnatorConfig config = RepairnatorConfig.getInstance();

        // help is disabled by default
		assertFalse(LauncherUtils.getArgHelp(arguments));

		// debug is disabled by default
		assertFalse(LauncherUtils.getArgDebug(arguments));
		assertEquals(Level.INFO, Utils.getLoggersLevel());

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
		File defaultLocalMavenRepository = new File(System.getenv("HOME") + "/.m2/repository");
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
		RepairnatorConfig.SORALD_REPAIR_MODE defaultSoraldRepairMode = RepairnatorConfig.SORALD_REPAIR_MODE.DEFAULT;
		assertEquals(defaultSoraldRepairMode.name(), LauncherUtils.getArgSoraldRepairMode(arguments));
		assertEquals(defaultSoraldRepairMode, config.getSoraldRepairMode());
		Integer defaultSegmentSize = 200;
		assertEquals(defaultSegmentSize, LauncherUtils.getArgSegmentSize(arguments));
		assertEquals((int) defaultSegmentSize, config.getSegmentSize());
		Integer defaultMaxFixesPerRule = 2000;
		assertEquals(defaultMaxFixesPerRule, LauncherUtils.getArgSoraldMaxFixesPerRule(arguments));
		assertEquals((int) defaultMaxFixesPerRule, config.getSoraldMaxFixesPerRule());

		// default patch ranking mode
		RepairnatorConfig.PATCH_RANKING_MODE defaultPatchRankingMode = RepairnatorConfig.PATCH_RANKING_MODE.NONE;
		assertEquals(defaultPatchRankingMode.name(), LauncherUtils.getArgPatchRankingMode(arguments));
		assertEquals(defaultPatchRankingMode, config.getPatchRankingMode());

		// travis arguments
		Integer defaultNextBuildId = InputBuildId.NO_PATCH;
		assertEquals(defaultNextBuildId, TravisLauncherUtils.getArgNextBuild(arguments));
		boolean defaultNoTravisRepair = false;
		assertEquals(defaultNoTravisRepair, TravisLauncherUtils.getArgNoTravisRepair(arguments));
		assertEquals(defaultNoTravisRepair, config.isNoTravisRepair());
		String defaultJTravisEndpoint = "https://api.travis-ci.org";
		assertEquals(defaultJTravisEndpoint, TravisLauncherUtils.getArgJTravisEndpoint(arguments));
		assertEquals(defaultJTravisEndpoint, config.getJTravisEndpoint());
		String defaultJTravisToken = "";
		assertEquals(defaultJTravisToken, TravisLauncherUtils.getArgTravisToken(arguments));
		assertEquals(defaultJTravisToken, config.getTravisToken());

		// the default repair tool
        assertEquals(1, ((FlaggedOption)launcher.defineArgs().getByLongFlag("repairTools")).getDefault().length);
        assertEquals("NPEFix", ((FlaggedOption)launcher.defineArgs().getByLongFlag("repairTools")).getDefault()[0]);

        // non default value is accepted
        assertEquals("NopolAllTests", ((FlaggedOption)launcher.defineArgs().getByLongFlag("repairTools")).getStringParser().parse("NopolAllTests"));

        // incorrect values are rejected
        try {
            ((FlaggedOption)launcher.defineArgs().getByLongFlag("repairTools")).getStringParser().parse("garbage");
            fail();
        } catch (Exception expected) {}

    }

    @Test
    public void testPipeline() throws Exception {
        // requires env variable M2_HOME and GITHUB_OAUTH
        // (set in Travis config)
        // eg export M2_HOME=/usr/share/maven
        // from surli/failingBuild
        Launcher l = new Launcher(new String[]{"--build", "564711868", "--repairTools", "NPEFix"});
		Patches patchNotifier = new Patches();
		l.setPatchNotifier(patchNotifier);
		l.mainProcess();
        assertEquals("PATCHED", l.getInspector().getFinding());
		assertEquals(10, patchNotifier.allpatches.size());
		assertTrue("patch is found", patchNotifier.allpatches.get(0).getDiff().contains("list == null"));

	}

    @Test
    public void testPipelineOnlyGitRepository() throws Exception {
        GitRepositoryLauncher l = new GitRepositoryLauncher(new String[]{
        			"--gitrepo",
        			"--gitrepourl", "https://github.com/surli/failingProject",
        		});

        Patches patchNotifier = new Patches();
		l.setPatchNotifier(patchNotifier);
        l.mainProcess();
        assertEquals("PATCHED", l.getInspector().getFinding());
		assertEquals(10, patchNotifier.allpatches.size());
		assertTrue("patch is found", patchNotifier.allpatches.get(0).getDiff().contains("list == null"));
	}

    @Test
    public void testPipelineGitRepositoryAndBranch() throws Exception {
    	GitRepositoryLauncher l = new GitRepositoryLauncher(new String[]{
	        		"--gitrepo",
	        		"--gitrepourl", "https://github.com/surli/failingProject",
	        		"--gitrepobranch", "astor-jkali-failure",
	        		"--repairTools", "AstorJKali"
        		});

        l.mainProcess();
        assertEquals("TEST FAILURE", l.getInspector().getFinding());
	}

    @Test
    public void testPipelineGitRepositoryAndCommitIdWithFailure() throws Exception {
    	GitRepositoryLauncher l = new GitRepositoryLauncher(new String[]{
        		"--gitrepo",
        		"--gitrepourl", "https://github.com/javierron/failingProject",
				"--gitrepoidcommit", "883bc40f01902654b1b1df094b2badb28e192097",
				"--gitrepobranch", "nofixes",
        		});

        l.mainProcess();
        assertEquals("TEST FAILURE", l.getInspector().getFinding());
	}

    @Test
    public void testPipelineGitRepositoryAndCommitIdWithSuccess() throws Exception {
    	GitRepositoryLauncher l = new GitRepositoryLauncher(new String[]{
        		"--gitrepo",
        		"--gitrepourl", "https://github.com/surli/failingProject",
        		"--gitrepoidcommit", "7e1837df8db7a563fba65f75f7f477c43c9c75e9",
                "--workspace", folder.getRoot().getAbsolutePath()
        		});

        Patches patchNotifier = new Patches();
		l.setPatchNotifier(patchNotifier);
        l.mainProcess();
        assertEquals("PATCHED", l.getInspector().getFinding());
		assertEquals(10, patchNotifier.allpatches.size());
		assertTrue("patch is found", patchNotifier.allpatches.get(0).getDiff().contains("list == null"));
	}

    @Ignore
    @Test
    public void testPipelineGitRepositoryFirstCommit() throws Exception {
    	GitRepositoryLauncher l = new GitRepositoryLauncher(new String[]{
        		"--gitrepo",
        		"--gitrepourl", "https://github.com/surli/failingProject",
        		"--gitrepofirstcommit"
        		});

        l.mainProcess();
        assertEquals("NOTBUILDABLE", l.getInspector().getFinding());
	}

    class Patches implements PatchNotifier {
    	List<RepairPatch> allpatches = new ArrayList<>();
		@Override
		public void notify(ProjectInspector inspector, String toolname, List<RepairPatch> patches) {
			allpatches.addAll(patches);
		}
	}

	@Ignore
	@Test
	public void testPRLuc12() throws Exception {
    	// reproducing the 12th PR of Luc
		// see https://github.com/eclipse/repairnator/issues/758
		Launcher l = new Launcher(new String[]{"--build", "395891390", "--repairTools", "NPEFix" });
		Patches patchNotifier = new Patches();
		l.setPatchNotifier(patchNotifier);
		l.mainProcess();
		assertEquals("PATCHED", l.getInspector().getFinding());
		assertEquals(1, patchNotifier.allpatches.size());
		assertTrue("patch is found", patchNotifier.allpatches.get(0).getDiff().contains("hashtagStore != null"));	}

}
