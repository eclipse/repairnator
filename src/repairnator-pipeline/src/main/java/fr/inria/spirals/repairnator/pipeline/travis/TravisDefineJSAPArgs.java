package fr.inria.spirals.repairnator.pipeline.travis;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.stringparsers.EnumeratedStringParser;
import fr.inria.spirals.repairnator.pipeline.IDefineJSAPArgs;
import fr.inria.spirals.repairnator.pipeline.RepairToolsManager;
import fr.inria.spirals.repairnator.process.step.repair.NPERepair;
import fr.inria.spirals.repairnator.LauncherUtils;
import fr.inria.spirals.repairnator.TravisLauncherUtils;
import org.apache.commons.lang3.StringUtils;

/* Args definition behavior for the default use case of Repairnator */
public class TravisDefineJSAPArgs implements IDefineJSAPArgs {

    @Override
    public JSAP defineArgs() throws JSAPException {
        JSAP jsap = new JSAP();

        // Register arguments common to all Launchers
        LauncherUtils.registerCommonArgs(jsap);
        // Register arguments for TravisCI
        TravisLauncherUtils.registerTravisArgs(jsap);

        FlaggedOption opt2 = new FlaggedOption("repairTools");
        opt2.setLongFlag("repairTools");
        String availablerepairTools = StringUtils.join(RepairToolsManager.getRepairToolsName(), ",");
        opt2.setStringParser(EnumeratedStringParser.getParser(availablerepairTools.replace(',', ';'), true));
        opt2.setList(true);
        opt2.setListSeparator(',');
        opt2.setHelp("Specify one or several repair tools to use among: " + availablerepairTools);
        opt2.setDefault(NPERepair.TOOL_NAME); // default one is not all available ones
        jsap.registerParameter(opt2);

        return jsap;
    }
}