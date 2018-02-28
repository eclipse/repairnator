package fr.inria.spirals.repairnator.scanner;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.martiansoftware.jsap.*;
import com.martiansoftware.jsap.stringparsers.DateStringParser;
import com.martiansoftware.jsap.stringparsers.EnumeratedStringParser;
import com.martiansoftware.jsap.stringparsers.FileStringParser;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.notifier.EndProcessNotifier;
import fr.inria.spirals.repairnator.notifier.engines.EmailNotifierEngine;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.spirals.repairnator.serializer.ProcessSerializer;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.json.JSONFileSerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.json.MongoDBSerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.table.CSVSerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.table.GoogleSpreadsheetSerializerEngine;
import fr.inria.spirals.repairnator.serializer.gspreadsheet.GoogleSpreadSheetFactory;
import fr.inria.spirals.repairnator.serializer.ScannerDetailedDataSerializer;
import fr.inria.spirals.repairnator.serializer.ScannerSerializer;
import fr.inria.spirals.repairnator.serializer.ScannerSerializer4Bears;
import fr.inria.spirals.repairnator.serializer.mongodb.MongoConnection;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by urli on 23/12/2016.
 */
public class Launcher {
    private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(Launcher.class);

    private JSAP jsap;
    private JSAPResult arguments;
    private LauncherMode launcherMode;
    private List<SerializerEngine> engines;
    private EndProcessNotifier endProcessNotifier;

    public Launcher(String[] args) throws JSAPException {
        this.defineArgs();
        this.arguments = jsap.parse(args);
        this.checkArguments();

        if (this.arguments.getBoolean("debug")) {
            Utils.setLoggersLevel(Level.DEBUG);
        } else {
            Utils.setLoggersLevel(Level.INFO);
        }

        this.launcherMode = LauncherMode.valueOf(this.arguments.getString("launcherMode").toUpperCase());
        this.prepareSerializerEngines();
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

            this.endProcessNotifier = new EndProcessNotifier(notifierEngines, "scanner (runid: "+this.arguments.getString("runId")+")");
        }
    }

    private void prepareSerializerEngines() {
        this.engines = new ArrayList<>();

        if (this.arguments.getString("spreadsheet") != null && this.arguments.getFile("googleSecretPath").exists()) {
            LOGGER.info("Initialize Google spreadsheet serializer engine.");
            GoogleSpreadSheetFactory.setSpreadsheetId(this.arguments.getString("spreadsheet"));

            try {
                GoogleSpreadSheetFactory.initWithFileSecret(this.arguments.getFile("googleSecretPath").getPath());
                this.engines.add(new GoogleSpreadsheetSerializerEngine());
            } catch (IOException | GeneralSecurityException e) {
                LOGGER.error("Error while initializing Google Spreadsheet, no information will be serialized in spreadsheets", e);
            }
        } else {
            LOGGER.info("Google Spreadsheet won't be used for serialization.");
        }

        if (this.arguments.getFile("output") != null) {
            LOGGER.info("Initialize files serializers engines.");
            this.engines.add(new CSVSerializerEngine(this.arguments.getFile("output").getPath()));
            this.engines.add(new JSONFileSerializerEngine(this.arguments.getFile("output").getPath()));
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

        if (!this.arguments.getString("launcherMode").equals("bears") && this.arguments.getBoolean("skip-failing")) {
            this.printUsage();
        }
    }

    private void printUsage() {
        System.err.println("Usage: java <repairnator-scanner> [option(s)]");
        System.err.println();
        System.err.println("Options : ");
        System.err.println();
        System.err.println(jsap.getHelp());
        System.exit(-1);
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

        sw1 = new Switch("scanOnly");
        sw1.setLongFlag("scan-only");
        sw1.setDefault("false");
        sw1.setHelp("Use it when the scanner is not used to launch the pipeline to gather more data.");
        this.jsap.registerParameter(sw1);

        sw1 = new Switch("skip-failing");
        sw1.setLongFlag("skip-failing");
        sw1.setDefault("false");
        sw1.setHelp("Use it when the scanner should skip failing builds (can be used only with bears mode)");
        this.jsap.registerParameter(sw1);

        sw1 = new Switch("notifyEndProcess");
        sw1.setLongFlag("notifyEndProcess");
        sw1.setDefault("false");
        sw1.setHelp("Activate the notification when the process ends.");
        this.jsap.registerParameter(sw1);

        // Tab size
        FlaggedOption opt2 = new FlaggedOption("input");
        opt2.setShortFlag('i');
        opt2.setLongFlag("input");
        opt2.setStringParser(FileStringParser.getParser().setMustExist(true).setMustBeFile(true));
        opt2.setRequired(true);
        opt2.setHelp("Specify where to find the list of projects to scan.");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("output");
        opt2.setShortFlag('o');
        opt2.setLongFlag("output");
        opt2.setStringParser(FileStringParser.getParser().setMustExist(false).setMustBeFile(true));
        opt2.setHelp("Specify where to write the list of build ids (default: stdout)");
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
        opt2.setHelp("Specify if the scanner intends to get failing build (REPAIR) or fixer builds (BEARS).");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("lookupHours");
        opt2.setShortFlag('l');
        opt2.setLongFlag("lookupHours");
        opt2.setStringParser(JSAP.INTEGER_PARSER);
        opt2.setDefault("4");
        opt2.setHelp("Specify the hour number to lookup to get builds");
        this.jsap.registerParameter(opt2);

        DateStringParser dateStringParser = DateStringParser.getParser();
        dateStringParser.setProperty("format", "dd/MM/yyyy");

        opt2 = new FlaggedOption("lookFromDate");
        opt2.setShortFlag('f');
        opt2.setLongFlag("lookFromDate");
        opt2.setStringParser(dateStringParser);
        opt2.setHelp("Specify the initial date to get builds (e.g. 01/01/2017). Note that the search starts from 00:00:00 of the specified date.");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("lookToDate");
        opt2.setShortFlag('t');
        opt2.setLongFlag("lookToDate");
        opt2.setStringParser(dateStringParser);
        opt2.setHelp("Specify the final date to get builds (e.g. 31/01/2017). Note that the search is until 23:59:59 of the specified date.");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("googleSecretPath");
        opt2.setShortFlag('g');
        opt2.setLongFlag("googleSecretPath");
        opt2.setStringParser(FileStringParser.getParser().setMustBeFile(true));
        opt2.setDefault("./client_secret.json");
        opt2.setHelp("Specify the path to the JSON google secret for serializing.");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("runId");
        opt2.setLongFlag("runId");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("Specify run id for the scanner.");
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

        opt2 = new FlaggedOption("spreadsheet");
        opt2.setLongFlag("spreadsheet");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("Specify Google Spreadsheet ID to put data.");
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

    private List<BuildToBeInspected> runScanner() throws IOException {
        Launcher.LOGGER.info("Start to scan projects in travis...");
        ProjectScanner scanner;
        Date lookFromDate = this.arguments.getDate("lookFromDate");
        Date lookToDate = this.arguments.getDate("lookToDate");
        if (lookToDate != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(lookToDate);
            calendar.set(Calendar.HOUR_OF_DAY, calendar.getMaximum(Calendar.HOUR_OF_DAY));
            calendar.set(Calendar.MINUTE, calendar.getMaximum(Calendar.MINUTE));
            calendar.set(Calendar.SECOND, calendar.getMaximum(Calendar.SECOND));
            calendar.set(Calendar.MILLISECOND, calendar.getMaximum(Calendar.MILLISECOND));
            lookToDate = calendar.getTime();
        }
        if (lookFromDate != null && lookToDate != null && lookFromDate.before(lookToDate)) {
            scanner = new ProjectScanner(lookFromDate, lookToDate, launcherMode, this.arguments.getString("runId"), this.arguments.getBoolean("skip-failing"));
        } else {
            int lookupHours = this.arguments.getInt("lookupHours");
            Calendar limitCal = Calendar.getInstance();
            limitCal.add(Calendar.HOUR_OF_DAY, -lookupHours);
            lookFromDate = limitCal.getTime();
            lookToDate = new Date();
            scanner = new ProjectScanner(lookFromDate, lookToDate, launcherMode, this.arguments.getString("runId"), this.arguments.getBoolean("skip-failing"));
        }

        List<BuildToBeInspected> buildsToBeInspected = scanner.getListOfBuildsToBeInspected(this.arguments.getFile("input").getPath());
        ProcessSerializer scannerSerializer;

        if (launcherMode == LauncherMode.REPAIR) {
            scannerSerializer = new ScannerSerializer(this.engines, scanner);
        } else {
            scannerSerializer = new ScannerSerializer4Bears(this.engines, scanner);

            if (this.arguments.getBoolean("scanOnly")) {
                ScannerDetailedDataSerializer scannerDetailedDataSerializer = new ScannerDetailedDataSerializer(this.engines, buildsToBeInspected);
                scannerDetailedDataSerializer.serialize();
            }
        }

        scannerSerializer.serialize();

        if (buildsToBeInspected.isEmpty()) {
            Launcher.LOGGER.info("No build has been found ("+scanner.getTotalScannedBuilds()+" scanned builds.)");
        }
        return buildsToBeInspected;
    }

    private void mainProcess() throws IOException {

        List<BuildToBeInspected> buildsToBeInspected = this.runScanner();

        if (buildsToBeInspected != null) {
            for (BuildToBeInspected buildToBeInspected : buildsToBeInspected) {
                Launcher.LOGGER.info("Incriminated project : " + buildToBeInspected.getBuggyBuild().getRepository().getSlug() + ":" + buildToBeInspected.getBuggyBuild().getId());
            }

            this.processOutput(buildsToBeInspected);
        } else {
            Launcher.LOGGER.warn("Builds inspected has null value.");
        }
        if (this.endProcessNotifier != null) {
            this.endProcessNotifier.notifyEnd();
        }
    }

    private void processOutput(List<BuildToBeInspected> listOfBuilds) {
        if (this.arguments.getFile("output") != null) {
            String outputPath = this.arguments.getFile("output").getAbsolutePath();
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));

                for (BuildToBeInspected buildToBeInspected : listOfBuilds) {
                    writer.write(buildToBeInspected.getBuggyBuild().getId() + "");
                    writer.newLine();
                    writer.flush();
                }

                writer.close();
                return;
            } catch (IOException e) {
                LOGGER.error("Error while writing file " + outputPath + ". The content will be printed in the standard output.", e);
            }
        }

        for (BuildToBeInspected buildToBeInspected : listOfBuilds) {
            System.out.println(buildToBeInspected.getBuggyBuild().getId());
        }
    }

    public static void main(String[] args) throws Exception {
        Launcher launcher = new Launcher(args);
        launcher.mainProcess();
    }

}
