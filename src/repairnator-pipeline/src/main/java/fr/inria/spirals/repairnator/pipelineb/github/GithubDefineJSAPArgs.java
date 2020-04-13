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
import fr.inria.spirals.repairnator.process.step.repair.NPERepair;
import fr.inria.spirals.repairnator.GitRepositoryLauncherUtils;

import org.apache.commons.lang3.StringUtils;

/* Args definition behavior for repairing with Github instead of Travis */
public class GithubDefineJSAPArgs implements IDefineJSAPArgs{

    public JSAP defineArgs() throws JSAPException {
        // Verbose output
        JSAP jsap = new JSAP();

        // -h or --help
        jsap.registerParameter(LauncherUtils.defineArgHelp());
        // -d or --debug
        jsap.registerParameter(LauncherUtils.defineArgDebug());
        // --runId
        jsap.registerParameter(LauncherUtils.defineArgRunId());
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

        FlaggedOption opt = new FlaggedOption("z3");
        opt.setLongFlag("z3");
        opt.setDefault("./z3_for_linux");
        opt.setHelp("Specify path to Z3");
        jsap.registerParameter(opt);

        opt = new FlaggedOption("workspace");
        opt.setLongFlag("workspace");
        opt.setShortFlag('w');
        opt.setDefault("./workspace");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Specify a path to be used by the pipeline at processing things like to clone the project of the repository id being processed");
        jsap.registerParameter(opt);

        opt = new FlaggedOption("projectsToIgnore");
        opt.setLongFlag("projectsToIgnore");
        opt.setStringParser(FileStringParser.getParser().setMustBeFile(true));
        opt.setHelp("Specify the file containing a list of projects that the pipeline should deactivate serialization when processing builds from.");
        jsap.registerParameter(opt);

        opt = new FlaggedOption("listenermode");
        opt.setLongFlag("listenermode");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setDefault(LISTENER_MODE.NOOP.name());
        opt.setHelp("Possible string values KUBERNETES,NOOP . KUBERNETES is for running ActiveMQListener and "+LISTENER_MODE.NOOP.name()+" is for NoopRunner.");
        jsap.registerParameter(opt);

        opt = new FlaggedOption("activemqurl");
        opt.setLongFlag("activemqurl");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setDefault("tcp://localhost:61616");
        opt.setHelp("format: 'tcp://IP_OR_DNSNAME:61616', default as 'tcp://localhost:61616'");
        jsap.registerParameter(opt);

        opt = new FlaggedOption("activemqlistenqueuename");
        opt.setLongFlag("activemqlistenqueuename");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setDefault("pipeline");
        opt.setHelp("Just a name, default as 'pipeline'");
        jsap.registerParameter(opt);

        opt = new FlaggedOption("giturl");
        opt.setLongFlag("giturl");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Example: https://github.com/surli/failingProject.git");
        jsap.registerParameter(opt);

        opt = new FlaggedOption("gitbranch");
        opt.setLongFlag("gitbranch");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setDefault("master");
        opt.setHelp("Git branch name. Default: master");
        jsap.registerParameter(opt);

        opt = new FlaggedOption("gitcommithash");
        opt.setLongFlag("gitcommit");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("the hash of your git commit");
        jsap.registerParameter(opt);

        opt = new FlaggedOption("MavenHome");
        opt.setLongFlag("MavenHome");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Maven home folder, use in case if enviroment variable M2_HOME is null");
        opt.setDefault("/usr/share/maven");
        jsap.registerParameter(opt);

        opt = new FlaggedOption("repairTools");
        opt.setLongFlag("repairTools");

        String availablerepairTools = StringUtils.join(RepairToolsManager.getRepairToolsName(), ",");

        opt.setStringParser(EnumeratedStringParser.getParser(availablerepairTools.replace(',',';'), true));
        opt.setList(true);
        opt.setListSeparator(',');
        opt.setHelp("Specify one or several repair tools to use among: "+availablerepairTools);
        opt.setDefault(NPERepair.TOOL_NAME); // default one is not all available ones
        jsap.registerParameter(opt);

        // This option will have a list and must have n*3 elements, otherwise the last will be ignored.
        opt = new FlaggedOption("experimentalPluginRepoList");
        opt.setLongFlag("experimentalPluginRepoList");
        opt.setList(true);
        opt.setListSeparator(',');
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("The ids, names and urls of all experimental pluginrepos used. Must be a list of length n*3 in the order id, name, url, repeat.");
        jsap.registerParameter(opt);

        Switch sw = new Switch("tmpDirAsWorkSpace");
        sw.setLongFlag("tmpDirAsWorkSpace");
        sw.setDefault("false");
        sw.setHelp("Create tmp directory as workspace");
        jsap.registerParameter(sw);

        return jsap;
    }
}