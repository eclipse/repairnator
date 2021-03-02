package fr.inria.spirals.repairnator.pipeline.github;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.stringparsers.EnumeratedStringParser;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.pipeline.IDefineJSAPArgs;
import fr.inria.spirals.repairnator.pipeline.RepairToolsManager;
import fr.inria.spirals.repairnator.process.step.repair.NPERepair;
import fr.inria.spirals.repairnator.GitRepositoryLauncherUtils;
import fr.inria.spirals.repairnator.LauncherUtils;
import fr.inria.spirals.repairnator.process.step.repair.sequencer.SequencerRepair;
import fr.inria.spirals.repairnator.states.LauncherMode;
import org.apache.commons.lang3.StringUtils;

/* Args definition behavior for repairing with Github instead of Travis */
public class GithubDefineJSAPArgs implements IDefineJSAPArgs {

    public JSAP defineArgs() throws JSAPException {
        JSAP jsap = new JSAP();

        LauncherUtils.registerCommonArgs(jsap);
        GitRepositoryLauncherUtils.registerGitArgs(jsap);

        String defaultTool =
                RepairnatorConfig.getInstance().getLauncherMode().equals(LauncherMode.SEQUENCER_REPAIR) ?
                SequencerRepair.TOOL_NAME : NPERepair.TOOL_NAME;

        FlaggedOption opt = new FlaggedOption("repairTools");
        opt.setLongFlag("repairTools");
        String availablerepairTools = StringUtils.join(RepairToolsManager.getRepairToolsName(), ",");
        opt.setStringParser(EnumeratedStringParser.getParser(availablerepairTools.replace(',', ';'), true));
        opt.setList(true);
        opt.setListSeparator(',');
        opt.setHelp("Specify one or several repair tools to use among: " + availablerepairTools);
        opt.setDefault(defaultTool); // default one is not all available ones
        jsap.registerParameter(opt);

        return jsap;
    }
}