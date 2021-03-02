package fr.inria.spirals.repairnator.pipeline;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.stringparsers.EnumeratedStringParser;
import fr.inria.spirals.repairnator.GithubInputBuild;
import fr.inria.spirals.repairnator.LauncherType;
import fr.inria.spirals.repairnator.notifier.BugAndFixerBuildsNotifier;
import fr.inria.spirals.repairnator.notifier.GitRepositoryErrorNotifier;
import fr.inria.spirals.repairnator.notifier.GitRepositoryPatchNotifierImpl;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.process.inspectors.InspectorFactory;
import fr.inria.spirals.repairnator.process.step.repair.NPERepair;
import fr.inria.spirals.repairnator.serializer.*;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.spirals.repairnator.GitRepositoryLauncherUtils;
import fr.inria.spirals.repairnator.LauncherUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

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
            LOGGER.info("PIPELINE VERSION: " + properties.getProperty("PIPELINE_VERSION"));
        } else {
            LOGGER.info("No information about PIPELINE VERSION has been found.");
        }

        jsap = this.defineArgs();
        JSAPResult arguments = jsap.parse(args);
        LauncherUtils.checkArguments(jsap, arguments, LauncherType.PIPELINE);
        this.initConfig(arguments);

        this.checkNopolSolverPath(jsap);
        LOGGER.info("The pipeline will try to repair the following repository id: " + getConfig().getGitRepositoryId());

        this.initSerializerEngines();
        this.initNotifiers();
    }

    @Override
    public JSAP defineArgs() throws JSAPException {
        JSAP jsap = new JSAP();

        LauncherUtils.registerCommonArgs(jsap);
        GitRepositoryLauncherUtils.registerGitArgs(jsap);

        FlaggedOption opt;

        opt = new FlaggedOption("repairTools");
        opt.setLongFlag("repairTools");
        String availablerepairTools = StringUtils.join(RepairToolsManager.getRepairToolsName(), ",");

        opt.setStringParser(EnumeratedStringParser.getParser(availablerepairTools.replace(',', ';'), true));
        opt.setList(true);
        opt.setListSeparator(',');
        opt.setHelp("Specify one or several repair tools to use among: " + availablerepairTools);
        opt.setDefault(NPERepair.TOOL_NAME); // default one is not all available ones
        jsap.registerParameter(opt);

        return jsap;
    }

    @Override
    protected void initConfig(JSAPResult arguments) {
        LauncherUtils.initCommonConfig(this.getConfig(), arguments);
        GitRepositoryLauncherUtils.initGitConfig(this.getConfig(), arguments, jsap);

        if (getConfig().getLauncherMode() == LauncherMode.SEQUENCER_REPAIR) {
            getConfig().setRepairTools(new HashSet<>(Collections.singletonList("SequencerRepair")));
        } else {
            getConfig().setRepairTools(new HashSet<>(Arrays.asList(arguments.getStringArray("repairTools"))));
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
        LOGGER.info("Start by getting the repository (repositoryId: " + getConfig().getGitRepositoryId() + ") with the following config: " + getConfig());

        HardwareInfoSerializer hardwareInfoSerializer = new HardwareInfoSerializer(this.engines, getConfig().getRunId(), getConfig().getBuildId() + "");
        hardwareInfoSerializer.serialize();

        inspector = InspectorFactory.getGithubInspector(
                new GithubInputBuild(
                    getConfig().getGitRepositoryUrl(),
                    getConfig().getGitRepositoryBranch(),
                    getConfig().getGitRepositoryIdCommit()
                ),
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

        if (tempDir != null) {
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
