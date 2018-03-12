package fr.inria.spirals.repairnator.pipeline;

import ch.qos.logback.classic.Level;
import com.martiansoftware.jsap.*;
import com.martiansoftware.jsap.stringparsers.FileStringParser;
import fr.inria.jtravis.entities.Build;
import fr.inria.jtravis.entities.BuildStatus;
import fr.inria.jtravis.helpers.BuildHelper;
import fr.inria.jtravis.helpers.GithubTokenHelper;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.LauncherType;
import fr.inria.spirals.repairnator.LauncherUtils;
import fr.inria.spirals.repairnator.notifier.ErrorNotifier;
import fr.inria.spirals.repairnator.serializer.AstorSerializer;
import fr.inria.spirals.repairnator.serializer.MetricsSerializer;
import fr.inria.spirals.repairnator.serializer.NPEFixSerializer;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.AbstractNotifier;
import fr.inria.spirals.repairnator.notifier.FixerBuildNotifier;
import fr.inria.spirals.repairnator.notifier.PatchNotifier;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector4Bears;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.serializer.HardwareInfoSerializer;
import fr.inria.spirals.repairnator.serializer.InspectorSerializer;
import fr.inria.spirals.repairnator.serializer.InspectorSerializer4Bears;
import fr.inria.spirals.repairnator.serializer.InspectorTimeSerializer;
import fr.inria.spirals.repairnator.serializer.InspectorTimeSerializer4Bears;
import fr.inria.spirals.repairnator.serializer.NopolSerializer;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by urli on 09/03/2017.
 */
public class Launcher {

    private static final String TEST_PROJECT = "surli/failingproject";
    private static Logger LOGGER = LoggerFactory.getLogger(Launcher.class);
    private JSAP jsap;
    private JSAPResult arguments;
    private RepairnatorConfig config;
    private int buildId;
    private BuildToBeInspected buildToBeInspected;
    private List<SerializerEngine> engines;
    private List<AbstractNotifier> notifiers;

    public Launcher(String[] args) throws JSAPException {
        InputStream propertyStream = getClass().getResourceAsStream("/version.properties");
        Properties properties = new Properties();
        if (propertyStream != null) {
            try {
                properties.load(propertyStream);
            } catch (IOException e) {
                LOGGER.error("Error while loading property file", e);
            }
            LOGGER.info("PIPELINE VERSION: "+properties.getProperty("PIPELINE_VERSION"));
        } else {
            LOGGER.info("No information about PIPELINE VERSION has been found.");
        }

        this.defineArgs();
        this.arguments = jsap.parse(args);
        LauncherUtils.checkArguments(this.jsap, this.arguments, LauncherType.PIPELINE);
        this.initConfig();

        if (this.config.getLauncherMode() == LauncherMode.REPAIR) {
            this.checkToolsLoaded();
            this.checkNopolSolverPath();
        }

        if (this.arguments.getBoolean("debug")) {
            Utils.setLoggersLevel(Level.DEBUG);
        } else {
            Utils.setLoggersLevel(Level.INFO);
        }

        this.buildId = this.arguments.getInt("build");

        LOGGER.info("The pipeline will try to repair the following buildid: "+this.buildId);

        this.initializeSerializerEngines();
        this.initNotifiers();
    }

    private void initNotifiers() {
        List<NotifierEngine> notifierEngines = LauncherUtils.initNotifierEngines(this.arguments, LOGGER);
        ErrorNotifier.getInstance(notifierEngines);

        this.notifiers = new ArrayList<>();
        this.notifiers.add(new PatchNotifier(notifierEngines));
        this.notifiers.add(new FixerBuildNotifier(notifierEngines));
    }

    private void initConfig() {
        this.config = RepairnatorConfig.getInstance();
        this.config.setRunId(this.arguments.getString("runId"));
        this.config.setLauncherMode(LauncherMode.valueOf(this.arguments.getString("launcherMode").toUpperCase()));
        this.config.setClean(true);
        this.config.setZ3solverPath(this.arguments.getFile("z3").getPath());
        if (this.arguments.getFile("output") != null) {
            this.config.setSerializeJson(true);
            this.config.setJsonOutputPath(this.arguments.getFile("output").getPath());
        }
        if (this.arguments.getString("pushUrl") != null) {
            this.config.setPush(true);
            this.config.setPushRemoteRepo(this.arguments.getString("pushUrl"));
        }
        this.config.setWorkspacePath(this.arguments.getString("workspace"));

        this.config.setGithubLogin(this.arguments.getString("ghLogin"));
        this.config.setGithubToken(this.arguments.getString("ghOauth"));

        GithubTokenHelper.getInstance().setGithubOauth(this.config.getGithubToken());
        GithubTokenHelper.getInstance().setGithubLogin(this.config.getGithubLogin());
    }

    private void initializeSerializerEngines() {
        this.engines = new ArrayList<>();

        SerializerEngine spreadsheetSerializerEngine = LauncherUtils.initSpreadsheetSerializerEngineWithAccessToken(this.arguments, LOGGER);
        if (spreadsheetSerializerEngine != null) {
            this.engines.add(spreadsheetSerializerEngine);
        }

        List<SerializerEngine> fileSerializerEngines = LauncherUtils.initFileSerializerEngines(this.arguments, LOGGER);
        this.engines.addAll(fileSerializerEngines);

        SerializerEngine mongoDBSerializerEngine = LauncherUtils.initMongoDBSerializerEngine(this.arguments, LOGGER);
        if (mongoDBSerializerEngine != null) {
            this.engines.add(mongoDBSerializerEngine);
        }
    }

    private void checkNopolSolverPath() {
        String solverPath = this.config.getZ3solverPath();

        if (solverPath != null) {
            File file = new File(solverPath);

            if (!file.exists()) {
                System.err.println("The Nopol solver path should be an existing file: " + file.getPath() + " does not exist.");
                LauncherUtils.printUsage(this.jsap, LauncherType.PIPELINE);
            }
        } else {
            System.err.println("The Nopol solver path should be provided.");
            LauncherUtils.printUsage(this.jsap, LauncherType.PIPELINE);
        }
    }

    private void defineArgs() throws JSAPException {
        // Verbose output
        this.jsap = new JSAP();

        this.jsap.registerParameter(LauncherUtils.defineArgHelp());
        this.jsap.registerParameter(LauncherUtils.defineArgDebug());
        this.jsap.registerParameter(LauncherUtils.defineArgRunId());
        this.jsap.registerParameter(LauncherUtils.defineArgLauncherMode("Specify if RepairNator will be launch for repairing (REPAIR) or for collecting fixer builds (BEARS)."));
        this.jsap.registerParameter(LauncherUtils.defineArgOutput(false, true, false, true, "Specify path to output serialized files"));
        this.jsap.registerParameter(LauncherUtils.defineArgMongoDBHost());
        this.jsap.registerParameter(LauncherUtils.defineArgMongoDBName());
        this.jsap.registerParameter(LauncherUtils.defineArgSpreadsheetId());
        this.jsap.registerParameter(LauncherUtils.defineArgGoogleAccessToken());
        this.jsap.registerParameter(LauncherUtils.defineArgSmtpServer());
        this.jsap.registerParameter(LauncherUtils.defineArgNotifyto());
        this.jsap.registerParameter(LauncherUtils.defineArgPushUrl());

        FlaggedOption opt2 = new FlaggedOption("build");
        opt2.setShortFlag('b');
        opt2.setLongFlag("build");
        opt2.setStringParser(JSAP.INTEGER_PARSER);
        opt2.setRequired(true);
        opt2.setHelp("Specify the build id to use.");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("z3");
        opt2.setLongFlag("z3");
        opt2.setDefault("./z3_for_linux");
        opt2.setStringParser(FileStringParser.getParser().setMustBeFile(true).setMustExist(true));
        opt2.setHelp("Specify path to Z3");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("workspace");
        opt2.setLongFlag("workspace");
        opt2.setShortFlag('w');
        opt2.setDefault("./workspace");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("Specify path to output serialized files");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("ghLogin");
        opt2.setLongFlag("ghLogin");
        opt2.setRequired(true);
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("Specify login for Github use");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("ghOauth");
        opt2.setLongFlag("ghOauth");
        opt2.setRequired(true);
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("Specify oauth for Github use");
        this.jsap.registerParameter(opt2);
    }

    private void checkToolsLoaded() {
        URLClassLoader loader;

        try {
            loader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            loader.loadClass("com.sun.jdi.AbsentInformationException");
        } catch (ClassNotFoundException e) {
            System.err.println("Tools.jar must be loaded, here the classpath given for your app: "+System.getProperty("java.class.path"));
            LauncherUtils.printUsage(this.jsap, LauncherType.PIPELINE);
        }
    }

    private void getBuildToBeInspected() {
        Build buggyBuild = BuildHelper.getBuildFromId(this.buildId, null);

        if (buggyBuild.getFinishedAt() == null) {
            LOGGER.error("Apparently the buggy build is not yet finished (maybe it has been restarted?). The process will exit now.");
            System.exit(-1);
        }
        String runId = this.arguments.getString("runId");

        if (this.config.getLauncherMode() == LauncherMode.BEARS) {
            Build patchedBuild = BuildHelper.getNextBuildOfSameBranchOfStatusAfterBuild(buggyBuild, null);
            if (patchedBuild == null) {
                LOGGER.error("Error while getting patched build: obtained null value. The process will exit now.");
                System.exit(-1);
            }

            if (buggyBuild.getBuildStatus() == BuildStatus.FAILED) {
                this.buildToBeInspected = new BuildToBeInspected(buggyBuild, patchedBuild, ScannedBuildStatus.FAILING_AND_PASSING, runId);
            } else {
                this.buildToBeInspected = new BuildToBeInspected(buggyBuild, patchedBuild, ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES, runId);
            }
        } else {
            Build nextPassing = BuildHelper.getNextBuildOfSameBranchOfStatusAfterBuild(buggyBuild, BuildStatus.PASSED);

            if (nextPassing != null) {
                this.buildToBeInspected = new BuildToBeInspected(buggyBuild, nextPassing, ScannedBuildStatus.FAILING_AND_PASSING, runId);
            } else {
                this.buildToBeInspected = new BuildToBeInspected(buggyBuild, null, ScannedBuildStatus.ONLY_FAIL, runId);
            }

            // switch off push mechanism in case of test project
            // and switch off serialization
            if (buggyBuild.getRepository().getSlug().toLowerCase().equals(TEST_PROJECT)) {
                this.config.setPush(false);
                this.engines.clear();
            }
        }
    }

    private void mainProcess() throws IOException {
        LOGGER.info("Start by getting the build (buildId: "+this.buildId+") with the following config: "+this.config);
        this.getBuildToBeInspected();

        HardwareInfoSerializer hardwareInfoSerializer = new HardwareInfoSerializer(this.engines, this.config.getRunId(), this.buildId+"");
        hardwareInfoSerializer.serialize();

        List<AbstractDataSerializer> serializers = new ArrayList<>();

        if (this.config.getLauncherMode() == LauncherMode.REPAIR) {
            serializers.add(new InspectorSerializer(this.engines));
            serializers.add(new InspectorTimeSerializer(this.engines));
        } else {
            serializers.add(new InspectorSerializer4Bears(this.engines));
            serializers.add(new InspectorTimeSerializer4Bears(this.engines));
        }
        serializers.add(new NopolSerializer(this.engines));
        serializers.add(new NPEFixSerializer(this.engines));
        serializers.add(new AstorSerializer(this.engines));
        serializers.add(new MetricsSerializer(this.engines));

        ProjectInspector inspector;

        if (config.getLauncherMode() == LauncherMode.BEARS) {
            inspector = new ProjectInspector4Bears(buildToBeInspected, this.config.getWorkspacePath(), serializers, this.notifiers);
        } else {
            inspector = new ProjectInspector(buildToBeInspected, this.config.getWorkspacePath(), serializers, this.notifiers);
        }
        inspector.run();

        LOGGER.info("Inspector is finished. The process will now exit.");
        System.exit(0);
    }

    public static void main(String[] args) throws IOException, JSAPException {
        Launcher launcher = new Launcher(args);
        launcher.mainProcess();
    }
}
