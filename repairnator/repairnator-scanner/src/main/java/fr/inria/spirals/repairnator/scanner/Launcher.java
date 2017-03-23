package fr.inria.spirals.repairnator.scanner;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.martiansoftware.jsap.*;
import com.martiansoftware.jsap.stringparsers.DateStringParser;
import com.martiansoftware.jsap.stringparsers.EnumeratedStringParser;
import com.martiansoftware.jsap.stringparsers.FileStringParser;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.LauncherMode;
import fr.inria.spirals.repairnator.ProcessSerializer;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.serializer.GoogleSpreadSheetFactory;
import fr.inria.spirals.repairnator.serializer.gsheet.process.GoogleSpreadSheetScannerDetailedDataSerializer;
import fr.inria.spirals.repairnator.serializer.gsheet.process.GoogleSpreadSheetScannerSerializer;
import fr.inria.spirals.repairnator.serializer.gsheet.process.GoogleSpreadSheetScannerSerializer4Bears;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.security.GeneralSecurityException;
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

        if (this.launcherMode == LauncherMode.REPAIR) {
            GoogleSpreadSheetFactory.setSpreadsheetId(GoogleSpreadSheetFactory.REPAIR_SPREADSHEET_ID);
        } else {
            GoogleSpreadSheetFactory.setSpreadsheetId(GoogleSpreadSheetFactory.BEAR_SPREADSHEET_ID);
        }

        try {
            GoogleSpreadSheetFactory.initWithFileSecret(this.arguments.getFile("googleSecretPath").getPath());
        } catch (IOException | GeneralSecurityException e) {
           LOGGER.error("Error while initializing Google Spreadsheet, no information will be serialized in spreadsheets", e);
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
        sw1.setHelp("Use it when the scanner is not used to launch the pipeline to gather more datas in spreadsheet.");
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
        opt2.setStringParser(FileStringParser.getParser());
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
        opt2.setHelp("Specify the initial date to get builds");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("lookToDate");
        opt2.setShortFlag('t');
        opt2.setLongFlag("lookToDate");
        opt2.setStringParser(dateStringParser);
        opt2.setHelp("Specify the final date to get builds");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("googleSecretPath");
        opt2.setShortFlag('g');
        opt2.setLongFlag("googleSecretPath");
        opt2.setStringParser(FileStringParser.getParser().setMustBeFile(true).setMustExist(true));
        opt2.setDefault("./client_secret.json");
        opt2.setHelp("Specify the path to the JSON google secret for serializing.");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("runId");
        opt2.setLongFlag("runId");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("Specify run id for the scanner.");
        this.jsap.registerParameter(opt2);
    }

    private List<BuildToBeInspected> runScanner() throws IOException {
        Launcher.LOGGER.info("Start to scan projects in travis...");
        ProjectScanner scanner;
        Date lookFromDate = this.arguments.getDate("lookFromDate");
        Date lookToDate = this.arguments.getDate("lookToDate");
        if (lookFromDate != null && lookToDate != null && lookFromDate.before(lookToDate)) {
            scanner = new ProjectScanner(lookFromDate, lookToDate, launcherMode, this.arguments.getString("runId"));
        } else {
            int lookupHours = this.arguments.getInt("lookupHours");
            Calendar limitCal = Calendar.getInstance();
            limitCal.add(Calendar.HOUR_OF_DAY, -lookupHours);
            lookFromDate = limitCal.getTime();
            lookToDate = new Date();
            scanner = new ProjectScanner(lookFromDate, lookToDate, launcherMode, this.arguments.getString("runId"));
        }

        List<BuildToBeInspected> buildsToBeInspected = scanner.getListOfBuildsToBeInspected(this.arguments.getFile("input").getPath());
        ProcessSerializer scannerSerializer;

        if (launcherMode == LauncherMode.REPAIR) {
            scannerSerializer = new GoogleSpreadSheetScannerSerializer(scanner);
        } else {
            scannerSerializer = new GoogleSpreadSheetScannerSerializer4Bears(scanner);

            if (this.arguments.getBoolean("scanOnly")) {
                GoogleSpreadSheetScannerDetailedDataSerializer scannerDetailedDataSerializer = new GoogleSpreadSheetScannerDetailedDataSerializer(buildsToBeInspected);
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
                Launcher.LOGGER.info("Incriminated project : " + buildToBeInspected.getBuild().getRepository().getSlug() + ":" + buildToBeInspected.getBuild().getId());
            }

            this.processOutput(buildsToBeInspected);
        } else {
            Launcher.LOGGER.warn("Builds inspected has null value.");
            System.exit(-1);
        }
    }

    private void processOutput(List<BuildToBeInspected> listOfBuilds) {
        if (this.arguments.getFile("output") != null) {
            String outputPath = this.arguments.getFile("output").getAbsolutePath();
            if (outputPath != null) {
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));

                    for (BuildToBeInspected buildToBeInspected : listOfBuilds) {
                        writer.write(buildToBeInspected.getBuild().getId() + "");
                        writer.newLine();
                        writer.flush();
                    }

                    writer.close();
                    return;
                } catch (IOException e) {
                    LOGGER.error("Error while writing file " + outputPath + ". The content will be printed in the standard output.", e);
                }
            }
        }

        for (BuildToBeInspected buildToBeInspected : listOfBuilds) {
            System.out.println(buildToBeInspected.getBuild().getId());
        }
    }

    public static void main(String[] args) throws Exception {
        Launcher launcher = new Launcher(args);
        launcher.mainProcess();
    }

}
