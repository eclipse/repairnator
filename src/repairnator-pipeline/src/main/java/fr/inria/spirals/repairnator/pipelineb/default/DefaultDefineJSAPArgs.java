package fr.inria.spirals.repairnator.pipeline;

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
import fr.inria.spirals.repairnator.pipeline.RepairToolsManager;
import fr.inria.spirals.repairnator.process.step.repair.NPERepair;

import org.apache.commons.lang3.StringUtils;

/* Args definition behavior for the default use case of Repairnator */
public class DefaultDefineJSAPArgs implements IDefineJSAPArgs{

	@Override
	public JSAP defineArgs() throws JSAPException{
		// Verbose output
        JSAP jsap = new JSAP();

        // -h or --help
        jsap.registerParameter(LauncherUtils.defineArgHelp());
        // -d or --debug
        jsap.registerParameter(LauncherUtils.defineArgDebug());
        // --runId
        jsap.registerParameter(LauncherUtils.defineArgRunId());
        // --bears
        jsap.registerParameter(LauncherUtils.defineArgBearsMode());
        // --checkstyle
        jsap.registerParameter(LauncherUtils.defineArgCheckstyleMode());
        // -o or --output
        jsap.registerParameter(LauncherUtils.defineArgOutput(LauncherType.PIPELINE, "Specify path to output serialized files"));
        // --dbhost
        jsap.registerParameter(LauncherUtils.defineArgMongoDBHost());
        // --dbname
        jsap.registerParameter(LauncherUtils.defineArgMongoDBName());
        // --smtpServer
        jsap.registerParameter(LauncherUtils.defineArgSmtpServer());
        // --smtpPort
        jsap.registerParameter(LauncherUtils.defineArgSmtpPort());
        // --smtpTLS
        jsap.registerParameter(LauncherUtils.defineArgSmtpTLS());
        // --smtpUsername
        jsap.registerParameter(LauncherUtils.defineArgSmtpUsername());
        // --smtpPassword
        jsap.registerParameter(LauncherUtils.defineArgSmtpPassword());
        // --notifyto
        jsap.registerParameter(LauncherUtils.defineArgNotifyto());
        // --pushurl
        jsap.registerParameter(LauncherUtils.defineArgPushUrl());
        // --ghOauth
        jsap.registerParameter(LauncherUtils.defineArgGithubOAuth());
        // --githubUserName
        jsap.registerParameter(LauncherUtils.defineArgGithubUserName());
        // --githubUserEmail
        jsap.registerParameter(LauncherUtils.defineArgGithubUserEmail());
        // --createPR
        jsap.registerParameter(LauncherUtils.defineArgCreatePR());

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
        opt2.setDefault(InputBuildId.NO_PATCH+"");
        opt2.setHelp("Specify the next build id to use (only in BEARS mode).");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("z3");
        opt2.setLongFlag("z3");
        opt2.setDefault("./z3_for_linux");
        // opt2.setStringParser(FileStringParser.getParser().setMustBeFile(true).setMustExist(true));
        opt2.setHelp("Specify path to Z3");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("workspace");
        opt2.setLongFlag("workspace");
        opt2.setShortFlag('w');
        opt2.setDefault("./workspace");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("Specify a path to be used by the pipeline at processing things like to clone the project of the build id being processed");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("projectsToIgnore");
        opt2.setLongFlag("projectsToIgnore");
        opt2.setStringParser(FileStringParser.getParser().setMustBeFile(true));
        opt2.setHelp("Specify the file containing a list of projects that the pipeline should deactivate serialization when processing builds from.");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("listenermode");
        opt2.setLongFlag("listenermode");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setDefault(LISTENER_MODE.NOOP.name());
        opt2.setHelp("Possible string values KUBERNETES,NOOP . KUBERNETES is for running ActiveMQListener and "+LISTENER_MODE.NOOP.name()+" is for NoopRunner.");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("activemqurl");
        opt2.setLongFlag("activemqurl");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setDefault("tcp://localhost:61616");
        opt2.setHelp("format: 'tcp://IP_OR_DNSNAME:61616', default as 'tcp://localhost:61616'");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("activemqlistenqueuename");
        opt2.setLongFlag("activemqlistenqueuename");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setDefault("pipeline");
        opt2.setHelp("Just a name, default as 'pipeline'");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("activemqusername");
        opt2.setLongFlag("activemqusername");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setDefault("");
        opt2.setHelp("The username to access ActiveMQ, which is blank by default");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("activemqpassword");
        opt2.setLongFlag("activemqpassword");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setDefault("");
        opt2.setHelp("The password to access ActiveMQ, which is blank by default");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("giturl");
        opt2.setLongFlag("giturl");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("Example: https://github.com/surli/failingProject.git");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("gitbranch");
        opt2.setLongFlag("gitbranch");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setDefault("master");
        opt2.setHelp("Git branch name. Default: master");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("gitcommithash");
        opt2.setLongFlag("gitcommit");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("the hash of your git commit");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("MavenHome");
        opt2.setLongFlag("MavenHome");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("Maven home folder, use in case if enviroment variable M2_HOME is null");
        opt2.setDefault("/usr/share/maven");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("repairTools");
        opt2.setLongFlag("repairTools");
        String availablerepairTools = StringUtils.join(RepairToolsManager.getRepairToolsName(), ",");

        opt2.setStringParser(EnumeratedStringParser.getParser(availablerepairTools.replace(',',';'), true));
        opt2.setList(true);
        opt2.setListSeparator(',');
        opt2.setHelp("Specify one or several repair tools to use among: "+availablerepairTools);
        opt2.setDefault(NPERepair.TOOL_NAME); // default one is not all available ones
        jsap.registerParameter(opt2);

        // This option will have a list and must have n*3 elements, otherwise the last will be ignored.
        opt2 = new FlaggedOption("experimentalPluginRepoList");
        opt2.setLongFlag("experimentalPluginRepoList");
        opt2.setList(true);
        opt2.setListSeparator(',');
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("The ids, names and urls of all experimental pluginrepos used. Must be a list of length n*3 in the order id, name, url, repeat.");
        jsap.registerParameter(opt2);

        Switch sw = new Switch("tmpDirAsWorkSpace");
        sw.setLongFlag("tmpDirAsWorkSpace");
        sw.setDefault("false");
        sw.setHelp("Create tmp directory as workspace");
        jsap.registerParameter(sw);

        sw = new Switch("noTravisRepair");
        sw.setLongFlag("noTravisRepair");
        sw.setDefault("false");
        sw.setHelp("repair with git url , branch and commit instead of travis build ids");
        jsap.registerParameter(sw);

        sw = new Switch("rankPatches");
        sw.setLongFlag("rankPatches");
        sw.setDefault("false");
        sw.setHelp("If true, patches produced by repair tools will be ranked by an overfitting model");
        jsap.registerParameter(sw);

        return jsap;
	}
}