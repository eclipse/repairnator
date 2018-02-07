package fr.inria.spirals.repairnator.realtime;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.stringparsers.FileStringParser;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.engines.EmailNotifierEngine;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.serializer.HardwareInfoSerializer;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.json.JSONFileSerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.json.MongoDBSerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.table.CSVSerializerEngine;
import fr.inria.spirals.repairnator.serializer.mongodb.MongoConnection;
import fr.inria.spirals.repairnator.states.LauncherMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RTLauncher {
    private static Logger LOGGER = LoggerFactory.getLogger(RTLauncher.class);
    private final LauncherMode launcherMode;
    private JSAP jsap;
    private JSAPResult arguments;
    private List<SerializerEngine> engines;
    private RepairnatorConfig config;

    private RTLauncher(String[] args) throws JSAPException {
        this.defineArgs();
        this.arguments = jsap.parse(args);
        this.checkArguments();
        this.checkEnvironmentVariables();
        this.launcherMode = LauncherMode.REPAIR;

        this.initConfig();
        this.initializeSerializerEngines();
        this.initNotifiers();
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

        sw1 = new Switch("skipDelete");
        sw1.setLongFlag("skipDelete");
        sw1.setDefault("false");
        sw1.setHelp("Skip the deletion of docker container.");
        this.jsap.registerParameter(sw1);

        sw1 = new Switch("notifyEndProcess");
        sw1.setLongFlag("notifyEndProcess");
        sw1.setDefault("false");
        sw1.setHelp("Activate the notification when the process ends.");
        this.jsap.registerParameter(sw1);

        sw1 = new Switch("createOutputDir");
        sw1.setLongFlag("createOutputDir");
        sw1.setDefault("false");
        sw1.setHelp("Create a specific directory for output.");
        this.jsap.registerParameter(sw1);

        FlaggedOption opt2 = new FlaggedOption("imageName");
        opt2.setShortFlag('n');
        opt2.setLongFlag("name");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setRequired(true);
        opt2.setHelp("Specify the docker image name to use.");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("output");
        opt2.setShortFlag('o');
        opt2.setLongFlag("output");
        opt2.setStringParser(FileStringParser.getParser().setMustBeDirectory(true).setMustExist(true));
        opt2.setRequired(true);
        opt2.setHelp("Specify where to put serialized files from dockerpool");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("logDirectory");
        opt2.setShortFlag('l');
        opt2.setLongFlag("logDirectory");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setRequired(true);
        opt2.setHelp("Specify where to put logs and serialized files created by docker machines.");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("threads");
        opt2.setShortFlag('t');
        opt2.setLongFlag("threads");
        opt2.setStringParser(JSAP.INTEGER_PARSER);
        opt2.setDefault("2");
        opt2.setHelp("Specify the number of threads to run in parallel");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("runId");
        opt2.setLongFlag("runId");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("Specify the run id for this launch.");
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
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("Specify push URL to push data from docker builds");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("smtpServer");
        opt2.setLongFlag("smtpServer");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("Specify SMTP server to use for Email notification");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("notifyto");
        opt2.setLongFlag("notifyto");
        opt2.setList(true);
        opt2.setListSeparator(',');
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("Specify email adresses to notify");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("whitelist");
        opt2.setShortFlag('w');
        opt2.setLongFlag("whitelist");
        opt2.setStringParser(FileStringParser.getParser().setMustBeDirectory(false).setMustExist(true));
        opt2.setHelp("Specify the path of whitelisted repository");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("blacklist");
        opt2.setShortFlag('b');
        opt2.setLongFlag("blacklist");
        opt2.setStringParser(FileStringParser.getParser().setMustBeDirectory(false).setMustExist(true));
        opt2.setHelp("Specify the path of blacklisted repository");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("jobsleeptime");
        opt2.setLongFlag("jobsleeptime");
        opt2.setStringParser(JSAP.INTEGER_PARSER);
        opt2.setDefault(InspectJobs.JOB_SLEEP_TIME+"");
        opt2.setHelp("Specify the sleep time between two requests to Travis Job endpoint (in seconds)");
        this.jsap.registerParameter(opt2);
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

    private void printUsage() {
        System.err.println("Usage: java <repairnator-rtscanner name> [option(s)]");
        System.err.println();
        System.err.println("Options : ");
        System.err.println();
        System.err.println(jsap.getHelp());
        System.exit(-1);
    }

    private void checkEnvironmentVariables() {
        for (String envVar : Utils.ENVIRONMENT_VARIABLES) {
            if (System.getenv(envVar) == null || System.getenv(envVar).equals("")) {
                System.err.println("You must set the following environment variable: "+envVar);
                this.printUsage();
            }
        }
    }

    private void initConfig() {
        this.config = RepairnatorConfig.getInstance();
        this.config.setLauncherMode(this.launcherMode);

        if (this.arguments.getString("pushUrl") != null) {
            this.config.setPush(true);
            this.config.setPushRemoteRepo(this.arguments.getString("pushUrl"));
        }
        this.config.setRunId(this.arguments.getString("runId"));
        this.config.setMongodbHost(this.arguments.getString("mongoDBHost"));
        this.config.setMongodbName(this.arguments.getString("mongoDBName"));
        this.config.setSmtpServer(this.arguments.getString("smtpServer"));
        this.config.setNotifyTo(this.arguments.getStringArray("notifyto"));
    }

    private void initializeSerializerEngines() {
        this.engines = new ArrayList<>();

        this.engines.add(new CSVSerializerEngine(this.arguments.getFile("output").getPath()));
        this.engines.add(new JSONFileSerializerEngine(this.arguments.getFile("output").getPath()));

        if (this.arguments.getString("mongoDBHost") != null) {
            LOGGER.info("Initialize mongoDB serializer engine.");
            MongoConnection mongoConnection = new MongoConnection(this.arguments.getString("mongoDBHost"), this.arguments.getString("mongoDBName"));
            if (mongoConnection.isConnected()) {
                this.engines.add(new MongoDBSerializerEngine(mongoConnection));
            }
        } else {
            LOGGER.info("MongoDB won't be used for serialization");
        }
    }

    private void initNotifiers() {
        if (this.arguments.getBoolean("notifyEndProcess")) {
            List<NotifierEngine> notifierEngines = new ArrayList<>();
            if (this.arguments.getString("smtpServer") != null && this.arguments.getStringArray("notifyto") != null) {
                LOGGER.info("The email notifier engine will be used.");

                notifierEngines.add(new EmailNotifierEngine(this.arguments.getStringArray("notifyto"), this.arguments.getString("smtpServer")));
            } else {
                LOGGER.info("The email notifier engine won't be used.");
            }
        }
    }

    private void initAndRunRTScanner() {
        LOGGER.info("Init RTScanner...");
        String runId = this.arguments.getString("runId");
        HardwareInfoSerializer hardwareInfoSerializer = new HardwareInfoSerializer(this.engines, runId, "rtScanner");
        hardwareInfoSerializer.serialize();
        RTScanner rtScanner = new RTScanner(runId, this.engines);
        rtScanner.getInspectJobs().setSleepTime(this.arguments.getInt("jobsleeptime"));

        LOGGER.info("Init build runner");
        BuildRunner buildRunner = rtScanner.getBuildRunner();
        buildRunner.setDockerOutputDir(this.arguments.getString("logDirectory"));
        buildRunner.setRunId(runId);
        buildRunner.setCreateOutputDir(this.arguments.getBoolean("createOutputDir"));
        buildRunner.setSkipDelete(this.arguments.getBoolean("skipDelete"));
        buildRunner.setEngines(this.engines);
        buildRunner.setDockerImageName(this.arguments.getString("imageName"));
        buildRunner.initExecutorService(this.arguments.getInt("threads"));


        if (this.arguments.contains("whitelist")) {
            rtScanner.initWhiteListedRepository(this.arguments.getFile("whitelist"));
        }

        if (this.arguments.contains("blacklist")) {
            rtScanner.initBlackListedRepository(this.arguments.getFile("blacklist"));
        }

        LOGGER.info("Start RTScanner...");
        rtScanner.launch();
    }

    public static void main(String[] args) throws JSAPException {
        RTLauncher rtLauncher = new RTLauncher(args);
        rtLauncher.initAndRunRTScanner();
    }

}
