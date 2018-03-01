package fr.inria.spirals.repairnator.dockerpool;
import com.google.api.client.auth.oauth2.Credential;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.stringparsers.EnumeratedStringParser;
import com.martiansoftware.jsap.stringparsers.FileStringParser;
import fr.inria.spirals.repairnator.notifier.EndProcessNotifier;
import fr.inria.spirals.repairnator.notifier.engines.EmailNotifierEngine;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.dockerpool.serializer.EndProcessSerializer;
import fr.inria.spirals.repairnator.serializer.HardwareInfoSerializer;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.json.JSONFileSerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.json.MongoDBSerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.table.CSVSerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.table.GoogleSpreadsheetSerializerEngine;
import fr.inria.spirals.repairnator.serializer.gspreadsheet.GoogleSpreadSheetFactory;
import fr.inria.spirals.repairnator.serializer.gspreadsheet.ManageGoogleAccessToken;

import fr.inria.spirals.repairnator.serializer.mongodb.MongoConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by urli on 13/03/2017.
 */
public class Launcher extends AbstractPoolManager {
    private static Logger LOGGER = LoggerFactory.getLogger(Launcher.class);
    private final LauncherMode launcherMode;
    private JSAP jsap;
    private JSAPResult arguments;
    private String accessToken;
    private List<SerializerEngine> engines;
    private RepairnatorConfig config;
    private EndProcessNotifier endProcessNotifier;

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

        opt2 = new FlaggedOption("input");
        opt2.setShortFlag('i');
        opt2.setLongFlag("input");
        opt2.setStringParser(FileStringParser.getParser().setMustBeFile(true).setMustExist(true));
        opt2.setRequired(true);
        opt2.setHelp("Specify the input file containing the list of build ids.");
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

        opt2 = new FlaggedOption("globalTimeout");
        opt2.setShortFlag('g');
        opt2.setLongFlag("globalTimeout");
        opt2.setStringParser(JSAP.INTEGER_PARSER);
        opt2.setDefault("1");
        opt2.setHelp("Specify the number of day before killing the whole pool.");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("googleSecretPath");
        opt2.setShortFlag('s');
        opt2.setLongFlag("googleSecretPath");
        opt2.setStringParser(FileStringParser.getParser().setMustBeFile(true));
        opt2.setDefault("./client_secret.json");
        opt2.setHelp("Specify the path to the JSON google secret for serializing.");
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

        String launcherModeValues = "";
        for (LauncherMode mode : LauncherMode.values()) {
            launcherModeValues += mode.name() + ";";
        }
        launcherModeValues = launcherModeValues.substring(0, launcherModeValues.length() - 1);

        // Launcher mode
        opt2 = new FlaggedOption("launcherMode");
        opt2.setShortFlag('m');
        opt2.setLongFlag("launcherMode");
        opt2.setStringParser(EnumeratedStringParser.getParser(launcherModeValues));
        opt2.setRequired(true);
        opt2.setHelp("Specify if the dockerpool intends to repair failing build (REPAIR) or gather builds info (BEARS).");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("spreadsheet");
        opt2.setLongFlag("spreadsheet");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("Specify Google Spreadsheet ID to put data.");
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

    private void checkEnvironmentVariables() {
        for (String envVar : Utils.ENVIRONMENT_VARIABLES) {
            if (System.getenv(envVar) == null || System.getenv(envVar).equals("")) {
                System.err.println("You must set the following environment variable: "+envVar);
                this.printUsage();
            }
        }
    }

    private void printUsage() {
        System.err.println("Usage: java <repairnator-dockerpool name> [option(s)]");
        System.err.println();
        System.err.println("Options : ");
        System.err.println();
        System.err.println(jsap.getHelp());
        System.exit(-1);
    }

    private Map<Integer, Integer> readListOfBuildIds() {
        Map<Integer, Integer> result = new HashMap<>();
        File inputFile = this.arguments.getFile("input");

        try {
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            while (reader.ready()) {
                String line = reader.readLine().trim();
                String[] buildIds = line.split(",");
                if (buildIds.length == 1) {
                    result.put(Integer.parseInt(buildIds[0]), 0);
                } else {
                    result.put(Integer.parseInt(buildIds[0]), Integer.parseInt(buildIds[1]));
                }
            }

            reader.close();
        } catch (IOException e) {
            throw new RuntimeException("Error while reading build ids from file: "+inputFile.getPath(),e);
        }

        return result;
    }

    private void runPool() throws IOException {
        String runId = this.arguments.getString("runId");
        HardwareInfoSerializer hardwareInfoSerializer = new HardwareInfoSerializer(this.engines, runId, "dockerPool");
        hardwareInfoSerializer.serialize();

        EndProcessSerializer endProcessSerializer = new EndProcessSerializer(this.engines, runId);
        Map<Integer, Integer> buildIds = this.readListOfBuildIds();
        LOGGER.info("Find "+buildIds.size()+" builds to run.");

        endProcessSerializer.setNbBuilds(buildIds.size());

        String imageId = this.findDockerImage(this.arguments.getString("imageName"));
        LOGGER.info("Found the following docker image id: "+imageId);

        this.setDockerOutputDir(this.arguments.getString("logDirectory"));
        this.setRunId(runId);
        this.setCreateOutputDir(this.arguments.getBoolean("createOutputDir"));
        this.setSkipDelete(this.arguments.getBoolean("skipDelete"));
        this.setEngines(this.engines);

        ExecutorService executorService = Executors.newFixedThreadPool(this.arguments.getInt("threads"));

        for (Map.Entry<Integer, Integer> buildId : buildIds.entrySet()) {
            executorService.submit(this.submitBuild(imageId, buildId.getKey(), buildId.getValue()));
        }

        executorService.shutdown();
        try {
            if (executorService.awaitTermination(this.arguments.getInt("globalTimeout"), TimeUnit.DAYS)) {
                LOGGER.info("Job finished within time.");
                endProcessSerializer.setStatus("ok");
            } else {
                LOGGER.warn("Timeout launched: the job is running for one day. Force stopped "+ submittedRunnablePipelineContainers.size()+" docker container(s).");
                executorService.shutdownNow();
                this.setStatusForUnexecutedJobs();
                endProcessSerializer.setStatus("timeout");
            }
        } catch (InterruptedException e) {
            LOGGER.error("Error while await termination. Force stopped "+ submittedRunnablePipelineContainers.size()+" docker container(s).", e);
            executorService.shutdownNow();
            this.setStatusForUnexecutedJobs();
            endProcessSerializer.setStatus("interrupted");
        }

        this.getDockerClient().close();
        endProcessSerializer.serialize();
        if (this.endProcessNotifier != null) {
            this.endProcessNotifier.notifyEnd();
        }
    }

    private void setStatusForUnexecutedJobs() {
        for (RunnablePipelineContainer runnablePipelineContainer : submittedRunnablePipelineContainers) {
            runnablePipelineContainer.serialize("ABORTED");
        }
    }

    private Launcher(String[] args) throws JSAPException {
        this.defineArgs();
        this.arguments = jsap.parse(args);
        this.checkArguments();
        this.checkEnvironmentVariables();
        this.launcherMode = LauncherMode.valueOf(this.arguments.getString("launcherMode").toUpperCase());

        this.initConfig();
        this.initializeSerializerEngines();
        this.initNotifiers();
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

            this.endProcessNotifier = new EndProcessNotifier(notifierEngines, "dockerpool - (runid: "+this.config.getRunId()+")");
        }
    }

    private void initConfig() {
        this.config = RepairnatorConfig.getInstance();
        this.config.setLauncherMode(LauncherMode.valueOf(this.arguments.getString("launcherMode").toUpperCase()));

        if (this.arguments.getString("pushUrl") != null) {
            this.config.setPush(true);
            this.config.setPushRemoteRepo(this.arguments.getString("pushUrl"));
        }
        this.config.setRunId(this.arguments.getString("runId"));
        this.config.setSpreadsheetId(this.arguments.getString("spreadsheet"));
        this.config.setMongodbHost(this.arguments.getString("mongoDBHost"));
        this.config.setMongodbName(this.arguments.getString("mongoDBName"));
        this.config.setSmtpServer(this.arguments.getString("smtpServer"));
        this.config.setNotifyTo(this.arguments.getStringArray("notifyto"));
    }

    private void initializeSerializerEngines() {
        this.engines = new ArrayList<>();

        if (this.arguments.getString("spreadsheet") != null && this.arguments.getFile("googleSecretPath").exists()) {
            LOGGER.info("Initialize Google spreadsheet serializer engine.");
            GoogleSpreadSheetFactory.setSpreadsheetId(this.arguments.getString("spreadsheet"));

            try {
                GoogleSpreadSheetFactory.initWithFileSecret(this.arguments.getFile("googleSecretPath").getPath());

                ManageGoogleAccessToken manageGoogleAccessToken = ManageGoogleAccessToken.getInstance();
                Credential credential = manageGoogleAccessToken.getCredential();

                if (credential != null) {
                    this.accessToken = credential.getAccessToken();
                    this.config.setGoogleAccessToken(this.accessToken);
                }

                this.engines.add(new GoogleSpreadsheetSerializerEngine());
            } catch (IOException | GeneralSecurityException e) {
                LOGGER.error("Error while initializing Google Spreadsheet, no information will be serialized in spreadsheets", e);
            }
        } else {
            LOGGER.info("Google Spreadsheet won't be used for serialization.");
        }

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

    public static void main(String[] args) throws Exception {
        Launcher launcher = new Launcher(args);
        launcher.runPool();
    }
}
