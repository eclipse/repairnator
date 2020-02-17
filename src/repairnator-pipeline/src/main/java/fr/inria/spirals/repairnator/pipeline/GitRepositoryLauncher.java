package fr.inria.spirals.repairnator.pipeline;

import static fr.inria.spirals.repairnator.config.RepairnatorConfig.LISTENER_MODE;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.stringparsers.EnumeratedStringParser;
import com.martiansoftware.jsap.stringparsers.FileStringParser;

import ch.qos.logback.classic.Level;

import com.martiansoftware.jsap.Switch;

import fr.inria.spirals.repairnator.GitRepositoryLauncherUtils;
import fr.inria.spirals.repairnator.LauncherType;
import fr.inria.spirals.repairnator.LauncherUtils;
import fr.inria.spirals.repairnator.notifier.BugAndFixerBuildsNotifier;
import fr.inria.spirals.repairnator.notifier.GitRepositoryErrorNotifier;
import fr.inria.spirals.repairnator.notifier.GitRepositoryPatchNotifierImpl;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.process.inspectors.GitRepositoryProjectInspector;
import fr.inria.spirals.repairnator.process.step.repair.NPERepair;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.serializer.HardwareInfoSerializer;
import fr.inria.spirals.repairnator.serializer.InspectorSerializer4GitRepository;
import fr.inria.spirals.repairnator.serializer.InspectorTimeSerializer4GitRepository;
import fr.inria.spirals.repairnator.serializer.PatchesSerializer4GitRepository;
import fr.inria.spirals.repairnator.serializer.PipelineErrorSerializer4GitRepository;
import fr.inria.spirals.repairnator.serializer.PropertiesSerializer4GitRepository;
import fr.inria.spirals.repairnator.serializer.ToolDiagnosticSerializer4GitRepository;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.serializer.PullRequestSerializer4GitRepository;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.spirals.repairnator.utils.Utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

/**
 * This class is the main entry point for the Repairnator pipeline
 * when Repairnator is executed with the GIT_REPOSITORY launcher mode.
 */
public class GitRepositoryLauncher extends Launcher {
    private static Logger LOGGER = LoggerFactory.getLogger(GitRepositoryLauncher.class);
   
    private static File tempDir;
    private JSAP jsap;

    /* just give an empty instance of the launcher for customized execution */
    public GitRepositoryLauncher() {
        super();
    }

    public GitRepositoryLauncher(String[] args) throws JSAPException {
        InputStream propertyStream = getClass().getResourceAsStream("/version.properties");
        Properties properties = new Properties();
        if (propertyStream != null) {
            try {
                properties.load(propertyStream);
            } catch (IOException e) {
                LOGGER.error("Error while loading property file.", e);
            }
            LOGGER.info("PIPELINE VERSION: "+properties.getProperty("PIPELINE_VERSION"));
        } else {
            LOGGER.info("No information about PIPELINE VERSION has been found.");
        }

        jsap = defineArgs();
        JSAPResult arguments = jsap.parse(args);
        LauncherUtils.checkArguments(jsap, arguments, LauncherType.PIPELINE);
        this.initConfig(arguments);

        checkToolsLoaded(jsap);
        this.checkNopolSolverPath(jsap);
        LOGGER.info("The pipeline will try to repair the following repository id: " + getConfig().getGitRepositoryId());
        
        if (getConfig().isDebug()) {
            Utils.setLoggersLevel(Level.DEBUG);
        } else {
            Utils.setLoggersLevel(Level.INFO);
        }

        this.initSerializerEngines();
        this.initNotifiers();
    }

    public static JSAP defineArgs() throws JSAPException {
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

    @Override
    protected void initConfig(JSAPResult arguments) {
    	if (LauncherUtils.getArgDebug(arguments)) {
            getConfig().setDebug(true);
        }
        getConfig().setClean(true);
        getConfig().setRunId(LauncherUtils.getArgRunId(arguments));
        getConfig().setGithubToken(LauncherUtils.getArgGithubOAuth(arguments));
        
        if (GitRepositoryLauncherUtils.getArgGitRepositoryMode(arguments)) {
            getConfig().setLauncherMode(LauncherMode.GIT_REPOSITORY);
            if (GitRepositoryLauncherUtils.getArgGitRepositoryFirstCommit(arguments)) {
            	getConfig().setGitRepositoryFirstCommit(true);
            }
        } else {
        	System.err.println("Error: Parameter 'gitrepo' is required in GIT_REPOSITORY launcher mode.");
    		LauncherUtils.printUsage(jsap, LauncherType.PIPELINE);
        }
        
        if (LauncherUtils.getArgOutput(arguments) != null) {
            getConfig().setOutputPath(LauncherUtils.getArgOutput(arguments).getPath());
        }
        getConfig().setMongodbHost(LauncherUtils.getArgMongoDBHost(arguments));
        getConfig().setMongodbName(LauncherUtils.getArgMongoDBName(arguments));
        getConfig().setSmtpServer(LauncherUtils.getArgSmtpServer(arguments));
        getConfig().setSmtpPort(LauncherUtils.getArgSmtpPort(arguments));
        getConfig().setSmtpTLS(LauncherUtils.getArgSmtpTLS(arguments));
        getConfig().setSmtpUsername(LauncherUtils.getArgSmtpUsername(arguments));
        getConfig().setSmtpPassword(LauncherUtils.getArgSmtpPassword(arguments));
        getConfig().setNotifyTo(LauncherUtils.getArgNotifyto(arguments));

        if (LauncherUtils.getArgPushUrl(arguments) != null) {
            getConfig().setPush(true);
            getConfig().setPushRemoteRepo(LauncherUtils.getArgPushUrl(arguments));
        }
        getConfig().setCreatePR(LauncherUtils.getArgCreatePR(arguments));

        // we fork if we need to create a PR or if we need to notify
        if (getConfig().isCreatePR() || (getConfig().getSmtpServer() != null && !getConfig().getSmtpServer().isEmpty() && getConfig().getNotifyTo() != null && getConfig().getNotifyTo().length > 0)) {
            getConfig().setFork(true);
        }

        if (arguments.getString("gitRepositoryUrl") == null) {
    		System.err.println("Error: Parameter 'gitrepourl' is required in GIT_REPOSITORY launcher mode.");
    		LauncherUtils.printUsage(jsap, LauncherType.PIPELINE);
    	}

    	if (getConfig().isGitRepositoryFirstCommit() && arguments.getString("gitRepositoryIdCommit") != null) {
    		System.err.println("Error: Parameters 'gitrepofirstcommit' and 'gitrepoidcommit' cannot be used at the same time.");
    		LauncherUtils.printUsage(jsap, LauncherType.PIPELINE);
    	}

    	getConfig().setGitRepositoryUrl(arguments.getString("gitRepositoryUrl"));
    	getConfig().setGitRepositoryBranch(arguments.getString("gitRepositoryBranch"));
    	getConfig().setGitRepositoryIdCommit(arguments.getString("gitRepositoryIdCommit"));
        

        getConfig().setZ3solverPath(new File(arguments.getString("z3")).getPath());
        getConfig().setWorkspacePath(arguments.getString("workspace"));
        if (arguments.getBoolean("tmpDirAsWorkSpace")) {
            tempDir = com.google.common.io.Files.createTempDir();
            getConfig().setWorkspacePath(tempDir.getAbsolutePath());
            getConfig().setOutputPath(tempDir.getAbsolutePath());
            getConfig().setZ3solverPath(new File(tempDir.getAbsolutePath() + File.separator + "z3_for_linux").getPath());
        }

        getConfig().setGithubUserEmail(LauncherUtils.getArgGithubUserEmail(arguments));
        getConfig().setGithubUserName(LauncherUtils.getArgGithubUserName(arguments));
        getConfig().setListenerMode(arguments.getString("listenermode"));
        getConfig().setActiveMQUrl(arguments.getString("activemqurl"));
        getConfig().setActiveMQListenQueueName(arguments.getString("activemqlistenqueuename"));

        getConfig().setGitUrl(arguments.getString("giturl"));
        getConfig().setGitBranch(arguments.getString("gitbranch"));
        getConfig().setGitCommitHash(arguments.getString("gitcommithash"));
        getConfig().setMavenHome(arguments.getString("MavenHome"));

        if (arguments.getFile("projectsToIgnore") != null) {
            getConfig().setProjectsToIgnoreFilePath(arguments.getFile("projectsToIgnore").getPath());
        }

        getConfig().setRepairTools(new HashSet<>(Arrays.asList(arguments.getStringArray("repairTools"))));

        // Make sure that it is a multiple of three in the list
        if((arguments.getStringArray("experimentalPluginRepoList").length) % 3 == 0) {
            getConfig().setExperimentalPluginRepoList(arguments.getStringArray("experimentalPluginRepoList"));
        } else if (arguments.getStringArray("experimentalPluginRepoList").length != 0) {
            LOGGER.warn("The experimental plugin repo list is not correctly formed."
                    + " Please make sure you have provided id, name and url for all repos. "
                    + "Repairnator will continue without these repos.");
            getConfig().setExperimentalPluginRepoList(null);
        } else {
            getConfig().setExperimentalPluginRepoList(null);
        }
    }

    @Override
    protected void initSerializerEngines() {
        this.engines = new ArrayList<>();

        List<SerializerEngine> fileSerializerEngines = GitRepositoryLauncherUtils.initFileSerializerEngines(LOGGER);
        this.engines.addAll(fileSerializerEngines);

        SerializerEngine mongoDBSerializerEngine = LauncherUtils.initMongoDBSerializerEngine(LOGGER);
        if (mongoDBSerializerEngine != null) {
            this.engines.add(mongoDBSerializerEngine);
        }
    }

    @Override
    protected void initNotifiers() {
        List<NotifierEngine> notifierEngines = LauncherUtils.initNotifierEngines(LOGGER);
        GitRepositoryErrorNotifier.getInstance(notifierEngines);

        this.notifiers = new ArrayList<>();
        this.notifiers.add(new BugAndFixerBuildsNotifier(notifierEngines));

        this.patchNotifier = new GitRepositoryPatchNotifierImpl(notifierEngines);
    }

    @Override
    public boolean mainProcess() {
        LOGGER.info("Start by getting the repository (repositoryId: " + getConfig().getGitRepositoryId()+") with the following config: " + getConfig());

        HardwareInfoSerializer hardwareInfoSerializer = new HardwareInfoSerializer(this.engines, getConfig().getRunId(), getConfig().getBuildId()+"");
        hardwareInfoSerializer.serialize();

        List<AbstractDataSerializer> serializers = new ArrayList<>();

        inspector = new GitRepositoryProjectInspector(
        		getConfig().getGitRepositoryUrl(),
        		getConfig().getGitRepositoryBranch(),
        		getConfig().getGitRepositoryIdCommit(),
        		getConfig().isGitRepositoryFirstCommit(),
        		getConfig().getWorkspacePath(),
        		serializers,
        		this.notifiers
        );
        
        System.out.println("Finished " + this.inspector.isPipelineEnding());
        
        serializers.add(new InspectorSerializer4GitRepository(this.engines, inspector));
    	serializers.add(new PropertiesSerializer4GitRepository(this.engines, inspector));
        serializers.add(new InspectorTimeSerializer4GitRepository(this.engines, inspector));
        serializers.add(new PipelineErrorSerializer4GitRepository(this.engines, inspector));
        serializers.add(new PatchesSerializer4GitRepository(this.engines, inspector));
        serializers.add(new ToolDiagnosticSerializer4GitRepository(this.engines, inspector));
        serializers.add(new PullRequestSerializer4GitRepository(this.engines, inspector));

        inspector.setPatchNotifier(this.patchNotifier);
        inspector.run();

        if(tempDir != null) {
            tempDir.delete();
        }

        LOGGER.info("Inspector is finished. The process will exit now.");
        return true;
    }

    public static void main(String[] args) throws JSAPException {
    	GitRepositoryLauncher launcher = new GitRepositoryLauncher(args);
    	initProcess(launcher);
    }
}
