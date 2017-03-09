package fr.inria.spirals.repairnator;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.martiansoftware.jsap.*;
import com.martiansoftware.jsap.stringparsers.EnumeratedStringParser;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.config.RepairnatorConfigException;
import fr.inria.spirals.repairnator.scanner.ProjectScanner;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector4Bears;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.serializer.csv.CSVSerializer4Bears;
import fr.inria.spirals.repairnator.serializer.csv.CSVSerializer4RepairNator;
import fr.inria.spirals.repairnator.serializer.gsheet.inspectors.GoogleSpreadSheetInspectorSerializer;
import fr.inria.spirals.repairnator.serializer.gsheet.inspectors.GoogleSpreadSheetInspectorSerializer4Bears;
import fr.inria.spirals.repairnator.serializer.gsheet.inspectors.GoogleSpreadSheetInspectorTrackTreatedBuilds;
import fr.inria.spirals.repairnator.serializer.gsheet.inspectors.GoogleSpreadSheetNopolSerializer;
import fr.inria.spirals.repairnator.serializer.gsheet.process.GoogleSpreadSheetEndProcessSerializer;
import fr.inria.spirals.repairnator.serializer.gsheet.process.GoogleSpreadSheetEndProcessSerializer4Bears;
import fr.inria.spirals.repairnator.serializer.gsheet.process.GoogleSpreadSheetScannerSerializer;
import fr.inria.spirals.repairnator.serializer.gsheet.process.GoogleSpreadSheetScannerSerializer4Bears;
import fr.inria.spirals.repairnator.serializer.json.JsonSerializer;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by urli on 23/12/2016.
 */
public class Launcher {
    private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(Launcher.class);

    private static final String[] ENVIRONMENT_VARIABLES = new String[] { "M2_HOME", "GITHUB_OAUTH", "GITHUB_LOGIN" };
    private static final int NB_THREADS = 4;

    private JSAP jsap;
    private JSAPResult arguments;
    private RepairnatorConfig config;
    private ProjectScanner scanner;
    private List<ProjectInspector> projectInspectors;

    public Launcher(String[] args) throws JSAPException {
        this.defineArgs();
        this.arguments = jsap.parse(args);
        this.checkArguments();
        this.checkEnvironmentVariables();
        try {
            this.config = RepairnatorConfig.getInstance();
        } catch (RepairnatorConfigException e) {
            System.err.println(e.getMessage());
            System.err.println(e.getCause());
            this.printUsage();
        }

        this.config.setLauncherMode(LauncherMode.valueOf(this.arguments.getString("launcherMode").toUpperCase()));
        this.config.setInputFile(this.arguments.getString("input"));

        if (this.config.getLauncherMode() == LauncherMode.REPAIR) {
            this.checkToolsLoaded();
            this.checkNopolSolverPath();
        }

        if (this.arguments.getBoolean("debug")) {
            this.setLevel(Level.DEBUG);
        } else {
            this.setLevel(Level.INFO);
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
        System.err.println("Usage: java <repairnator name> [option(s)]");
        System.err.println();
        System.err.println("Options : ");
        System.err.println();
        System.err.println(jsap.getHelp());
        System.err.println("Please note that the following environment variables must be set: ");
        for (String env : Launcher.ENVIRONMENT_VARIABLES) {
            System.err.println(" - " + env);
        }
        System.err.println("For using Nopol, you must add tools.jar in your classpath from your installed jdk");
        System.exit(-1);
    }

    private void checkEnvironmentVariables() {
        for (String envVar : Launcher.ENVIRONMENT_VARIABLES) {
            if (System.getenv(envVar) == null) {
                this.printUsage();
            }
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

        // Tab size
        FlaggedOption opt2 = new FlaggedOption("input");
        opt2.setShortFlag('i');
        opt2.setLongFlag("input");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setRequired(true);
        opt2.setHelp("Specify where to find the list of projects or build ids to scan.");
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
    }

    private static void setLevel(Level level) {
        Logger jtravis = (Logger) LoggerFactory.getLogger("fr.inria.spirals.jtravis.helpers");
        jtravis.setLevel(level);

        Logger nopol = (Logger) LoggerFactory.getLogger("fr.inria.lille.repair.nopol");
        nopol.setLevel(level);

        Logger repairnator = (Logger) LoggerFactory.getLogger("fr.inria.spirals.repairnator");
        repairnator.setLevel(level);

        Logger jgit = (Logger) LoggerFactory.getLogger("org.eclipse.jgit");
        jgit.setLevel(Level.WARN);
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

    private static void initWorkspace(String path) throws IOException {
        File file = new File(path);

        if (file.exists()) {
            throw new IOException("The following directory already exists: " + path + ". Please choose an empty directory.");
        }
    }

    private List<BuildToBeInspected> runScanner() throws IOException {
        Launcher.LOGGER.info("Start to scan projects in travis...");

        this.scanner = new ProjectScanner(this.config.getLookupHours(), this.config.getLauncherMode(), this.config.getFileMode());
        List<BuildToBeInspected> buildsToBeInspected = this.scanner.getListOfBuildsToBeInspected(this.config.getInputFile());

        if (this.config.getFileMode() == FileMode.SLUG) {
            ProcessSerializer scannerSerializer;

            if (this.config.getLauncherMode() == LauncherMode.REPAIR) {
                scannerSerializer = new GoogleSpreadSheetScannerSerializer(this.scanner, this.config.getGoogleSecretPath());
            } else {
                scannerSerializer = new GoogleSpreadSheetScannerSerializer4Bears(this.scanner, this.config.getGoogleSecretPath());
            }

            scannerSerializer.serialize();
        }

        return buildsToBeInspected;
    }

    private void mainProcess() throws IOException {

        Launcher.LOGGER.info("Launching with the following configuration: "+this.config);

        SimpleDateFormat dateFormat = new SimpleDateFormat("YYYYMMdd_HHmmss");
        String completeWorkspace = this.config.getWorkspacePath() + File.separator + dateFormat.format(new Date());
        this.initWorkspace(completeWorkspace);

        this.config.setWorkspacePath(completeWorkspace);

        List<BuildToBeInspected> buildsToBeInspected = this.runScanner();

        if (buildsToBeInspected != null) {
            for (BuildToBeInspected buildToBeInspected : buildsToBeInspected) {
                Launcher.LOGGER.info("Incriminated project : " + buildToBeInspected.getBuild().getRepository().getSlug() + ":" + buildToBeInspected.getBuild().getId());
            }

            this.runInspectors(buildsToBeInspected);
        } else {
            Launcher.LOGGER.warn("Builds inspected has null value.");
            System.exit(-1);
        }
    }

    private void runInspectors(List<BuildToBeInspected> buildsToBeInspected) throws IOException {
        // init serializers
        List<AbstractDataSerializer> serializers = new ArrayList<>();

        if (this.config.getLauncherMode() == LauncherMode.REPAIR) {
            GoogleSpreadSheetFactory.setSpreadsheetId(GoogleSpreadSheetFactory.REPAIR_SPREADSHEET_ID);

            serializers.add(new CSVSerializer4RepairNator(this.config.getJsonOutputPath()));
            serializers.add(new GoogleSpreadSheetInspectorSerializer(this.config.getGoogleSecretPath()));
            serializers.add(new GoogleSpreadSheetNopolSerializer(this.config.getGoogleSecretPath()));
        } else {
            GoogleSpreadSheetFactory.setSpreadsheetId(GoogleSpreadSheetFactory.BEAR_SPREADSHEET_ID);

            serializers.add(new CSVSerializer4Bears(this.config.getJsonOutputPath()));
            serializers.add(new GoogleSpreadSheetInspectorSerializer4Bears(this.config.getGoogleSecretPath()));
        }

        JsonSerializer jsonSerializer = new JsonSerializer(this.config.getJsonOutputPath(), this.config.getLauncherMode(), this.config.getFileMode());

        if (this.config.isSerializeJson()) {
            serializers.add(jsonSerializer);
        }

        if (this.config.getFileMode() == FileMode.SLUG) {
            serializers.add(new GoogleSpreadSheetInspectorTrackTreatedBuilds(buildsToBeInspected, this.config.getGoogleSecretPath()));
        }

        Launcher.LOGGER.info("Start cloning and compiling projects...");

        this.projectInspectors = new ArrayList<ProjectInspector>();

        for (BuildToBeInspected buildToBeInspected : buildsToBeInspected) {
            ProjectInspector inspector;

            if (config.getLauncherMode() == LauncherMode.BEARS) {
                inspector = new ProjectInspector4Bears(buildToBeInspected, this.config.getWorkspacePath(), serializers);
            } else {
                inspector = new ProjectInspector(buildToBeInspected, this.config.getWorkspacePath(), serializers);
            }
            projectInspectors.add(inspector);
        }

        final ExecutorService pool = Executors.newFixedThreadPool(NB_THREADS);

        for (final ProjectInspector inspector : projectInspectors) {
            pool.submit(new Runnable() {
                @Override
                public void run() {
                    inspector.run();
                }
            });
        }

        try {
            pool.shutdown();
            if (!pool.awaitTermination(1, TimeUnit.DAYS)) {
                pool.shutdownNow();
                LOGGER.error("Shutdown pool of threads.");
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            LOGGER.error(e.getMessage(), e);
        }

        this.serializeEndProcess();

        if (this.config.isSerializeJson()) {
            Launcher.LOGGER.info("Start writing a JSON output...");
            jsonSerializer.setScanner(scanner);
            jsonSerializer.createOutput();
        }


        if (this.config.isClean()) {
            Launcher.LOGGER.info("Clean the workspace now...");
            FileUtils.deleteDirectory(this.config.getWorkspacePath());
        }

        // to be sure the process is finished
        System.exit(0);
    }

    private void serializeEndProcess() throws IOException {
        if (this.config.getFileMode() == FileMode.SLUG) {
            Launcher.LOGGER.info("Serialize end process...");
            if (this.config.getLauncherMode() == LauncherMode.REPAIR) {
                int nbReproducedFails = 0;
                int nbReproducedErrors = 0;

                for (ProjectInspector inspector : this.projectInspectors) {
                    if (inspector.isReproducedAsFail()) {
                        nbReproducedFails++;
                    }
                    if (inspector.isReproducedAsError()) {
                        nbReproducedErrors++;
                    }
                }

                GoogleSpreadSheetEndProcessSerializer googleSpreadSheetEndProcessSerializer = new GoogleSpreadSheetEndProcessSerializer(scanner, this.config.getGoogleSecretPath());
                googleSpreadSheetEndProcessSerializer.setReproducedFailures(nbReproducedFails);
                googleSpreadSheetEndProcessSerializer.setReproducedErrors(nbReproducedErrors);
                googleSpreadSheetEndProcessSerializer.serialize();
            } else {
                int nbFixerBuildCase1 = 0;
                int nbFixerBuildCase2 = 0;

                for (ProjectInspector inspector : projectInspectors) {
                    ProjectInspector4Bears inspector4Bears = (ProjectInspector4Bears)inspector;
                    if (inspector4Bears.isFixerBuildCase1()) {
                        nbFixerBuildCase1++;
                    }
                    if (inspector4Bears.isFixerBuildCase2()) {
                        nbFixerBuildCase2++;
                    }
                }

                GoogleSpreadSheetEndProcessSerializer4Bears googleSpreadSheetEndProcessSerializer4Bears = new GoogleSpreadSheetEndProcessSerializer4Bears(scanner, this.config.getGoogleSecretPath());
                googleSpreadSheetEndProcessSerializer4Bears.setNbFixerBuildCase1(nbFixerBuildCase1);
                googleSpreadSheetEndProcessSerializer4Bears.setNbFixerBuildCase2(nbFixerBuildCase2);
                googleSpreadSheetEndProcessSerializer4Bears.serialize();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Launcher launcher = new Launcher(args);
        launcher.mainProcess();
    }

}
