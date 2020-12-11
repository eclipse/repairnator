package fr.inria.spirals.repairnator.pipeline.github;

import static fr.inria.spirals.repairnator.config.RepairnatorConfig.SORALD_REPAIR_MODE;

import com.martiansoftware.jsap.JSAPResult;

import fr.inria.spirals.repairnator.LauncherUtils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.spirals.repairnator.GitRepositoryLauncherUtils;
import fr.inria.spirals.repairnator.LauncherType;
import fr.inria.spirals.repairnator.pipeline.IInitConfig;

import java.util.Arrays;
import java.util.HashSet;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

import com.martiansoftware.jsap.JSAP;

/* Config init behavior for repairing with Github instead of Travis */
public class GithubInitConfig implements IInitConfig {
    private static Logger LOGGER = LoggerFactory.getLogger(GithubInitConfig.class);
    private static File tempDir;

	protected static RepairnatorConfig getConfig() {
        return RepairnatorConfig.getInstance();
    }

    @Override
	public void initConfigWithJSAP(JSAP jsap, String[] inputArgs) {
        JSAPResult arguments = jsap.parse(inputArgs);
		if (LauncherUtils.getArgDebug(arguments)) {
            getConfig().setDebug(true);
        }
        getConfig().setClean(true);
        getConfig().setRunId(LauncherUtils.getArgRunId(arguments));
        getConfig().setGithubToken(LauncherUtils.getArgGithubOAuth(arguments));
        
        if (GitRepositoryLauncherUtils.getArgGitRepositoryMode(arguments)) {
            if (GitRepositoryLauncherUtils.getArgGitRepositoryFirstCommit(arguments)) {
                getConfig().setGitRepositoryFirstCommit(true);
            }
        } else {
            System.err.println("Error: Parameter 'gitrepo' is required in GIT_REPOSITORY launcher mode.");
        }
        
        if (LauncherUtils.getArgOutput(arguments) != null) {
            getConfig().setOutputPath(LauncherUtils.getArgOutput(arguments).getPath());
        }
        getConfig().setMongodbHost(LauncherUtils.getArgMongoDBHost(arguments));
        getConfig().setMongodbName(LauncherUtils.getArgMongoDBName(arguments));
        getConfig().setSmtpServer(LauncherUtils.getArgSmtpServer(arguments));
        getConfig().setSmtpPort(LauncherUtils.getArgSmtpPort(arguments));
        getConfig().setSmtpTLS(LauncherUtils.getArgSmtpTLS(arguments));
        getConfig().setSmtpUsername(LauncherUtils.getArgSmtpUsername(arguments));
        getConfig().setSmtpPassword(LauncherUtils.getArgSmtpPassword(arguments));
        getConfig().setNotifyTo(LauncherUtils.getArgNotifyto(arguments));

        if (LauncherUtils.getArgPushUrl(arguments) != null) {
            getConfig().setPush(true);
            getConfig().setPushRemoteRepo(LauncherUtils.getArgPushUrl(arguments));
        }
        getConfig().setCreatePR(LauncherUtils.getArgCreatePR(arguments));

        // we fork if we need to create a PR or if we need to notify
        if ((getConfig().isCreatePR() || (getConfig().getSmtpServer() != null && !getConfig().getSmtpServer().isEmpty() && getConfig().getNotifyTo() != null && getConfig().getNotifyTo().length > 0)) && getConfig().getGithubToken() != null) {
            getConfig().setFork(true);
        }

        if (arguments.getString("gitRepositoryUrl") == null) {
            System.err.println("Error: Parameter 'gitrepourl' is required in GIT_REPOSITORY launcher mode.");
        }

        if (getConfig().isGitRepositoryFirstCommit() && arguments.getString("gitRepositoryIdCommit") != null) {
            System.err.println("Error: Parameters 'gitrepofirstcommit' and 'gitrepoidcommit' cannot be used at the same time.");
        }

        getConfig().setGitRepositoryUrl(arguments.getString("gitRepositoryUrl"));
        getConfig().setGitRepositoryBranch(arguments.getString("gitRepositoryBranch"));
        getConfig().setGitRepositoryIdCommit(arguments.getString("gitRepositoryIdCommit"));
        

        getConfig().setZ3solverPath(new File(arguments.getString("z3")).getPath());
        getConfig().setWorkspacePath(arguments.getString("workspace"));
        if (arguments.getBoolean("tmpDirAsWorkSpace")) {
            tempDir = com.google.common.io.Files.createTempDir();
            getConfig().setWorkspacePath(tempDir.getAbsolutePath());
            getConfig().setOutputPath(tempDir.getAbsolutePath());
            getConfig().setZ3solverPath(new File(tempDir.getAbsolutePath() + File.separator + "z3_for_linux").getPath());
        }

        getConfig().setGithubUserEmail(LauncherUtils.getArgGithubUserEmail(arguments));
        getConfig().setGithubUserName(LauncherUtils.getArgGithubUserName(arguments));
        getConfig().setListenerMode(arguments.getString("listenermode"));
        getConfig().setActiveMQUrl(arguments.getString("activemqurl"));
        getConfig().setActiveMQListenQueueName(arguments.getString("activemqlistenqueuename"));

        getConfig().setGitUrl(arguments.getString("giturl"));
        getConfig().setGitBranch(arguments.getString("gitbranch"));
        getConfig().setGitCommitHash(arguments.getString("gitcommithash"));
        getConfig().setMavenHome(arguments.getString("MavenHome"));
        getConfig().setLocalMavenRepository(arguments.getString("localMavenRepository"));

        if (arguments.getFile("projectsToIgnore") != null) {
            getConfig().setProjectsToIgnoreFilePath(arguments.getFile("projectsToIgnore").getPath());
        }

        getConfig().setRepairTools(new HashSet<>(Arrays.asList(arguments.getStringArray("repairTools"))));
        
        // Make sure that it is a multiple of three in the list
        if((arguments.getStringArray("experimentalPluginRepoList").length) % 3 == 0) {
            getConfig().setExperimentalPluginRepoList(arguments.getStringArray("experimentalPluginRepoList"));
        } else if (arguments.getStringArray("experimentalPluginRepoList").length != 0) {
            LOGGER.warn("The experimental plugin repo list is not correctly formed."
                    + " Please make sure you have provided id, name and url for all repos. "
                    + "Repairnator will continue without these repos.");
            getConfig().setExperimentalPluginRepoList(null);
        } else {
            getConfig().setExperimentalPluginRepoList(null);
        }

        getConfig().setSonarRules(removeDuplicatesInArray(arguments.getString("sonarRules").split(",")));
        getConfig().setSegmentSize(arguments.getInt("segmentSize"));
        getConfig().setSoraldRepairMode(SORALD_REPAIR_MODE.valueOf(arguments.getString("soraldRepairMode")));
        getConfig().setSoraldMaxFixesPerRule(arguments.getInt("soraldMaxFixesPerRule"));
	}

    private static String[] removeDuplicatesInArray(String[] arr) {
        Set<String> set = new HashSet<String>();
        for(int i = 0; i < arr.length; i++){
          set.add(arr[i]);
        }
        return set.stream().toArray(String[]::new);
    }
}