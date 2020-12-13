package fr.inria.spirals.repairnator.pipeline.github;

import static fr.inria.spirals.repairnator.config.RepairnatorConfig.LISTENER_MODE;
import static fr.inria.spirals.repairnator.config.RepairnatorConfig.SORALD_REPAIR_MODE;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.stringparsers.EnumeratedStringParser;
import com.martiansoftware.jsap.stringparsers.FileStringParser;
import com.martiansoftware.jsap.Switch;

import fr.inria.spirals.repairnator.InputBuildId;
import fr.inria.spirals.repairnator.LauncherUtils;
import fr.inria.spirals.repairnator.LauncherType;
import fr.inria.spirals.repairnator.process.step.repair.NPERepair;
import fr.inria.spirals.repairnator.GitRepositoryLauncherUtils;
import fr.inria.spirals.repairnator.pipeline.IDefineJSAPArgs;
import fr.inria.spirals.repairnator.pipeline.RepairToolsManager;

import org.apache.commons.lang3.StringUtils;

/* Args definition behavior for repairing with Github instead of Travis */
public class GithubDefineJSAPArgs implements IDefineJSAPArgs{

    public JSAP defineArgs() throws JSAPException {
        // Verbose output
        JSAP jsap = new JSAP();
        LauncherUtils.registerCommonArgs(jsap);

        // --gitRepo
        jsap.registerParameter(GitRepositoryLauncherUtils.defineArgGitRepositoryMode());
        // --gitRepoUrl
        jsap.registerParameter(GitRepositoryLauncherUtils.defineArgGitRepositoryUrl());
        // --gitRepoBranch
        jsap.registerParameter(GitRepositoryLauncherUtils.defineArgGitRepositoryBranch());
        // --gitRepoIdCommit
        jsap.registerParameter(GitRepositoryLauncherUtils.defineArgGitRepositoryIdCommit());
        // --gitRepoFirstCommit
        jsap.registerParameter(GitRepositoryLauncherUtils.defineArgGitRepositoryFirstCommit());


        FlaggedOption opt;

        opt = new FlaggedOption("repairTools");
        opt.setLongFlag("repairTools");

        String availablerepairTools = StringUtils.join(RepairToolsManager.getRepairToolsName(), ",");

        opt.setStringParser(EnumeratedStringParser.getParser(availablerepairTools.replace(',',';'), true));
        opt.setList(true);
        opt.setListSeparator(',');
        opt.setHelp("Specify one or several repair tools to use among: "+availablerepairTools);
        opt.setDefault(NPERepair.TOOL_NAME); // default one is not all available ones
        jsap.registerParameter(opt);

        // Sorald config
        opt = new FlaggedOption("sonarRules");
        opt.setLongFlag("sonarRules");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setDefault("2116");
        opt.setHelp("Required if SonarQube is specified in the repairtools as argument. Format: 1948,1854,RuleNumber.. . Supported rules: https://github.com/kth-tcs/sonarqube-repair/blob/master/docs/HANDLED_RULES.md");
        jsap.registerParameter(opt);

        opt = new FlaggedOption("soraldRepairMode");
        opt.setLongFlag("soraldRepairMode");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setDefault(SORALD_REPAIR_MODE.DEFAULT.name());
        opt.setHelp("DEFAULT - default mode , load everything in at once into Sorald. SEGMENT - repair segments of the projects instead, segmentsize can be specified.");
        jsap.registerParameter(opt);

        opt = new FlaggedOption("segmentSize");
        opt.setLongFlag("segmentSize");
        opt.setStringParser(JSAP.INTEGER_PARSER);
        opt.setDefault("200");
        opt.setHelp("Segment size for the segment repair.");
        jsap.registerParameter(opt);

        opt = new FlaggedOption("soraldMaxFixesPerRule");
        opt.setLongFlag("soraldMaxFixesPerRule");
        opt.setStringParser(JSAP.INTEGER_PARSER);
        opt.setDefault("2000");
        opt.setHelp("Number of fixes per SonarQube rule.");
        jsap.registerParameter(opt);

        return jsap;
    }
}