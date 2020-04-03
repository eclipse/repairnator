package fr.inria.spirals.repairnator.pipeline;

import com.martiansoftware.jsap.JSAPResult;

import fr.inria.spirals.repairnator.LauncherUtils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.spirals.repairnator.utils.Utils;

import java.util.Arrays;
import java.util.HashSet;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

import com.martiansoftware.jsap.JSAP;

import ch.qos.logback.classic.Level;

/* Config init behavior for the default use case of Repairnator */
public class DefaultInitConfig implements IInitConfig {
    private static Logger LOGGER = LoggerFactory.getLogger(DefaultInitConfig.class);
    private static File tempDir;

	protected static RepairnatorConfig getConfig() {
        return RepairnatorConfig.getInstance();
    }

    @Override
	public void initConfigWithJSAP(JSAP jsap,String[] inputArgs) {
        LauncherHelpers.getInstance().loadProperty();
        JSAPResult arguments = jsap.parse(inputArgs);

		if (LauncherUtils.getArgDebug(arguments)) {
            this.getConfig().setDebug(true);
        }
        this.getConfig().setClean(true);
        this.getConfig().setRunId(LauncherUtils.getArgRunId(arguments));
        this.getConfig().setGithubToken(LauncherUtils.getArgGithubOAuth(arguments));

        /* Remove later - already set at basic args at the launcher */
        if (LauncherUtils.gerArgBearsMode(arguments)) {
            this.getConfig().setLauncherMode(LauncherMode.BEARS);
        } else if (LauncherUtils.gerArgCheckstyleMode(arguments)) {
            this.getConfig().setLauncherMode(LauncherMode.CHECKSTYLE);
        } else {
            this.getConfig().setLauncherMode(LauncherMode.REPAIR);
        }
        if (LauncherUtils.getArgOutput(arguments) != null) {
            this.getConfig().setOutputPath(LauncherUtils.getArgOutput(arguments).getPath());
        }
        this.getConfig().setMongodbHost(LauncherUtils.getArgMongoDBHost(arguments));
        this.getConfig().setMongodbName(LauncherUtils.getArgMongoDBName(arguments));
        this.getConfig().setSmtpServer(LauncherUtils.getArgSmtpServer(arguments));
        this.getConfig().setSmtpPort(LauncherUtils.getArgSmtpPort(arguments));
        this.getConfig().setSmtpTLS(LauncherUtils.getArgSmtpTLS(arguments));
        this.getConfig().setSmtpUsername(LauncherUtils.getArgSmtpUsername(arguments));
        this.getConfig().setSmtpPassword(LauncherUtils.getArgSmtpPassword(arguments));
        this.getConfig().setNotifyTo(LauncherUtils.getArgNotifyto(arguments));

        if (LauncherUtils.getArgPushUrl(arguments) != null) {
            this.getConfig().setPush(true);
            this.getConfig().setPushRemoteRepo(LauncherUtils.getArgPushUrl(arguments));
        }
        this.getConfig().setCreatePR(LauncherUtils.getArgCreatePR(arguments));

        // we fork if we need to create a PR or if we need to notify (but only when we have a git token)
        if (this.getConfig().isCreatePR() || (this.getConfig().getSmtpServer() != null && !this.getConfig().getSmtpServer().isEmpty() && this.getConfig().getNotifyTo() != null && this.getConfig().getNotifyTo().length > 0 && this.getConfig().getGithubToken() != null)) {
            this.getConfig().setFork(true);
        }
        this.getConfig().setBuildId(arguments.getInt("build"));
        if (this.getConfig().getLauncherMode() == LauncherMode.BEARS) {
            this.getConfig().setNextBuildId(arguments.getInt("nextBuild"));
        }
        this.getConfig().setZ3solverPath(new File(arguments.getString("z3")).getPath());
        this.getConfig().setWorkspacePath(arguments.getString("workspace"));
        if (arguments.getBoolean("tmpDirAsWorkSpace")) {
            this.tempDir = com.google.common.io.Files.createTempDir();
            this.getConfig().setWorkspacePath(this.tempDir.getAbsolutePath());
            this.getConfig().setOutputPath(this.tempDir.getAbsolutePath());
            this.getConfig().setZ3solverPath(new File(this.tempDir.getAbsolutePath() + File.separator + "z3_for_linux").getPath());
        }

        this.getConfig().setGithubUserEmail(LauncherUtils.getArgGithubUserEmail(arguments));
        this.getConfig().setGithubUserName(LauncherUtils.getArgGithubUserName(arguments));
        this.getConfig().setListenerMode(arguments.getString("listenermode"));
        this.getConfig().setActiveMQUrl(arguments.getString("activemqurl"));
        this.getConfig().setActiveMQListenQueueName(arguments.getString("activemqlistenqueuename"));
        this.getConfig().setActiveMQUsername(arguments.getString("activemqusername"));
        this.getConfig().setActiveMQPassword(arguments.getString("activemqpassword"));

        this.getConfig().setGitUrl(arguments.getString("giturl"));
        this.getConfig().setGitBranch(arguments.getString("gitbranch"));
        this.getConfig().setGitCommitHash(arguments.getString("gitcommithash"));
        this.getConfig().setMavenHome(arguments.getString("MavenHome"));

        this.getConfig().setNoTravisRepair(arguments.getBoolean("noTravisRepair"));

        if (arguments.getFile("projectsToIgnore") != null) {
            this.getConfig().setProjectsToIgnoreFilePath(arguments.getFile("projectsToIgnore").getPath());
        }

        this.getConfig().setRepairTools(new HashSet<>(Arrays.asList(arguments.getStringArray("repairTools"))));
        if (this.getConfig().getLauncherMode() == LauncherMode.REPAIR) {
            LOGGER.info("The following repair tools will be used: " + StringUtils.join(this.getConfig().getRepairTools(), ", "));
        }

        // Make sure that it is a multiple of three in the list
        if((arguments.getStringArray("experimentalPluginRepoList").length) % 3 == 0) {
            this.getConfig().setExperimentalPluginRepoList(arguments.getStringArray("experimentalPluginRepoList"));
        } else if (arguments.getStringArray("experimentalPluginRepoList").length != 0) {
            LOGGER.warn("The experimental plugin repo list is not correctly formed."
                    + " Please make sure you have provided id, name and url for all repos. "
                    + "Repairnator will continue without these repos.");
            this.getConfig().setExperimentalPluginRepoList(null);
        } else {
            this.getConfig().setExperimentalPluginRepoList(null);
        }

        if (this.getConfig().getLauncherMode() == LauncherMode.REPAIR) {
            LauncherHelpers.getInstance().checkToolsLoaded(jsap);
            LauncherHelpers.getInstance().checkNopolSolverPath(jsap);
            LOGGER.info("The pipeline will try to repair the following build id: "+this.getConfig().getBuildId());
        } else {
            LauncherHelpers.getInstance().checkNextBuildId(jsap);
            LOGGER.info("The pipeline will try to reproduce a bug from build "+this.getConfig().getBuildId()+" and its corresponding patch from build "+this.getConfig().getNextBuildId());
        }

        if (this.getConfig().isDebug()) {
            Utils.setLoggersLevel(Level.DEBUG);
        } else {
            Utils.setLoggersLevel(Level.INFO);
        }
	}
}