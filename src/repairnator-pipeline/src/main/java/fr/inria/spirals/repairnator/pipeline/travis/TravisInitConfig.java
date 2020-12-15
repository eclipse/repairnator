package fr.inria.spirals.repairnator.pipeline.travis;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.pipeline.IInitConfig;
import fr.inria.spirals.repairnator.pipeline.LauncherHelpers;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.spirals.repairnator.utils.LauncherUtils;
import fr.inria.spirals.repairnator.utils.TravisLauncherUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;

/* Config init behavior for the default use case of Repairnator */
public class TravisInitConfig implements IInitConfig {
    private static Logger LOGGER = LoggerFactory.getLogger(TravisInitConfig.class);

    protected static RepairnatorConfig getConfig() {
        return RepairnatorConfig.getInstance();
    }

    @Override
    public void initConfigWithJSAP(JSAP jsap, String[] inputArgs) {
        LauncherHelpers.getInstance().loadProperty();
        JSAPResult arguments = jsap.parse(inputArgs);

        LauncherUtils.initCommonConfig(getConfig(), arguments);
        TravisLauncherUtils.initTravisConfig(getConfig(), arguments);

        getConfig().setRepairTools(new HashSet<>(Arrays.asList(arguments.getStringArray("repairTools"))));
        if (getConfig().getLauncherMode() == LauncherMode.REPAIR) {
            LOGGER.info("The following repair tools will be used: " + StringUtils.join(getConfig().getRepairTools(), ", "));
        }

    }
}