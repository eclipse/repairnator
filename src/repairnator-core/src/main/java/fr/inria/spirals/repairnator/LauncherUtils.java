package fr.inria.spirals.repairnator;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.engines.EmailNotifierEngine;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.json.JSONFileSerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.json.MongoDBSerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.table.CSVSerializerEngine;
import fr.inria.spirals.repairnator.serializer.mongodb.MongoConnection;
import fr.inria.spirals.repairnator.utils.Utils;
import org.slf4j.Logger;

import java.io.File;
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

    public static Switch defineArgCreatePR() {
        Switch sw = new Switch("createPR");
        sw.setLongFlag("createPR");
        sw.setDefault("false");
        sw.setHelp("Activate the creation of a Pull Request in case of patch.");
        return sw;
    }

    public static boolean getArgCreatePR(JSAPResult arguments) {
        return arguments.getBoolean("createPR");
    }

    public static Switch defineArgCheckstyleMode() {
        Switch sw = new Switch("checkstyle");
        sw.setLongFlag("checkstyle");
        sw.setDefault("false");
        sw.setHelp("This mode allows to use repairnator to analyze build failing because of checkstyle.");
        return sw;
    }

    public static boolean getArgCheckstyleMode(JSAPResult arguments) {
        return arguments.getBoolean("checkstyle");
    }

    public static Switch defineArgSequencerRepairMode() {
        Switch sw = new Switch("sequencerRepair");
        sw.setLongFlag("sequencerRepair");
        sw.setDefault("false");
        sw.setHelp("This mode allows to use repairnator to run a SequencerRepair specific pipeline.");
        return sw;
    }

    public static boolean getArgSequencerRepairMode(JSAPResult arguments) {
        return arguments.getBoolean("sequencerRepair");
    }

    public static Switch defineArgBearsMode() {
        Switch sw = new Switch("bears");
        sw.setLongFlag("bears");
        sw.setDefault("false");
        sw.setHelp("This mode allows to use repairnator to analyze pairs of bugs and human-produced patches.");
        return sw;
    }

    public static boolean getArgBearsMode(JSAPResult arguments) {
        return arguments.getBoolean("bears");
    }

    public static FlaggedOption defineArgRunId() {
        FlaggedOption opt = new FlaggedOption("runId");
        opt.setLongFlag("runId");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setDefault("1234");
        opt.setHelp("Specify the run id for this launch.");
        return opt;
    }

    public static String getArgRunId(JSAPResult arguments) {
        return arguments.getString("runId");
    }

    public static FlaggedOption defineArgInput() {
        /* ./builds.txt contain one build id per line

            346537408
            347223109
            348887356
            349620528
            351046304
            353774072
            358555930
            361311603
            368867994
            369727490
            369854684
            369859631
            370885458
            371144762
            371488143
            374841318
            376812142

            (this is the list from Expedition 2)
         */
        FlaggedOption opt = new FlaggedOption("input");
        opt.setShortFlag('i');
        opt.setLongFlag("input");
        opt.setDefault("./builds.txt");
        opt.setHelp("Specify the input file containing the list of build ids.");
        return opt;
    }

    public static FlaggedOption defineArgProjectInput() {
        /* ./projects.txt contain one slug per line

        INRIA/spoon
        rails/rails
        ....
         */
        FlaggedOption opt = new FlaggedOption("input");
        opt.setShortFlag('i');
        opt.setLongFlag("input");
        opt.setDefault("./projects.txt");
        opt.setHelp("Specify where to find the list of projects to scan.");
        return opt;
    }

    public static FlaggedOption defineArgBranchInput() {
        /* ./branches.txt contain one branch per line
            typically from https://github.com/Spirals-Team/librepair-experiments/
         */
        FlaggedOption opt = new FlaggedOption("input");
        opt.setShortFlag('i');
        opt.setLongFlag("input");
        opt.setDefault("./projects.txt");
        opt.setHelp("Specify the input file containing the list of branches to reproduce (one branch per line)");
        return opt;
    }

    public static File getArgInput(JSAPResult arguments) {
        return arguments.getObject("input") instanceof File ? (File) arguments.getObject("input") : new File(arguments.getString("input"));
    }

    public static FlaggedOption defineArgOutput(LauncherType launcherType, String helpMessage) {
        FlaggedOption opt = new FlaggedOption("output");
        opt.setShortFlag('o');
        opt.setLongFlag("output");
        // we don't assume the presence of "/tmp" (eg on windows) and the it is writable
        opt.setDefault("./repairnator-output");
        if (launcherType == LauncherType.DOCKERPOOL || launcherType == LauncherType.CHECKBRANCHES) {
            opt.setRequired(true);
        }

        opt.setHelp(helpMessage);

        return opt;
    }

    public static File getArgOutput(JSAPResult arguments) {
        File output = new File(arguments.getString("output"));
        if (!output.exists()) { 
            output.mkdirs();
        }
        return output;
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
    
    public static FlaggedOption defineArgSmtpPort() {
        FlaggedOption opt = new FlaggedOption("smtpPort");
        opt.setLongFlag("smtpPort");
        opt.setStringParser(JSAP.INTEGER_PARSER);
        opt.setDefault("25");
        opt.setHelp("The port on which to contact the SMTP server. Default 25");
        return opt;
    }
    
    public static int getArgSmtpPort(JSAPResult arguments) {
        return arguments.getInt("smtpPort");
    }
    
    public static Switch defineArgSmtpTLS() {
        Switch sw = new Switch("smtpTLS");
        sw.setLongFlag("smtpTLS");
        sw.setDefault("false");
        sw.setHelp("Decides whether to use TLS for email communication.");
        return sw;
    }
    
    public static boolean getArgSmtpTLS(JSAPResult arguments) {
        return arguments.getBoolean("smtpTLS");
    }
    
    public static FlaggedOption defineArgSmtpUsername() {
        FlaggedOption opt = new FlaggedOption("smtpUsername");
        opt.setLongFlag("smtpUsername");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Username for authorized server");
        return opt;
    }
    
    public static String getArgSmtpUsername(JSAPResult arguments) {
        return arguments.getString("smtpUsername");
    }
    
    public static FlaggedOption defineArgSmtpPassword() {
        FlaggedOption opt = new FlaggedOption("smtpPassword");
        opt.setLongFlag("smtpPassword");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Password for authorized server");
        return opt;
    }
    
    public static String getArgSmtpPassword(JSAPResult arguments) {
        return arguments.getString("smtpPassword");
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


    public static String getArgLogDirectory(JSAPResult arguments) {
        return arguments.getString("output");
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
        opt.setDefault("1");
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
        opt.setHelp("Specify repository URL to push data on the format https://github.com/user/repo.");
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
        opt.setDefault("repairnator/pipeline:latest");
        opt.setHelp("Specify the docker image name to use.");
        return opt;
    }

    public static String getArgDockerImageName(JSAPResult arguments) {
        return arguments.getString("imageName");
    }

    public static FlaggedOption defineArgGithubOAuth() {
        FlaggedOption opt = new FlaggedOption("ghOauth");
        opt.setLongFlag("ghOauth");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setDefault(System.getenv("GITHUB_OAUTH"));
        opt.setHelp("Specify Github Token to use");
        return opt;
    }

    public static String getArgGithubOAuth(JSAPResult arguments) {
        return arguments.getString("ghOauth");
    }

    public static FlaggedOption defineArgGithubUserName() {
        FlaggedOption opt = new FlaggedOption("githubUserName");
        opt.setLongFlag("githubUserName");
        opt.setDefault("repairnator");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Specify the name of the user who commits");
        return opt;
    }

    public static String getArgGithubUserName(JSAPResult arguments) {
        return arguments.getString("githubUserName");
    }

    public static FlaggedOption defineArgGithubUserEmail() {
        FlaggedOption opt = new FlaggedOption("githubUserEmail");
        opt.setLongFlag("githubUserEmail");
        opt.setDefault("noreply@github.com");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Specify the email of the user who commits");
        return opt;
    }

    public static String getArgGithubUserEmail(JSAPResult arguments) {
        return arguments.getString("githubUserEmail");
    }

    public static void checkArguments(JSAP jsap, JSAPResult arguments, LauncherType launcherType) {
        if (!arguments.success()) {
            // print out specific error messages describing the problems
            for (java.util.Iterator<?> errs = arguments.getErrorMessageIterator(); errs.hasNext();) {
                System.err.println("Error: " + errs.next());
            }
        }

        if (getArgHelp(arguments)) {
            printUsage(jsap, launcherType);
        }


        checkPushUrlArg(jsap, arguments, launcherType);
    }



    public static void checkPushUrlArg(JSAP jsap, JSAPResult arguments, LauncherType launcherType) {
        if (getArgPushUrl(arguments) != null) {
            if (!Utils.matchesGithubRepoUrl(getArgPushUrl(arguments))) {
                System.err.println("The value of the argument pushurl is wrong.");
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

        if (launcherType == LauncherType.PIPELINE) {
            System.err.println("The environment variable " + Utils.M2_HOME + " should be set and refer to the path of your maven home installation.");
            System.err.println("For using Nopol, you must add tools.jar in your classpath from your installed jdk");
        }

        System.exit(-1);
    }

    public static List<NotifierEngine> initNotifierEngines(Logger logger) {
        List<NotifierEngine> notifierEngines = new ArrayList<>();
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        if (config.getSmtpServer() != null && config.getNotifyTo() != null) {
            logger.info("The email notifier engine will be used.");

            notifierEngines.add(new EmailNotifierEngine(config.getNotifyTo(), config.getSmtpServer(), config.getSmtpPort(), config.isSmtpTLS(), config.getSmtpUsername(), config.getSmtpPassword()));
        } else {
            logger.info("The email notifier engine won't be used.");
        }
        return notifierEngines;
    }

    public static List<NotifierEngine> initEmailSummaryEngines(Logger logger) {
        List<NotifierEngine> summaryEngines = new ArrayList<>();
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        if(config.getSmtpServer() != null && config.getSummaryFrequency() != null && config.getNotifySummary() != null) {
            logger.info("The summary email engine will be used.");
            
            summaryEngines.add(new EmailNotifierEngine(config.getNotifySummary(), config.getSmtpServer(), config.getSmtpPort(), config.isSmtpTLS(), config.getSmtpUsername(), config.getSmtpPassword()));
        } else {
            logger.info("The summary email engine will not be used.");
        }
        return summaryEngines;
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
