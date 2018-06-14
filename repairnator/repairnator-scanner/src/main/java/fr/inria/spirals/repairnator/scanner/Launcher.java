package fr.inria.spirals.repairnator.scanner;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.martiansoftware.jsap.*;
import com.martiansoftware.jsap.stringparsers.DateStringParser;
import com.martiansoftware.jsap.stringparsers.EnumeratedStringParser;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.LauncherType;
import fr.inria.spirals.repairnator.LauncherUtils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.EndProcessNotifier;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.states.BearsMode;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.spirals.repairnator.serializer.ProcessSerializer;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.serializer.ScannerDetailedDataSerializer;
import fr.inria.spirals.repairnator.serializer.ScannerSerializer;
import fr.inria.spirals.repairnator.serializer.ScannerSerializer4Bears;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by urli on 23/12/2016.
 */
public class Launcher {
    private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(Launcher.class);
    private RepairnatorConfig config;
    private List<SerializerEngine> engines;
    private EndProcessNotifier endProcessNotifier;

    public Launcher(String[] args) throws JSAPException {
        InputStream propertyStream = getClass().getResourceAsStream("/version.properties");
        Properties properties = new Properties();
        if (propertyStream != null) {
            try {
                properties.load(propertyStream);
            } catch (IOException e) {
                LOGGER.error("Error while loading property file.", e);
            }
            LOGGER.info("SCANNER VERSION: "+properties.getProperty("SCANNER_VERSION"));
        } else {
            LOGGER.info("No information about SCANNER VERSION has been found.");
        }

        JSAP jsap = this.defineArgs();
        JSAPResult arguments = jsap.parse(args);
        LauncherUtils.checkArguments(jsap, arguments, LauncherType.SCANNER);
        this.initConfig(arguments); // "this.config" is only available after this call, which initializes the config

        if (this.config.isDebug()) {
            Utils.setLoggersLevel(Level.DEBUG);
        } else {
            Utils.setLoggersLevel(Level.INFO);
        }

        this.initSerializerEngines();
        this.initNotifiers();
    }

    private JSAP defineArgs() throws JSAPException {
        // Verbose output
        JSAP jsap = new JSAP();

        // -h or --help
        jsap.registerParameter(LauncherUtils.defineArgHelp());
        // -d or --debug
        jsap.registerParameter(LauncherUtils.defineArgDebug());
        // --runId
        jsap.registerParameter(LauncherUtils.defineArgRunId());
        // --bears
        jsap.registerParameter(LauncherUtils.defineArgBearsMode());
        // -i or --input
        jsap.registerParameter(LauncherUtils.defineArgInput("Specify where to find the list of projects to scan."));
        // -o or --output
        jsap.registerParameter(LauncherUtils.defineArgOutput(LauncherType.SCANNER, "Specify where to write the list of build ids (default: stdout)"));
        // --dbhost
        jsap.registerParameter(LauncherUtils.defineArgMongoDBHost());
        // --dbname
        jsap.registerParameter(LauncherUtils.defineArgMongoDBName());
        // --notifyEndProcess
        jsap.registerParameter(LauncherUtils.defineArgNotifyEndProcess());
        // --smtpServer
        jsap.registerParameter(LauncherUtils.defineArgSmtpServer());
        // --notifyto
        jsap.registerParameter(LauncherUtils.defineArgNotifyto());
        // --ghOauth
        jsap.registerParameter(LauncherUtils.defineArgGithubOAuth());

        FlaggedOption opt2 = new FlaggedOption("lookupHours");
        opt2.setShortFlag('l');
        opt2.setLongFlag("lookupHours");
        opt2.setStringParser(JSAP.INTEGER_PARSER);
        opt2.setDefault("4");
        opt2.setHelp("Specify the hour number to lookup to get builds");
        jsap.registerParameter(opt2);

        DateStringParser dateStringParser = DateStringParser.getParser();
        dateStringParser.setProperty("format", "dd/MM/yyyy");

        opt2 = new FlaggedOption("lookFromDate");
        opt2.setShortFlag('f');
        opt2.setLongFlag("lookFromDate");
        opt2.setStringParser(dateStringParser);
        opt2.setHelp("Specify the initial date to get builds (e.g. 01/01/2017). Note that the search starts from 00:00:00 of the specified date.");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("lookToDate");
        opt2.setShortFlag('t');
        opt2.setLongFlag("lookToDate");
        opt2.setStringParser(dateStringParser);
        opt2.setHelp("Specify the final date to get builds (e.g. 31/01/2017). Note that the search is until 23:59:59 of the specified date.");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("bearsMode");
        opt2.setLongFlag("bearsMode");
        String options = StringUtils.join(BearsMode.values(), ";").toLowerCase();
        opt2.setStringParser(EnumeratedStringParser.getParser(options));
        opt2.setDefault("both");
        opt2.setHelp("This option is only useful in case of '--bears' is used: it defines the type of fixer build to get. Available values: "+options);
        jsap.registerParameter(opt2);

        Switch aSwitch = new Switch("bearsDelimiter");
        aSwitch.setLongFlag("bearsDelimiter");
        aSwitch.setHelp("This option is only useful in case of '--bears' is used and '--bearsMode both' (default) is used: it allows to" +
                        " define a delimiter to output the failing passing and then the passing passing in order to consider them separately");
        jsap.registerParameter(aSwitch);
        return jsap;
    }

    private void initConfig(JSAPResult arguments) {
        this.config = RepairnatorConfig.getInstance();

        if (LauncherUtils.getArgDebug(arguments)) {
            this.config.setDebug(true);
        }
        this.config.setRunId(LauncherUtils.getArgRunId(arguments));
        this.config.setGithubToken(LauncherUtils.getArgGithubOAuth(arguments));

        if (LauncherUtils.gerArgBearsMode(arguments)) {
            this.config.setLauncherMode(LauncherMode.BEARS);
        } else {
            this.config.setLauncherMode(LauncherMode.REPAIR);
        }
        this.config.setInputPath(LauncherUtils.getArgInput(arguments).getPath());
        if (LauncherUtils.getArgOutput(arguments) != null) {
            this.config.setSerializeJson(true);
            this.config.setOutputPath(LauncherUtils.getArgOutput(arguments).getAbsolutePath());
        }
        this.config.setMongodbHost(LauncherUtils.getArgMongoDBHost(arguments));
        this.config.setMongodbName(LauncherUtils.getArgMongoDBName(arguments));
        this.config.setNotifyEndProcess(LauncherUtils.getArgNotifyEndProcess(arguments));
        this.config.setSmtpServer(LauncherUtils.getArgSmtpServer(arguments));
        this.config.setNotifyTo(LauncherUtils.getArgNotifyto(arguments));
        Date lookFromDate = arguments.getDate("lookFromDate");
        Date lookToDate = arguments.getDate("lookToDate");
        if (lookToDate != null) {
            lookToDate = Utils.getLastTimeFromDate(lookToDate);
        }
        if (lookFromDate == null || lookToDate == null || lookFromDate.after(lookToDate)) {
            int lookupHours = arguments.getInt("lookupHours");
            Calendar limitCal = Calendar.getInstance();
            limitCal.add(Calendar.HOUR_OF_DAY, -lookupHours);
            lookFromDate = limitCal.getTime();
            lookToDate = new Date();
        }
        this.config.setLookFromDate(lookFromDate);
        this.config.setLookToDate(lookToDate);
        this.config.setBearsMode(BearsMode.valueOf(arguments.getString("bearsMode").toUpperCase()));
        this.config.setBearsDelimiter(arguments.getBoolean("bearsDelimiter"));
    }

    private void initSerializerEngines() {
        this.engines = new ArrayList<>();

        List<SerializerEngine> fileSerializerEngines = LauncherUtils.initFileSerializerEngines(LOGGER);
        this.engines.addAll(fileSerializerEngines);

        SerializerEngine mongoDBSerializerEngine = LauncherUtils.initMongoDBSerializerEngine(LOGGER);
        if (mongoDBSerializerEngine != null) {
            this.engines.add(mongoDBSerializerEngine);
        }
    }

    private void initNotifiers() {
        if (this.config.isNotifyEndProcess()) {
            List<NotifierEngine> notifierEngines = LauncherUtils.initNotifierEngines(LOGGER);
            this.endProcessNotifier = new EndProcessNotifier(notifierEngines, LauncherType.SCANNER.name().toLowerCase()+" (runid: "+this.config.getRunId()+")");
        }
    }

    private void mainProcess() throws IOException {
        LOGGER.info("Configuration: " + this.config.toString());
        Map<ScannedBuildStatus, List<BuildToBeInspected>> buildsToBeInspected = this.runScanner();

        if (buildsToBeInspected != null) {
            /*for (BuildToBeInspected buildToBeInspected : buildsToBeInspected) {
                if (this.config.getLauncherMode() == LauncherMode.REPAIR) {
                    Launcher.LOGGER.info("Incriminated project and build: " + buildToBeInspected.getBuggyBuild().getRepository().getSlug() + ":" + buildToBeInspected.getBuggyBuild().getId());
                } else {
                    Launcher.LOGGER.info("Incriminated project and pair of builds: " + buildToBeInspected.getBuggyBuild().getRepository().getSlug() + ":" + buildToBeInspected.getBuggyBuild().getId() + "" + Utils.COMMA + "" + buildToBeInspected.getPatchedBuild().getId());
                }
            }*/

            this.processOutput(buildsToBeInspected);
        } else {
            Launcher.LOGGER.warn("The variable 'builds to be inspected' has null value.");
        }
        if (this.endProcessNotifier != null) {
            this.endProcessNotifier.notifyEnd();
        }
    }

    private Map<ScannedBuildStatus, List<BuildToBeInspected>> runScanner() throws IOException {
        Launcher.LOGGER.info("Start to scan projects in Travis...");

        ProjectScanner scanner = new ProjectScanner(this.config.getLookFromDate(), this.config.getLookToDate(), this.config.getRunId());

        Map<ScannedBuildStatus, List<BuildToBeInspected>> buildsToBeInspected = scanner.getListOfBuildsToBeInspectedFromProjects(this.config.getInputPath());

        ProcessSerializer scannerSerializer;
        if (this.config.getLauncherMode() == LauncherMode.REPAIR) {
            scannerSerializer = new ScannerSerializer(this.engines, scanner);
        } else {
            scannerSerializer = new ScannerSerializer4Bears(this.engines, scanner);
            ScannerDetailedDataSerializer scannerDetailedDataSerializer = new ScannerDetailedDataSerializer(this.engines, buildsToBeInspected);
            scannerDetailedDataSerializer.serialize();
        }
        scannerSerializer.serialize();

        Launcher.LOGGER.info("---------------------------------------------------------------");
        Launcher.LOGGER.info("Scanner results.");
        Launcher.LOGGER.info("---------------------------------------------------------------");
        if (buildsToBeInspected.isEmpty()) {
            Launcher.LOGGER.info("No build interesting to be inspected has been found ("+scanner.getTotalScannedBuilds()+" scanned builds.)");
        } else {
            Launcher.LOGGER.info(buildsToBeInspected.size()+" builds interesting to be inspected have been found ("+scanner.getTotalScannedBuilds()+" scanned builds.)");
        }
        return buildsToBeInspected;
    }

    private void processOutput(Map<ScannedBuildStatus, List<BuildToBeInspected>> listOfBuilds) {
        if (this.config.isSerializeJson()) {
            this.printToFile(listOfBuilds);
        } else {
            this.printToStdout(listOfBuilds);
        }
    }

    private void printToFile(Map<ScannedBuildStatus, List<BuildToBeInspected>> listOfBuilds) {
        String outputPath = this.config.getOutputPath();
        int lastIndexOfSeparator = outputPath.lastIndexOf(File.separatorChar);
        String dirPath = outputPath.substring(0, lastIndexOfSeparator);
        String filePathExtension = outputPath.substring(lastIndexOfSeparator);

        int lastIndexOfDot = filePathExtension.lastIndexOf('.');

        String filePath, extension;
        if (lastIndexOfDot > -1) {
            filePath = dirPath + filePathExtension.substring(0, lastIndexOfDot);
            extension = filePathExtension.substring(lastIndexOfDot);
        } else {
            filePath = outputPath;
            extension = "";
        }

        try {
            BufferedWriter writer = null;

            if (this.config.getLauncherMode() != LauncherMode.BEARS || !this.config.isBearsDelimiter()) {
                writer = new BufferedWriter(new FileWriter(outputPath));
            }

            for (ScannedBuildStatus status : ScannedBuildStatus.values()) {
                if (!listOfBuilds.get(status).isEmpty()) {
                    if (this.config.getLauncherMode() == LauncherMode.BEARS && this.config.isBearsDelimiter()) {
                        String statusPath = filePath + "_" + status.name() + extension;
                        writer = new BufferedWriter(new FileWriter(statusPath));
                    }
                    for (BuildToBeInspected buildToBeInspected : listOfBuilds.get(status)) {
                        if (this.config.getLauncherMode() == LauncherMode.REPAIR) {
                            Launcher.LOGGER.info("Incriminated project and build: " + buildToBeInspected.getBuggyBuild().getRepository().getSlug() + ":" + buildToBeInspected.getBuggyBuild().getId());
                            writer.write(buildToBeInspected.getBuggyBuild().getId() + "");
                        } else {
                            Launcher.LOGGER.info("Incriminated project and pair of builds: " + buildToBeInspected.getBuggyBuild().getRepository().getSlug() + ":" + buildToBeInspected.getBuggyBuild().getId() + "" + Utils.COMMA + "" + buildToBeInspected.getPatchedBuild().getId());
                            writer.write(buildToBeInspected.getBuggyBuild().getId() + "" + Utils.COMMA + "" + buildToBeInspected.getPatchedBuild().getId());
                        }
                        writer.newLine();
                        writer.flush();
                    }
                }
            }

            writer.close();
        } catch (IOException e) {
            LOGGER.error("Error while writing file " + outputPath + ". The content will be printed in the standard output.", e);
            this.printToStdout(listOfBuilds);
        }
    }

    private void printToStdout(Map<ScannedBuildStatus, List<BuildToBeInspected>> listOfBuilds) {
        for (ScannedBuildStatus status : ScannedBuildStatus.values()) {
            if (!listOfBuilds.get(status).isEmpty()) {
                if (this.config.isBearsDelimiter()) {
                    System.out.println("[Status="+status.name()+"]");
                }
                for (BuildToBeInspected buildToBeInspected : listOfBuilds.get(status)) {
                    if (this.config.getLauncherMode() == LauncherMode.REPAIR) {
                        Launcher.LOGGER.info("Incriminated project and build: " + buildToBeInspected.getBuggyBuild().getRepository().getSlug() + ":" + buildToBeInspected.getBuggyBuild().getId());
                        System.out.println(buildToBeInspected.getBuggyBuild().getId());
                    } else {
                        Launcher.LOGGER.info("Incriminated project and pair of builds: " + buildToBeInspected.getBuggyBuild().getRepository().getSlug() + ":" + buildToBeInspected.getBuggyBuild().getId() + "" + Utils.COMMA + "" + buildToBeInspected.getPatchedBuild().getId());
                        System.out.println(buildToBeInspected.getBuggyBuild().getId() + "" + Utils.COMMA + "" + buildToBeInspected.getPatchedBuild().getId());
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Launcher launcher = new Launcher(args);
        launcher.mainProcess();
    }

}
