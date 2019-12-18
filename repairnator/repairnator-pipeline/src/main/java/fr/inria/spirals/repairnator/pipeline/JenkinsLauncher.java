package fr.inria.spirals.repairnator.pipeline;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.states.LauncherMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.HashSet;
import java.util.Arrays;
import java.io.PrintStream;
import java.util.ArrayList;

/* Entry point as Jenkins plugin - skip JSAP */
public class JenkinsLauncher extends Launcher {
	private static Logger LOGGER = LoggerFactory.getLogger(JenkinsLauncher.class);

	private static RepairnatorConfig getConfig() {
        return RepairnatorConfig.getInstance();
    }

	public JenkinsLauncher() {}

    public void jenkinsMain(int buildId) {
        /* Setting config */
        this.getConfig().setClean(true);
        this.getConfig().setRunId("1234");
        this.getConfig().setGithubToken("");
        this.getConfig().setLauncherMode(LauncherMode.REPAIR);
        this.getConfig().setBuildId(buildId);
        this.getConfig().setZ3solverPath(new File("./z3_for_linux").getPath());

        this.getConfig().setRepairTools(new HashSet<>(Arrays.asList("NPEFix".split(" "))));
        if (this.getConfig().getLauncherMode() == LauncherMode.REPAIR) {
            LOGGER.info("The following repair tools will be used: " + StringUtils.join(this.getConfig().getRepairTools(), ", "));
        }

        this.initSerializerEngines();
        this.initNotifiers();
        this.mainProcess();
    }

}