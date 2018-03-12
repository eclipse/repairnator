package fr.inria.spirals.repairnator;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import fr.inria.spirals.repairnator.notifier.engines.EmailNotifierEngine;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.json.JSONFileSerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.json.MongoDBSerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.table.CSVSerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.table.GoogleSpreadsheetSerializerEngine;
import fr.inria.spirals.repairnator.serializer.gspreadsheet.GoogleSpreadSheetFactory;
import fr.inria.spirals.repairnator.serializer.mongodb.MongoConnection;
import org.slf4j.Logger;

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

    public static Switch defineArgDebug() {
        Switch sw = new Switch("debug");
        sw.setShortFlag('d');
        sw.setLongFlag("debug");
        sw.setDefault("false");
        return sw;
    }

    public static Switch defineArgNotifyEndProcess() {
        Switch sw = new Switch("notifyEndProcess");
        sw.setLongFlag("notifyEndProcess");
        sw.setDefault("false");
        sw.setHelp("Activate the notification when the process ends.");
        return sw;
    }

    public static void checkArguments(JSAP jsap, JSAPResult arguments, LauncherType launcherType) {
        if (!arguments.success()) {
            // print out specific error messages describing the problems
            for (java.util.Iterator<?> errs = arguments.getErrorMessageIterator(); errs.hasNext();) {
                System.err.println("Error: " + errs.next());
            }
            printUsage(jsap, launcherType);
        }

        if (arguments.getBoolean("help")) {
            printUsage(jsap, launcherType);
        }

        if (launcherType == LauncherType.SCANNER) {
            if (!arguments.getString("launcherMode").equals("bears") && arguments.getBoolean("skip-failing")) {
                printUsage(jsap, launcherType);
            }
        }
    }

    public static void checkEnvironmentVariables(JSAP jsap, LauncherType launcherType) {
        for (String envVar : Utils.ENVIRONMENT_VARIABLES) {
            if (System.getenv(envVar) == null || System.getenv(envVar).equals("")) {
                System.err.println("You must set the following environment variable: "+envVar);
                LauncherUtils.printUsage(jsap, launcherType);
            }
        }
    }

    public static void printUsage(JSAP jsap, LauncherType launcherType) {
        String moduleName = "repairnator-"+launcherType.name().toLowerCase();
        System.err.println("Usage: java <"+moduleName+" name> [option(s)]");
        System.err.println();
        System.err.println("Options: ");
        System.err.println();
        System.err.println(jsap.getHelp());

        if (launcherType == LauncherType.DOCKERPOOL || launcherType == LauncherType.REALTIME) {
            System.err.println("Please note that the following environment variables must be set: ");
            for (String env : Utils.ENVIRONMENT_VARIABLES) {
                System.err.println(" - " + env);
            }
            System.err.println("For using Nopol, you must add tools.jar in your classpath from your installed jdk");
        }

        System.exit(-1);
    }

    public static List<NotifierEngine> initNotifierEngines(JSAPResult arguments, Logger logger) {
        List<NotifierEngine> notifierEngines = new ArrayList<>();
        if (arguments.getString("smtpServer") != null && arguments.getStringArray("notifyto") != null) {
            logger.info("The email notifier engine will be used.");

            notifierEngines.add(new EmailNotifierEngine(arguments.getStringArray("notifyto"), arguments.getString("smtpServer")));
        } else {
            logger.info("The email notifier engine won't be used.");
        }
        return notifierEngines;
    }

    public static SerializerEngine initMongoDBSerializerEngine(JSAPResult arguments, Logger logger) {
        if (arguments.getString("mongoDBHost") != null) {
            logger.info("Initialize mongoDB serializer engine.");
            MongoConnection mongoConnection = new MongoConnection(arguments.getString("mongoDBHost"), arguments.getString("mongoDBName"));
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

    public static SerializerEngine initSpreadsheetSerializerEngineWithFileSecret(JSAPResult arguments, Logger logger) {
        if (arguments.getString("spreadsheet") != null && arguments.getFile("googleSecretPath").exists()) {
            logger.info("Initialize Google spreadsheet serializer engine.");
            GoogleSpreadSheetFactory.setSpreadsheetId(arguments.getString("spreadsheet"));
            try {
                GoogleSpreadSheetFactory.initWithFileSecret(arguments.getFile("googleSecretPath").getPath());
                return new GoogleSpreadsheetSerializerEngine();
            } catch (IOException | GeneralSecurityException e) {
                logger.error("Error while initializing Google Spreadsheet, no information will be serialized in spreadsheets.", e);
            }
        } else {
            logger.info("Google Spreadsheet won't be used for serialization.");
        }
        return null;
    }

    public static SerializerEngine initSpreadsheetSerializerEngineWithAccessToken(JSAPResult arguments, Logger logger) {
        if (arguments.getString("spreadsheet") != null && arguments.getString("googleAccessToken") != null) {
            logger.info("Initialize Google spreadsheet serializer engine.");
            GoogleSpreadSheetFactory.setSpreadsheetId(arguments.getString("spreadsheet"));
            try {
                if (GoogleSpreadSheetFactory.initWithAccessToken(arguments.getString("googleAccessToken"))) {
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

    public static List<SerializerEngine> initFileSerializerEngines(JSAPResult arguments, Logger logger) {
        List<SerializerEngine> fileSerializerEngines = new ArrayList<>();
        if (arguments.getFile("output") != null) {
            logger.info("Initialize file serializer engines.");

            String path = arguments.getFile("output").getPath();
            path += arguments.contains("build") ? "/"+arguments.getInt("build") : "";

            fileSerializerEngines.add(new CSVSerializerEngine(path));
            fileSerializerEngines.add(new JSONFileSerializerEngine(path));
        } else {
            logger.info("File serializers won't be used.");
        }
        return fileSerializerEngines;
    }

}
