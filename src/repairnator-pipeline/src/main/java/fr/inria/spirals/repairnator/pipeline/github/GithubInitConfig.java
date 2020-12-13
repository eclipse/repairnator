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
        LauncherUtils.initCommonConfig(this.getConfig(), arguments);

        if (GitRepositoryLauncherUtils.getArgGitRepositoryMode(arguments)) {
            if (GitRepositoryLauncherUtils.getArgGitRepositoryFirstCommit(arguments)) {
                getConfig().setGitRepositoryFirstCommit(true);
            }
        } else {
            System.err.println("Error: Parameter 'gitrepo' is required in GIT_REPOSITORY launcher mode.");
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