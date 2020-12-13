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
import fr.inria.spirals.repairnator.process.inspectors.InspectorFactory;
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
public class GitRepositoryLauncher extends LegacyLauncher {
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

        jsap = this.defineArgs();
        JSAPResult arguments = jsap.parse(args);
        LauncherUtils.checkArguments(jsap, arguments, LauncherType.PIPELINE);
        this.initConfig(arguments);

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

    @Override
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

        return jsap;
    }

    @Override
    protected void initConfig(JSAPResult arguments) {
        LauncherUtils.initCommonConfig(this.getConfig(), arguments);
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

        inspector = InspectorFactory.getGithubInspector(
        		getConfig().getGitRepositoryUrl(),
        		getConfig().getGitRepositoryBranch(),
        		getConfig().getGitRepositoryIdCommit(),
        		getConfig().isGitRepositoryFirstCommit(),
        		getConfig().getWorkspacePath(),
        		this.notifiers
        );
        
        System.out.println("Finished " + this.inspector.isPipelineEnding());
        
        inspector.getSerializers().add(new InspectorSerializer4GitRepository(this.engines, inspector));
    	inspector.getSerializers().add(new PropertiesSerializer4GitRepository(this.engines, inspector));
        inspector.getSerializers().add(new InspectorTimeSerializer4GitRepository(this.engines, inspector));
        inspector.getSerializers().add(new PipelineErrorSerializer4GitRepository(this.engines, inspector));
        inspector.getSerializers().add(new PatchesSerializer4GitRepository(this.engines, inspector));
        inspector.getSerializers().add(new ToolDiagnosticSerializer4GitRepository(this.engines, inspector));
        inspector.getSerializers().add(new PullRequestSerializer4GitRepository(this.engines, inspector));

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
    	launcher.initProcess(launcher);
    }


}
