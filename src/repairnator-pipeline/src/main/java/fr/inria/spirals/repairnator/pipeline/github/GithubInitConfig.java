package fr.inria.spirals.repairnator.pipeline.github;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.pipeline.IInitConfig;
import fr.inria.spirals.repairnator.GitRepositoryLauncherUtils;
import fr.inria.spirals.repairnator.LauncherUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;

/* Config init behavior for repairing with Github instead of Travis */
public class GithubInitConfig implements IInitConfig {
    private static Logger LOGGER = LoggerFactory.getLogger(GithubInitConfig.class);

    protected static RepairnatorConfig getConfig() {
        return RepairnatorConfig.getInstance();
    }

    @Override
    public void initConfigWithJSAP(JSAP jsap, String[] inputArgs) {
        JSAPResult arguments = jsap.parse(inputArgs);

        LauncherUtils.initCommonConfig(getConfig(), arguments);
        GitRepositoryLauncherUtils.initGitConfig(getConfig(), arguments, jsap);

        getConfig().setRepairTools(new HashSet<>(Arrays.asList(arguments.getStringArray("repairTools"))));

        getConfig().setWorkspacePath(arguments.getString("workspace"));
    }

}