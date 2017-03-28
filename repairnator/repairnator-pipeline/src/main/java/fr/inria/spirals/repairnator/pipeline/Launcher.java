package fr.inria.spirals.repairnator.pipeline;

import ch.qos.logback.classic.Level;
import com.martiansoftware.jsap.*;
import com.martiansoftware.jsap.stringparsers.EnumeratedStringParser;
import com.martiansoftware.jsap.stringparsers.FileStringParser;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.BuildStatus;
import fr.inria.spirals.jtravis.helpers.BuildHelper;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.LauncherMode;
import fr.inria.spirals.repairnator.ScannedBuildStatus;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.config.RepairnatorConfigException;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector4Bears;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.serializer.InspectorSerializer;
import fr.inria.spirals.repairnator.serializer.InspectorSerializer4Bears;
import fr.inria.spirals.repairnator.serializer.InspectorTimeSerializer;
import fr.inria.spirals.repairnator.serializer.InspectorTimeSerializer4Bears;
import fr.inria.spirals.repairnator.serializer.NopolSerializer;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.json.JSONFileSerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.json.MongoDBSerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.table.CSVSerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.table.GoogleSpreadsheetSerializerEngine;
import fr.inria.spirals.repairnator.serializer.gspreadsheet.GoogleSpreadSheetFactory;
import fr.inria.spirals.repairnator.serializer.mongodb.MongoConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by urli on 09/03/2017.
 */
public class Launcher {

    private static Logger LOGGER = LoggerFactory.getLogger(Launcher.class);
    private JSAP jsap;
    private JSAPResult arguments;
    private RepairnatorConfig config;
    private int buildId;
    private BuildToBeInspected buildToBeInspected;
    private List<SerializerEngine> engines;


    public Launcher(String[] args) throws JSAPException {
        this.defineArgs();
        this.arguments = jsap.parse(args);
        this.checkArguments();
        this.checkEnvironmentVariables();
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

        this.initializeSerializerEngines();
    }

    private void initConfig() {
        this.config = RepairnatorConfig.getInstance();
        this.config.setLauncherMode(LauncherMode.valueOf(this.arguments.getString("launcherMode").toUpperCase()));
        this.config.setClean(true);
        this.config.setZ3solverPath(this.arguments.getFile("z3").getPath());
        if (this.arguments.getFile("outputPath") != null) {
            this.config.setSerializeJson(true);
            this.config.setJsonOutputPath(this.arguments.getFile("outputPath").getPath());
        }
        if (this.arguments.getInetAddress("pushUrl") != null) {
            this.config.setPush(true);
            this.config.setPushRemoteRepo(this.arguments.getInetAddress("pushUrl").toString());
        }
        this.config.setWorkspacePath(this.arguments.getString("workspace"));
    }

    private void initializeSerializerEngines() {
        this.engines = new ArrayList<>();

        if (this.arguments.getString("spreadsheet") != null) {
            LOGGER.info("Initialize Google spreadsheet serializer engine.");
            GoogleSpreadSheetFactory.setSpreadsheetId(this.arguments.getString("spreadsheet"));

            try {
                if (GoogleSpreadSheetFactory.initWithAccessToken(this.arguments.getString("googleAccessToken"))) {
                    this.engines.add(new GoogleSpreadsheetSerializerEngine());
                } else {
                    LOGGER.error("Error while initializing Google Spreadsheet, no information will be serialized in spreadsheets");
                }
            } catch (IOException | GeneralSecurityException e) {
                LOGGER.error("Error while initializing Google Spreadsheet, no information will be serialized in spreadsheets", e);
            }
        } else {
            LOGGER.info("Google Spreadsheet won't be used for serialization.");
        }

        if (config.isSerializeJson()) {
            LOGGER.info("Initialize files serializers engines.");
            String serializedFiles = config.getJsonOutputPath()+"/"+this.buildId;
            this.engines.add(new CSVSerializerEngine(serializedFiles));
            this.engines.add(new JSONFileSerializerEngine(serializedFiles));
        } else {
            LOGGER.info("Files serializer won't be used");
        }

        if (this.arguments.getString("mongoDBHost") != null) {
            LOGGER.info("Initialize mongoDB serializer engine.");
            MongoConnection mongoConnection = new MongoConnection(this.arguments.getString("mongoDBHost"), this.arguments.getString("mongoDBName"));
            if (mongoConnection.isConnected()) {
                this.engines.add(new MongoDBSerializerEngine(mongoConnection));
            } else {
                LOGGER.error("Error while connecting to mongoDB.");
            }
        } else {
            LOGGER.info("MongoDB won't be used for serialization");
        }
    }

    private void checkNopolSolverPath() {
        String solverPath = this.config.getZ3solverPath();

        if (solverPath != null) {
            File file = new File(solverPath);

            if (!file.exists()) {
                System.err.println("The Nopol solver path should be an existing file: " + file.getPath() + " does not exist.");
                this.printUsage();
                System.exit(-1);
            }
        } else {
            System.err.println("The Nopol solver path should be provided.");
            this.printUsage();
            System.exit(-1);
        }
    }

    private void checkArguments() {
        if (!this.arguments.success()) {
            // print out specific error messages describing the problems
            for (java.util.Iterator<?> errs = arguments.getErrorMessageIterator(); errs.hasNext();) {
                System.err.println("Error: " + errs.next());
            }
            this.printUsage();
        }

        if (this.arguments.getBoolean("help")) {
            this.printUsage();
        }
    }

    private void defineArgs() throws JSAPException {
        // Verbose output
        this.jsap = new JSAP();

        // help
        Switch sw1 = new Switch("help");
        sw1.setShortFlag('h');
        sw1.setLongFlag("help");
        sw1.setDefault("false");
        this.jsap.registerParameter(sw1);

        // verbosity
        sw1 = new Switch("debug");
        sw1.setShortFlag('d');
        sw1.setLongFlag("debug");
        sw1.setDefault("false");
        this.jsap.registerParameter(sw1);

        FlaggedOption opt2 = new FlaggedOption("build");
        opt2.setShortFlag('b');
        opt2.setLongFlag("build");
        opt2.setStringParser(JSAP.INTEGER_PARSER);
        opt2.setRequired(true);
        opt2.setHelp("Specify the build id to use.");
        this.jsap.registerParameter(opt2);

        String launcherModeValues = "";
        for (LauncherMode mode : LauncherMode.values()) {
            launcherModeValues += mode.name() + ";";
        }
        launcherModeValues.substring(0, launcherModeValues.length() - 2);

        // Launcher mode
        opt2 = new FlaggedOption("launcherMode");
        opt2.setShortFlag('m');
        opt2.setLongFlag("launcherMode");
        opt2.setStringParser(EnumeratedStringParser.getParser(launcherModeValues));
        opt2.setRequired(true);
        opt2.setHelp("Specify if RepairNator will be launch for repairing (REPAIR) or for collecting fixer builds (BEARS).");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("runId");
        opt2.setLongFlag("runId");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("Specify the runId for this launch.");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("googleAccessToken");
        opt2.setShortFlag('g');
        opt2.setLongFlag("googleAccessToken");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("Specify the google access token to use for serializers.");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("spreadsheet");
        opt2.setLongFlag("spreadsheet");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("Specify Google Spreadsheet ID to put data.");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("mongoDBHost");
        opt2.setLongFlag("dbhost");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("Specify mongodb host.");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("mongoDBName");
        opt2.setLongFlag("dbname");
        opt2.setDefault("repairnator");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("Specify mongodb DB name.");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("pushUrl");
        opt2.setLongFlag("pushurl");
        opt2.setStringParser(JSAP.INETADDRESS_PARSER);
        opt2.setHelp("Specify repository to push data");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("z3");
        opt2.setLongFlag("z3");
        opt2.setDefault("./z3_for_linux");
        opt2.setStringParser(FileStringParser.getParser().setMustBeFile(true).setMustExist(true));
        opt2.setHelp("Specify path to Z3");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("outputPath");
        opt2.setLongFlag("output");
        opt2.setShortFlag('o');
        opt2.setStringParser(FileStringParser.getParser().setMustBeDirectory(true).setMustExist(true));
        opt2.setHelp("Specify path to output serialized files");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("workspace");
        opt2.setLongFlag("workspace");
        opt2.setShortFlag('w');
        opt2.setDefault("./workspace");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("Specify path to output serialized files");
        this.jsap.registerParameter(opt2);
    }

    private void checkEnvironmentVariables() {
        for (String envVar : Utils.ENVIRONMENT_VARIABLES) {
            if (System.getenv(envVar) == null || System.getenv(envVar).equals("")) {
                System.err.println("You must set the following environment variable: "+envVar);
                this.printUsage();
            }
        }
    }

    private void printUsage() {
        System.err.println("Usage: java <repairnator-pipeline name> [option(s)]");
        System.err.println();
        System.err.println("Options : ");
        System.err.println();
        System.err.println(jsap.getHelp());
        System.err.println("Please note that the following environment variables must be set: ");
        for (String env : Utils.ENVIRONMENT_VARIABLES) {
            System.err.println(" - " + env);
        }
        System.err.println("For using Nopol, you must add tools.jar in your classpath from your installed jdk");
        System.exit(-1);
    }

    private void checkToolsLoaded() {
        URLClassLoader loader;

        try {
            loader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            loader.loadClass("com.sun.jdi.AbsentInformationException");
        } catch (ClassNotFoundException e) {
            System.err.println("Tools.jar must be loaded, here the classpath given for your app: "+System.getProperty("java.class.path"));
            this.printUsage();
            System.exit(-1);
        }
    }

    private void getBuildToBeInspected() {
        Build build = BuildHelper.getBuildFromId(this.buildId, null);

        if (this.config.getLauncherMode() == LauncherMode.REPAIR) {
            this.buildToBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL, this.arguments.getString("runId"));
        } else {
            Build previousBuild = BuildHelper.getLastBuildOfSameBranchOfStatusBeforeBuild(build, null);
            if (previousBuild != null) {
                if (previousBuild.getBuildStatus() == BuildStatus.FAILED) {
                    this.buildToBeInspected = new BuildToBeInspected(build, previousBuild, ScannedBuildStatus.FAILING_AND_PASSING);
                } else {
                    this.buildToBeInspected = new BuildToBeInspected(build, previousBuild, ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES);
                }
            } else {
                throw new RuntimeException("There was an error getting the previous build");
            }
        }
    }

    private void mainProcess() throws IOException {
        LOGGER.info("Start by getting the build (buildId: "+this.buildId+") with the following config: "+this.config);
        this.getBuildToBeInspected();

        List<AbstractDataSerializer> serializers = new ArrayList<>();

        if (this.config.getLauncherMode() == LauncherMode.REPAIR) {
            serializers.add(new InspectorSerializer(this.engines));
            serializers.add(new InspectorTimeSerializer(this.engines));
            serializers.add(new NopolSerializer(this.engines));
        } else {
            serializers.add(new InspectorSerializer4Bears(this.engines));
            serializers.add(new InspectorTimeSerializer4Bears(this.engines));
        }

        ProjectInspector inspector;

        if (config.getLauncherMode() == LauncherMode.BEARS) {
            inspector = new ProjectInspector4Bears(buildToBeInspected, this.config.getWorkspacePath(), serializers);
        } else {
            inspector = new ProjectInspector(buildToBeInspected, this.config.getWorkspacePath(), serializers);
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
