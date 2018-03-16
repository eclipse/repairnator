package fr.inria.spirals.repairnator.scanner;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.martiansoftware.jsap.*;
import com.martiansoftware.jsap.stringparsers.DateStringParser;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.LauncherType;
import fr.inria.spirals.repairnator.LauncherUtils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.EndProcessNotifier;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.spirals.repairnator.serializer.ProcessSerializer;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.serializer.ScannerDetailedDataSerializer;
import fr.inria.spirals.repairnator.serializer.ScannerSerializer;
import fr.inria.spirals.repairnator.serializer.ScannerSerializer4Bears;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
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
    private RepairnatorConfig config;
    private List<SerializerEngine> engines;
    private EndProcessNotifier endProcessNotifier;

    public Launcher(String[] args) throws JSAPException {
        this.defineArgs();
        this.arguments = jsap.parse(args);
        LauncherUtils.checkArguments(this.jsap, this.arguments, LauncherType.SCANNER);

        if (LauncherUtils.getArgDebug(this.arguments)) {
            Utils.setLoggersLevel(Level.DEBUG);
        } else {
            Utils.setLoggersLevel(Level.INFO);
        }

        this.initConfig();
        this.initSerializerEngines();
        this.initNotifiers();
    }

    private void defineArgs() throws JSAPException {
        // Verbose output
        this.jsap = new JSAP();

        // -h or --help
        this.jsap.registerParameter(LauncherUtils.defineArgHelp());
        // -d or --debug
        this.jsap.registerParameter(LauncherUtils.defineArgDebug());
        // --runId
        this.jsap.registerParameter(LauncherUtils.defineArgRunId());
        // -m or --launcherMode
        this.jsap.registerParameter(LauncherUtils.defineArgLauncherMode("Specify if the scanner intends to get failing builds (REPAIR) or fixer builds (BEARS)."));
        // -i or --input
        this.jsap.registerParameter(LauncherUtils.defineArgInput("Specify where to find the list of projects to scan."));
        // -o or --output
        this.jsap.registerParameter(LauncherUtils.defineArgOutput(LauncherType.SCANNER, "Specify where to write the list of build ids (default: stdout)"));
        // --dbhost
        this.jsap.registerParameter(LauncherUtils.defineArgMongoDBHost());
        // --dbname
        this.jsap.registerParameter(LauncherUtils.defineArgMongoDBName());
        // --spreadsheet
        this.jsap.registerParameter(LauncherUtils.defineArgSpreadsheetId());
        // --googleSecretPath
        this.jsap.registerParameter(LauncherUtils.defineArgGoogleSecretPath());
        // --notifyEndProcess
        this.jsap.registerParameter(LauncherUtils.defineArgNotifyEndProcess());
        // --smtpServer
        this.jsap.registerParameter(LauncherUtils.defineArgSmtpServer());
        // --notifyto
        this.jsap.registerParameter(LauncherUtils.defineArgNotifyto());

        Switch sw1 = new Switch("skip-failing");
        sw1.setLongFlag("skip-failing");
        sw1.setDefault("false");
        sw1.setHelp("Use it when the scanner should skip failing builds (can be used only with bears mode)");
        this.jsap.registerParameter(sw1);

        FlaggedOption opt2 = new FlaggedOption("lookupHours");
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
    }

    private void initConfig() {
        this.config = RepairnatorConfig.getInstance();

        this.config.setRunId(LauncherUtils.getArgRunId(this.arguments));
        this.config.setLauncherMode(LauncherUtils.getArgLauncherMode(this.arguments));
        this.config.setMongodbHost(LauncherUtils.getArgMongoDBHost(this.arguments));
        this.config.setMongodbName(LauncherUtils.getArgMongoDBName(this.arguments));
        this.config.setSpreadsheetId(LauncherUtils.getArgSpreadsheetId(this.arguments));
        this.config.setSmtpServer(LauncherUtils.getArgSmtpServer(this.arguments));
        this.config.setNotifyTo(LauncherUtils.getArgNotifyto(this.arguments));
    }

    private void initSerializerEngines() {
        this.engines = new ArrayList<>();

        SerializerEngine spreadsheetSerializerEngine = LauncherUtils.initSpreadsheetSerializerEngineWithFileSecret(this.arguments, LOGGER);
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

    private void initNotifiers() {
        if (LauncherUtils.getArgNotifyEndProcess(this.arguments)) {
            List<NotifierEngine> notifierEngines = LauncherUtils.initNotifierEngines(this.arguments, LOGGER);
            this.endProcessNotifier = new EndProcessNotifier(notifierEngines, LauncherType.SCANNER.name().toLowerCase()+" (runid: "+this.config.getRunId()+")");
        }
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

    private List<BuildToBeInspected> runScanner() throws IOException {
        Launcher.LOGGER.info("Start to scan projects in travis...");
        ProjectScanner scanner;
        Date lookFromDate = this.arguments.getDate("lookFromDate");
        Date lookToDate = this.arguments.getDate("lookToDate");
        if (lookToDate != null) {
            lookToDate = Utils.getLastTimeFromDate(lookToDate);
        }
        if (lookFromDate != null && lookToDate != null && lookFromDate.before(lookToDate)) {
            scanner = new ProjectScanner(lookFromDate, lookToDate, this.config.getRunId(), this.arguments.getBoolean("skip-failing"));
        } else {
            int lookupHours = this.arguments.getInt("lookupHours");
            Calendar limitCal = Calendar.getInstance();
            limitCal.add(Calendar.HOUR_OF_DAY, -lookupHours);
            lookFromDate = limitCal.getTime();
            lookToDate = new Date();
            scanner = new ProjectScanner(lookFromDate, lookToDate, this.config.getRunId(), this.arguments.getBoolean("skip-failing"));
        }

        List<BuildToBeInspected> buildsToBeInspected = scanner.getListOfBuildsToBeInspected(LauncherUtils.getArgInput(this.arguments).getPath());
        ProcessSerializer scannerSerializer;

        if (this.config.getLauncherMode() == LauncherMode.REPAIR) {
            scannerSerializer = new ScannerSerializer(this.engines, scanner);
        } else {
            scannerSerializer = new ScannerSerializer4Bears(this.engines, scanner);
            ScannerDetailedDataSerializer scannerDetailedDataSerializer = new ScannerDetailedDataSerializer(this.engines, buildsToBeInspected);
            scannerDetailedDataSerializer.serialize();
        }

        scannerSerializer.serialize();

        if (buildsToBeInspected.isEmpty()) {
            Launcher.LOGGER.info("No build has been found ("+scanner.getTotalScannedBuilds()+" scanned builds.)");
        }
        return buildsToBeInspected;
    }

    private void processOutput(List<BuildToBeInspected> listOfBuilds) {
        if (LauncherUtils.getArgOutput(this.arguments) != null) {
            String outputPath = LauncherUtils.getArgOutput(this.arguments).getAbsolutePath();
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
