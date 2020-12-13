package fr.inria.spirals.repairnator.pipeline.travis;

import static fr.inria.spirals.repairnator.config.RepairnatorConfig.LISTENER_MODE;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.stringparsers.EnumeratedStringParser;
import com.martiansoftware.jsap.stringparsers.FileStringParser;
import com.martiansoftware.jsap.Switch;

import fr.inria.spirals.repairnator.InputBuildId;
import fr.inria.spirals.repairnator.LauncherUtils;
import fr.inria.spirals.repairnator.LauncherType;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.pipeline.RepairToolsManager;
import fr.inria.spirals.repairnator.process.step.repair.NPERepair;
import fr.inria.spirals.repairnator.pipeline.IDefineJSAPArgs;

import org.apache.commons.lang3.StringUtils;

/* Args definition behavior for the default use case of Repairnator */
public class TravisDefineJSAPArgs implements IDefineJSAPArgs {

    @Override
    public JSAP defineArgs() throws JSAPException {
        // Verbose output
        JSAP jsap = new JSAP();
        LauncherUtils.registerCommonArgs(jsap);

        // --bears
        jsap.registerParameter(LauncherUtils.defineArgBearsMode());
        // --checkstyle
        jsap.registerParameter(LauncherUtils.defineArgCheckstyleMode());


        FlaggedOption opt2 = new FlaggedOption("build");
        opt2.setShortFlag('b');
        opt2.setLongFlag("build");
        opt2.setStringParser(JSAP.INTEGER_PARSER);
        opt2.setDefault("0");
        opt2.setHelp("Specify the build id to use.");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("nextBuild");
        opt2.setShortFlag('n');
        opt2.setLongFlag("nextBuild");
        opt2.setStringParser(JSAP.INTEGER_PARSER);
        opt2.setDefault(InputBuildId.NO_PATCH + "");
        opt2.setHelp("Specify the next build id to use (only in BEARS mode).");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("repairTools");
        opt2.setLongFlag("repairTools");
        String availablerepairTools = StringUtils.join(RepairToolsManager.getRepairToolsName(), ",");

        opt2.setStringParser(EnumeratedStringParser.getParser(availablerepairTools.replace(',', ';'), true));
        opt2.setList(true);
        opt2.setListSeparator(',');
        opt2.setHelp("Specify one or several repair tools to use among: " + availablerepairTools);
        opt2.setDefault(NPERepair.TOOL_NAME); // default one is not all available ones
        jsap.registerParameter(opt2);


        opt2 = new FlaggedOption("patchRankingMode");
        opt2.setLongFlag("patchRankingMode");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setDefault(RepairnatorConfig.PATCH_RANKING_MODE.NONE.name());
        opt2.setHelp("Possible string values NONE, OVERFITTING.");
        jsap.registerParameter(opt2);

        Switch sw = new Switch("noTravisRepair");
        sw.setLongFlag("noTravisRepair");
        sw.setDefault("false");
        sw.setHelp("repair with git url , branch and commit instead of travis build ids");
        jsap.registerParameter(sw);


        return jsap;
    }
}