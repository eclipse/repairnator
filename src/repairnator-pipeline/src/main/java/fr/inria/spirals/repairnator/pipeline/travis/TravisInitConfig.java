package fr.inria.spirals.repairnator.pipeline.travis;

import com.martiansoftware.jsap.JSAPResult;

import fr.inria.spirals.repairnator.LauncherUtils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.spirals.repairnator.utils.Utils;
import fr.inria.spirals.repairnator.pipeline.IInitConfig;
import fr.inria.spirals.repairnator.pipeline.LauncherHelpers;

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
public class TravisInitConfig implements IInitConfig {
    private static Logger LOGGER = LoggerFactory.getLogger(TravisInitConfig.class);
    private static File tempDir;

	protected static RepairnatorConfig getConfig() {
        return RepairnatorConfig.getInstance();
    }

    @Override
	public void initConfigWithJSAP(JSAP jsap,String[] inputArgs) {
        LauncherHelpers.getInstance().loadProperty();
        JSAPResult arguments = jsap.parse(inputArgs);

        LauncherUtils.initCommonConfig(this.getConfig(), arguments);

        /* Remove later - already set at basic args at the launcher */
        if (LauncherUtils.getArgBearsMode(arguments)) {
            this.getConfig().setLauncherMode(LauncherMode.BEARS);
        } else if (LauncherUtils.getArgCheckstyleMode(arguments)) {
            this.getConfig().setLauncherMode(LauncherMode.CHECKSTYLE);
        } else {
            this.getConfig().setLauncherMode(LauncherMode.REPAIR);
        }


        this.getConfig().setBuildId(arguments.getInt("build"));
        if (this.getConfig().getLauncherMode() == LauncherMode.BEARS) {
            this.getConfig().setNextBuildId(arguments.getInt("nextBuild"));
        }

        this.getConfig().setNoTravisRepair(arguments.getBoolean("noTravisRepair"));
        this.getConfig().setPatchRankingMode(arguments.getString("patchRankingMode"));

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