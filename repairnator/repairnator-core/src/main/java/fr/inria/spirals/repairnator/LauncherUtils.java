package fr.inria.spirals.repairnator;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.stringparsers.EnumeratedStringParser;
import com.martiansoftware.jsap.stringparsers.FileStringParser;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.engines.EmailNotifierEngine;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.json.JSONFileSerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.json.MongoDBSerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.table.CSVSerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.table.GoogleSpreadsheetSerializerEngine;
import fr.inria.spirals.repairnator.serializer.gspreadsheet.GoogleSpreadSheetFactory;
import fr.inria.spirals.repairnator.serializer.mongodb.MongoConnection;
import fr.inria.spirals.repairnator.states.LauncherMode;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by fermadeiral
 */
public class LauncherUtils {

    public static Switch defineArgHelp() {
        Switch sw = new Switch("help");
        sw.setShortFlag('h');
        sw.setLongFlag("help");
        sw.setDefault("false");
        return sw;
    }

    public static boolean getArgHelp(JSAPResult arguments) {
        return arguments.getBoolean("help");
    }

    public static Switch defineArgDebug() {
        Switch sw = new Switch("debug");
        sw.setShortFlag('d');
        sw.setLongFlag("debug");
        sw.setDefault("false");
        return sw;
    }

    public static boolean getArgDebug(JSAPResult arguments) {
        return arguments.getBoolean("debug");
    }

    public static Switch defineArgNotifyEndProcess() {
        Switch sw = new Switch("notifyEndProcess");
        sw.setLongFlag("notifyEndProcess");
        sw.setDefault("false");
        sw.setHelp("Activate the notification when the process ends.");
        return sw;
    }

    public static boolean getArgNotifyEndProcess(JSAPResult arguments) {
        return arguments.getBoolean("notifyEndProcess");
    }

    public static FlaggedOption defineArgLauncherMode(String helpMessage) {
        String launcherModeValues = "";
        for (LauncherMode mode : LauncherMode.values()) {
            launcherModeValues += mode.name() + ";";
        }
        launcherModeValues = launcherModeValues.substring(0, launcherModeValues.length() - 1);

        FlaggedOption opt = new FlaggedOption("launcherMode");
        opt.setShortFlag('m');
        opt.setLongFlag("launcherMode");
        opt.setStringParser(EnumeratedStringParser.getParser(launcherModeValues));
        opt.setRequired(true);
        opt.setHelp(helpMessage);
        return opt;
    }

    public static LauncherMode getArgLauncherMode(JSAPResult arguments) {
        return LauncherMode.valueOf(arguments.getString("launcherMode").toUpperCase());
    }

    public static FlaggedOption defineArgRunId() {
        FlaggedOption opt = new FlaggedOption("runId");
        opt.setLongFlag("runId");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Specify the run id for this launch.");
        return opt;
    }

    public static String getArgRunId(JSAPResult arguments) {
        return arguments.getString("runId");
    }

    public static FlaggedOption defineArgInput(String helpMessage) {
        FlaggedOption opt = new FlaggedOption("input");
        opt.setShortFlag('i');
        opt.setLongFlag("input");
        opt.setStringParser(FileStringParser.getParser().setMustExist(true).setMustBeFile(true));
        opt.setRequired(true);
        opt.setHelp(helpMessage);
        return opt;
    }

    public static File getArgInput(JSAPResult arguments) {
        return arguments.getFile("input");
    }

    public static FlaggedOption defineArgOutput(LauncherType launcherType, String helpMessage) {
        FlaggedOption opt = new FlaggedOption("output");
        opt.setShortFlag('o');
        opt.setLongFlag("output");

        FileStringParser fileStringParser = FileStringParser.getParser();
        if (launcherType == LauncherType.SCANNER || launcherType == LauncherType.CHECKBRANCHES) {
            fileStringParser.setMustBeFile(true);
        } else {
            fileStringParser.setMustBeDirectory(true).setMustExist(true);
        }
        opt.setStringParser(fileStringParser);

        if (launcherType == LauncherType.DOCKERPOOL || launcherType == LauncherType.REALTIME || launcherType == LauncherType.CHECKBRANCHES) {
            opt.setRequired(true);
        }

        opt.setHelp(helpMessage);

        return opt;
    }

    public static File getArgOutput(JSAPResult arguments) {
        return arguments.getFile("output");
    }

    public static FlaggedOption defineArgMongoDBHost() {
        FlaggedOption opt = new FlaggedOption("mongoDBHost");
        opt.setLongFlag("dbhost");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Specify mongodb host.");
        return opt;
    }

    public static String getArgMongoDBHost(JSAPResult arguments) {
        return arguments.getString("mongoDBHost");
    }

    public static FlaggedOption defineArgMongoDBName() {
        FlaggedOption opt = new FlaggedOption("mongoDBName");
        opt.setLongFlag("dbname");
        opt.setDefault("repairnator");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Specify mongodb DB name.");
        return opt;
    }

    public static String getArgMongoDBName(JSAPResult arguments) {
        return arguments.getString("mongoDBName");
    }

    public static FlaggedOption defineArgSpreadsheetId() {
        FlaggedOption opt = new FlaggedOption("spreadsheet");
        opt.setLongFlag("spreadsheet");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Specify Google Spreadsheet ID to put data.");
        return opt;
    }

    public static String getArgSpreadsheetId(JSAPResult arguments) {
        return arguments.getString("spreadsheet");
    }

    public static FlaggedOption defineArgGoogleSecretPath() {
        FlaggedOption opt = new FlaggedOption("googleSecretPath");
        opt.setLongFlag("googleSecretPath");
        opt.setStringParser(FileStringParser.getParser().setMustBeFile(true));
        opt.setDefault("./client_secret.json");
        opt.setHelp("Specify the path to the JSON google secret for serializing.");
        return opt;
    }

    public static File getArgGoogleSecretPath(JSAPResult arguments) {
        return arguments.getFile("googleSecretPath");
    }

    public static FlaggedOption defineArgGoogleAccessToken() {
        FlaggedOption opt = new FlaggedOption("googleAccessToken");
        opt.setLongFlag("googleAccessToken");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Specify the google access token to use for serializers.");
        return opt;
    }

    public static String getArgGoogleAccessToken(JSAPResult arguments) {
        return arguments.getString("googleAccessToken");
    }

    public static FlaggedOption defineArgSmtpServer() {
        FlaggedOption opt = new FlaggedOption("smtpServer");
        opt.setLongFlag("smtpServer");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Specify SMTP server to use for Email notification");
        return opt;
    }

    public static String getArgSmtpServer(JSAPResult arguments) {
        return arguments.getString("smtpServer");
    }

    public static FlaggedOption defineArgNotifyto() {
        FlaggedOption opt = new FlaggedOption("notifyto");
        opt.setLongFlag("notifyto");
        opt.setList(true);
        opt.setListSeparator(',');
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Specify email addresses to notify");
        return opt;
    }

    public static String[] getArgNotifyto(JSAPResult arguments) {
        return arguments.getStringArray("notifyto");
    }

    public static Switch defineArgCreateOutputDir() {
        Switch sw = new Switch("createOutputDir");
        sw.setLongFlag("createOutputDir");
        sw.setDefault("false");
        sw.setHelp("Create a specific directory for output.");
        return sw;
    }

    public static boolean getArgCreateOutputDir(JSAPResult arguments) {
        return arguments.getBoolean("createOutputDir");
    }

    public static FlaggedOption defineArgLogDirectory() {
        FlaggedOption opt = new FlaggedOption("logDirectory");
        opt.setShortFlag('l');
        opt.setLongFlag("logDirectory");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setRequired(true);
        opt.setHelp("Specify where to put logs and serialized files created by docker machines.");
        return opt;
    }

    public static String getArgLogDirectory(JSAPResult arguments) {
        return arguments.getString("logDirectory");
    }

    public static Switch defineArgSkipDelete() {
        Switch sw = new Switch("skipDelete");
        sw.setLongFlag("skipDelete");
        sw.setDefault("false");
        sw.setHelp("Skip the deletion of docker container.");
        return sw;
    }

    public static boolean getArgSkipDelete(JSAPResult arguments) {
        return arguments.getBoolean("skipDelete");
    }

    public static FlaggedOption defineArgNbThreads() {
        FlaggedOption opt = new FlaggedOption("threads");
        opt.setShortFlag('t');
        opt.setLongFlag("threads");
        opt.setStringParser(JSAP.INTEGER_PARSER);
        opt.setDefault("2");
        opt.setHelp("Specify the number of threads to run in parallel");
        return opt;
    }

    public static int getArgNbThreads(JSAPResult arguments) {
        return arguments.getInt("threads");
    }

    public static FlaggedOption defineArgGlobalTimeout() {
        FlaggedOption opt = new FlaggedOption("globalTimeout");
        opt.setShortFlag('g');
        opt.setLongFlag("globalTimeout");
        opt.setStringParser(JSAP.INTEGER_PARSER);
        opt.setDefault("1");
        opt.setHelp("Specify the number of day before killing the whole pool.");
        return opt;
    }

    public static int getArgGlobalTimeout(JSAPResult arguments) {
        return arguments.getInt("globalTimeout");
    }

    public static FlaggedOption defineArgPushUrl() {
        FlaggedOption opt = new FlaggedOption("pushUrl");
        opt.setLongFlag("pushurl");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Specify repository URL to push data.");
        return opt;
    }

    public static String getArgPushUrl(JSAPResult arguments) {
        return arguments.getString("pushUrl");
    }

    public static FlaggedOption defineArgDockerImageName() {
        FlaggedOption opt = new FlaggedOption("imageName");
        opt.setShortFlag('n');
        opt.setLongFlag("name");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setRequired(true);
        opt.setHelp("Specify the docker image name to use.");
        return opt;
    }

    public static String getArgDockerImageName(JSAPResult arguments) {
        return arguments.getString("imageName");
    }

    public static void checkArguments(JSAP jsap, JSAPResult arguments, LauncherType launcherType) {
        if (!arguments.success()) {
            // print out specific error messages describing the problems
            for (java.util.Iterator<?> errs = arguments.getErrorMessageIterator(); errs.hasNext();) {
                System.err.println("Error: " + errs.next());
            }
            printUsage(jsap, launcherType);
        }

        if (getArgHelp(arguments)) {
            printUsage(jsap, launcherType);
        }
    }

    public static void checkEnvironmentVariable(String envVariable, JSAP jsap, LauncherType launcherType) {
        if (System.getenv(envVariable) == null || System.getenv(envVariable).equals("")) {
            System.err.println("You must set the following environment variable: "+envVariable);
            LauncherUtils.printUsage(jsap, launcherType);
        }
    }

    public static void printUsage(JSAP jsap, LauncherType launcherType) {
        String moduleName = "repairnator-"+launcherType.name().toLowerCase();
        System.err.println("Usage: java <"+moduleName+" name> [option(s)]");
        System.err.println();
        System.err.println("Options: ");
        System.err.println();
        System.err.println(jsap.getHelp());

        System.err.println("Please note that the GITHUB_OAUTH environment variables must be set.");

        if (launcherType == LauncherType.PIPELINE) {
            System.err.println("For using Nopol, you must add tools.jar in your classpath from your installed jdk");
        }

        System.exit(-1);
    }

    public static List<NotifierEngine> initNotifierEngines(Logger logger) {
        List<NotifierEngine> notifierEngines = new ArrayList<>();
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        if (config.getSmtpServer() != null && config.getNotifyTo() != null) {
            logger.info("The email notifier engine will be used.");

            notifierEngines.add(new EmailNotifierEngine(config.getNotifyTo(), config.getSmtpServer()));
        } else {
            logger.info("The email notifier engine won't be used.");
        }
        return notifierEngines;
    }

    public static SerializerEngine initMongoDBSerializerEngine(Logger logger) {
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        if (config.getMongodbHost() != null) {
            logger.info("Initialize mongoDB serializer engine.");
            MongoConnection mongoConnection = new MongoConnection(config.getMongodbHost(), config.getMongodbName());
            if (mongoConnection.isConnected()) {
                return new MongoDBSerializerEngine(mongoConnection);
            } else {
                logger.error("Error while connecting to mongoDB.");
            }
        } else {
            logger.info("MongoDB won't be used for serialization.");
        }
        return null;
    }

    public static SerializerEngine initSpreadsheetSerializerEngineWithFileSecret(Logger logger) {
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        if (config.getSpreadsheetId() != null && (new File(config.getGoogleSecretPath())).exists()) {
            logger.info("Initialize Google spreadsheet serializer engine.");
            GoogleSpreadSheetFactory.setSpreadsheetId(config.getSpreadsheetId());
            try {
                GoogleSpreadSheetFactory.initWithFileSecret(config.getGoogleSecretPath());
                return new GoogleSpreadsheetSerializerEngine();
            } catch (IOException | GeneralSecurityException e) {
                logger.error("Error while initializing Google Spreadsheet, no information will be serialized in spreadsheets.", e);
            }
        } else {
            logger.info("Google Spreadsheet won't be used for serialization.");
        }
        return null;
    }

    public static SerializerEngine initSpreadsheetSerializerEngineWithAccessToken(Logger logger) {
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        if (config.getSpreadsheetId() != null && config.getGoogleAccessToken() != null) {
            logger.info("Initialize Google spreadsheet serializer engine.");
            GoogleSpreadSheetFactory.setSpreadsheetId(config.getSpreadsheetId());
            try {
                if (GoogleSpreadSheetFactory.initWithAccessToken(config.getGoogleAccessToken())) {
                    return new GoogleSpreadsheetSerializerEngine();
                } else {
                    logger.error("Error while initializing Google Spreadsheet, no information will be serialized in spreadsheets.");
                }
            } catch (IOException | GeneralSecurityException e) {
                logger.error("Error while initializing Google Spreadsheet, no information will be serialized in spreadsheets.", e);
            }
        } else {
            logger.info("Google Spreadsheet won't be used for serialization.");
        }
        return null;
    }

    public static List<SerializerEngine> initFileSerializerEngines(Logger logger) {
        List<SerializerEngine> fileSerializerEngines = new ArrayList<>();
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        if (config.getOutputPath() != null) {
            logger.info("Initialize file serializer engines.");

            String path = config.getOutputPath();
            path += config.getBuildId() > 0 ? "/"+config.getBuildId() : "";

            fileSerializerEngines.add(new CSVSerializerEngine(path));
            fileSerializerEngines.add(new JSONFileSerializerEngine(path));
        } else {
            logger.info("File serializers won't be used.");
        }
        return fileSerializerEngines;
    }

}
