package fr.inria.spirals.repairnator;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.martiansoftware.jsap.*;
import com.martiansoftware.jsap.stringparsers.EnumeratedStringParser;
import fr.inria.spirals.repairnator.scanner.ProjectScanner;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector4Bears;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.serializer.csv.CSVSerializer4Bears;
import fr.inria.spirals.repairnator.serializer.csv.CSVSerializer4RepairNator;
import fr.inria.spirals.repairnator.serializer.gsheet.inspectors.GoogleSpreadSheetInspectorSerializer;
import fr.inria.spirals.repairnator.serializer.gsheet.inspectors.GoogleSpreadSheetInspectorSerializer4Bears;
import fr.inria.spirals.repairnator.serializer.gsheet.inspectors.GoogleSpreadSheetInspectorTimeSerializer;
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

    private static final String[] ENVIRONMENT_VARIABLES = new String[] { "M2_HOME", "GITHUB_OAUTH" };
    private static final int NB_THREADS = 4;

    private JSAP jsap;
    private JSAPResult arguments;
    private List<AbstractDataSerializer> serializers;

    private boolean debug;
    private String input;
    private LauncherMode launcherMode;
    private FileMode fileMode;
    private String output;
    private String workspace;
    private int lookupHours;
    private boolean push;
    private boolean clean;
    private String googleSecretPath;
    private String solverPath;

    public Launcher() {
        this.serializers = new ArrayList<AbstractDataSerializer>();
    }

    private void defineArgs() throws JSAPException {
        // Verbose output
        jsap = new JSAP();

        // help
        Switch sw1 = new Switch("help");
        sw1.setShortFlag('h');
        sw1.setLongFlag("help");
        sw1.setDefault("false");
        jsap.registerParameter(sw1);

        // verbosity
        sw1 = new Switch("debug");
        sw1.setShortFlag('d');
        sw1.setLongFlag("debug");
        sw1.setDefault("false");
        jsap.registerParameter(sw1);

        // Tab size
        FlaggedOption opt2 = new FlaggedOption("input");
        opt2.setShortFlag('i');
        opt2.setLongFlag("input");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setRequired(true);
        opt2.setHelp("Specify where to find the list of projects or build ids to scan.");
        jsap.registerParameter(opt2);

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
        opt2.setHelp("Specify if RepairNator will be launch for repairing (REPAIRNATOR) or for collecting fixer builds (BEARS).");
        jsap.registerParameter(opt2);

        String fileModeValues = "";
        for (FileMode mode : FileMode.values()) {
            fileModeValues += mode.name() + ";";
        }
        fileModeValues.substring(0, fileModeValues.length() - 2);

        // File mode
        opt2 = new FlaggedOption("fileMode");
        opt2.setShortFlag('f');
        opt2.setLongFlag("fileMode");
        opt2.setStringParser(EnumeratedStringParser.getParser(fileModeValues));
        opt2.setRequired(true);
        opt2.setHelp("Specify if the input contains project names (SLUG) or build ids (BUILD).");
        jsap.registerParameter(opt2);

        // output directory
        opt2 = new FlaggedOption("output");
        opt2.setShortFlag('o');
        opt2.setLongFlag("output");
        opt2.setHelp("Specify where to place JSON output.");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setRequired(true);
        jsap.registerParameter(opt2);

        // Workspace to clone repo
        opt2 = new FlaggedOption("workspace");
        opt2.setShortFlag('w');
        opt2.setLongFlag("workspace");
        opt2.setRequired(true);
        opt2.setHelp("Specify where to clone failing repository.");
        opt2.setDefault("./workspace");
        opt2.setStringParser(JSAP.STRING_PARSER);
        jsap.registerParameter(opt2);

        // Number of day to consider for retrieving builds
        opt2 = new FlaggedOption("lookupHours");
        opt2.setShortFlag('l');
        opt2.setLongFlag("lookupHours");
        opt2.setRequired(true);
        opt2.setHelp("Specify the number of hours to lookup in past for builds.");
        opt2.setDefault("1");
        opt2.setStringParser(JSAP.INTEGER_PARSER);
        jsap.registerParameter(opt2);

        // pushing builds
        sw1 = new Switch("push");
        sw1.setShortFlag('p');
        sw1.setLongFlag("push");
        sw1.setHelp("If set this flag push failing builds (bypass push even in conjunction with steps option).");
        jsap.registerParameter(sw1);

        // cleaning
        sw1 = new Switch("clean");
        sw1.setLongFlag("clean");
        sw1.setHelp("Clean workspace after each finished process.");
        jsap.registerParameter(sw1);

        // Google secret path
        opt2 = new FlaggedOption("googleSecretPath");
        opt2.setShortFlag('g');
        opt2.setLongFlag("googleSecretPath");
        opt2.setHelp("Specify the path of the google client secret file.");
        opt2.setDefault("./client_secret.json");
        opt2.setStringParser(JSAP.STRING_PARSER);
        jsap.registerParameter(opt2);

        // Solver path
        opt2 = new FlaggedOption("z3Path");
        opt2.setShortFlag('z');
        opt2.setLongFlag("z3Path");
        opt2.setHelp("Specify the solver path used by Nopol.");
        opt2.setStringParser(JSAP.STRING_PARSER);
        jsap.registerParameter(opt2);
    }

    private static void setLevel(Level level) {
        Logger jtravis = (Logger) LoggerFactory.getLogger("fr.inria.spirals.jtravis.helpers");
        jtravis.setLevel(Level.DEBUG);

        Logger nopol = (Logger) LoggerFactory.getLogger("fr.inria.lille.repair.nopol");
        nopol.setLevel(Level.DEBUG);

        Logger repairnator = (Logger) LoggerFactory.getLogger("fr.inria.spirals.repairnator");
        repairnator.setLevel(Level.DEBUG);

        Logger jgit = (Logger) LoggerFactory.getLogger("org.eclipse.jgit");
        jgit.setLevel(Level.WARN);
    }

    private static void initWorkspace(String path) throws IOException {
        File file = new File(path);

        if (file.exists()) {
            throw new IOException("The following directory already exists: " + path + ". Please choose an empty directory.");
        }
    }

    private boolean checkEnvironmentVariables() {
        for (String envVar : Launcher.ENVIRONMENT_VARIABLES) {
            if (System.getenv(envVar) == null) {
                return false;
            }
        }
        return true;
    }

    private void checkToolsLoaded() {
        URLClassLoader loader;

        try {
            loader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            loader.loadClass("com.sun.jdi.AbsentInformationException");
        } catch (ClassNotFoundException e) {
            System.err.println("Tools.jar must be loaded, here the classpath given for your app: "
                    + System.getProperty("java.class.path"));
            System.exit(-1);
        }
    }

    private void checkNopolSolverPath() {
        this.solverPath = this.arguments.getString("z3Path");

        if (this.solverPath != null) {
            File file = new File(this.solverPath);

            if (!file.exists()) {
                System.err.println("The Nopol solver path should be an existing file: " + file.getPath() + " does not exist.");
                System.exit(-1);
            }
        } else {
            System.err.println("The Nopol solver path should be provided.");
            System.exit(-1);
        }
    }

    private void run(String[] args) throws JSAPException, IOException {
        this.defineArgs();
        this.arguments = jsap.parse(args);

        if (!this.arguments.success()) {
            // print out specific error messages describing the problems
            for (java.util.Iterator<?> errs = arguments.getErrorMessageIterator(); errs.hasNext();) {
                System.err.println("Error: " + errs.next());
            }
        }
        if (!this.arguments.success() || !checkEnvironmentVariables() || this.arguments.getBoolean("help")) {
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
        this.checkToolsLoaded();
        this.launcherMode = LauncherMode.valueOf(this.arguments.getString("launcherMode").toUpperCase());
        if (this.launcherMode == LauncherMode.REPAIRNATOR) {
            this.checkNopolSolverPath();
        }
        if (this.arguments.success() && checkEnvironmentVariables()) {
            System.out.println(arguments.toString());
            mainProcess();
        }
    }

    private void mainProcess() throws IOException {
        this.debug = this.arguments.getBoolean("debug");
        this.input = this.arguments.getString("input");
        this.fileMode = FileMode.valueOf(this.arguments.getString("fileMode").toUpperCase());
        this.output = this.arguments.getString("output");
        this.workspace = this.arguments.getString("workspace");
        this.lookupHours = this.arguments.getInt("lookupHours");
        this.push = this.arguments.getBoolean("push");
        this.clean = this.arguments.getBoolean("clean");
        this.googleSecretPath = this.arguments.getString("googleSecretPath");

        if (this.debug) {
            setLevel(Level.DEBUG);
        }

        Launcher.LOGGER.debug("Start to scan projects in travis...");

        ProjectScanner scanner = new ProjectScanner(this.lookupHours);

        JsonSerializer jsonSerializer = new JsonSerializer(this.output, this.launcherMode, this.fileMode);
        jsonSerializer.setScanner(scanner);

        AbstractDataSerializer csvSerializer;
        AbstractDataSerializer googleSpreadSheetInspectorSerializer;
        GoogleSpreadSheetEndProcessSerializer googleSpreadSheetEndProcessSerializer = null;
        GoogleSpreadSheetEndProcessSerializer4Bears googleSpreadSheetEndProcessSerializer4Bears = null;
        if (this.launcherMode == LauncherMode.REPAIRNATOR) {
            csvSerializer = new CSVSerializer4RepairNator(this.output);
            googleSpreadSheetInspectorSerializer = new GoogleSpreadSheetInspectorSerializer(this.googleSecretPath);
            if (this.fileMode == FileMode.SLUG) {
                googleSpreadSheetEndProcessSerializer = new GoogleSpreadSheetEndProcessSerializer(scanner, this.googleSecretPath);
            }
            this.serializers.add(new GoogleSpreadSheetNopolSerializer(this.googleSecretPath));
        } else {
            csvSerializer = new CSVSerializer4Bears(this.output);
            googleSpreadSheetInspectorSerializer = new GoogleSpreadSheetInspectorSerializer4Bears(this.googleSecretPath);
            if (this.fileMode == FileMode.SLUG) {
                googleSpreadSheetEndProcessSerializer4Bears = new GoogleSpreadSheetEndProcessSerializer4Bears(scanner, this.googleSecretPath);
            }
            GoogleSpreadSheetFactory.setSpreadsheetId(GoogleSpreadSheetFactory.BEAR_SPREADSHEET_ID);
        }
        GoogleSpreadSheetInspectorTimeSerializer googleSpreadSheetInspectorTimeSerializer = new GoogleSpreadSheetInspectorTimeSerializer(this.googleSecretPath);

        this.serializers.add(jsonSerializer);
        this.serializers.add(csvSerializer);
        this.serializers.add(googleSpreadSheetInspectorSerializer);
        this.serializers.add(googleSpreadSheetInspectorTimeSerializer);

        List<BuildToBeInspected> buildsToBeInspected = null;

        String completeWorkspace = null;

        if (this.launcherMode == LauncherMode.REPAIRNATOR) {
            if (this.fileMode == FileMode.SLUG) {
                buildsToBeInspected = scanner.getListOfFailingBuildsFromProjects(this.input, this.launcherMode);
            } else {
                buildsToBeInspected = scanner.getListOfFailingBuildsFromGivenBuildIds(this.input, this.launcherMode);
            }
        } else {
            if (this.fileMode == FileMode.SLUG) {
                buildsToBeInspected = scanner.getListOfPassingBuildsFromProjects(this.input, this.launcherMode);
            } else {
                buildsToBeInspected = scanner.getListOfPassingBuildsFromGivenBuildIds(this.input, this.launcherMode);
            }
        }

        if (this.launcherMode == LauncherMode.REPAIRNATOR && this.fileMode == FileMode.SLUG) {
            GoogleSpreadSheetScannerSerializer scannerSerializer = new GoogleSpreadSheetScannerSerializer(scanner, this.googleSecretPath);
            scannerSerializer.serialize();
        }
        if (this.launcherMode == LauncherMode.BEARS && this.fileMode == FileMode.SLUG) {
            GoogleSpreadSheetScannerSerializer4Bears scannerSerializer = new GoogleSpreadSheetScannerSerializer4Bears(scanner, this.googleSecretPath);
            scannerSerializer.serialize();
        }

        if (buildsToBeInspected != null) {
            for (BuildToBeInspected buildToBeInspected : buildsToBeInspected) {
                System.out.println("Incriminated project : " + buildToBeInspected.getBuild().getRepository().getSlug() + ":" + buildToBeInspected.getBuild().getId());
            }

            if (this.fileMode == FileMode.SLUG) {
                GoogleSpreadSheetInspectorTrackTreatedBuilds googleSpreadSheetInspectorTrackTreatedBuilds = new GoogleSpreadSheetInspectorTrackTreatedBuilds(buildsToBeInspected, this.googleSecretPath);
                this.serializers.add(googleSpreadSheetInspectorTrackTreatedBuilds);
            }
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("YYYYMMdd_HHmmss");
        completeWorkspace = workspace + File.separator + dateFormat.format(new Date());

        if (completeWorkspace != null) {
            if (this.launcherMode == LauncherMode.REPAIRNATOR) {
                List<ProjectInspector> inspectors = runInspectors(buildsToBeInspected, completeWorkspace, false);

                int nbReproducedFails = 0;
                int nbReproducedErrors = 0;

                for (ProjectInspector inspector : inspectors) {
                    if (inspector.isReproducedAsFail()) {
                        nbReproducedFails++;
                    }
                    if (inspector.isReproducedAsError()) {
                        nbReproducedErrors++;
                    }
                }

                if (googleSpreadSheetEndProcessSerializer != null) {
                    googleSpreadSheetEndProcessSerializer.setReproducedFailures(nbReproducedFails);
                    googleSpreadSheetEndProcessSerializer.setReproducedErrors(nbReproducedErrors);
                    googleSpreadSheetEndProcessSerializer.serialize();
                }
            } else {
                List<ProjectInspector> inspectors = runInspectors(buildsToBeInspected, completeWorkspace, true);

                int nbFixerBuildCase1 = 0;
                int nbFixerBuildCase2 = 0;

                for (ProjectInspector inspector : inspectors) {
                    ProjectInspector4Bears inspector4Bears = (ProjectInspector4Bears)inspector;
                    if (inspector4Bears.isFixerBuildCase1()) {
                        nbFixerBuildCase1++;
                    }
                    if (inspector4Bears.isFixerBuildCase2()) {
                        nbFixerBuildCase2++;
                    }
                }

                if (googleSpreadSheetEndProcessSerializer4Bears != null) {
                    googleSpreadSheetEndProcessSerializer4Bears.setNbFixerBuildCase1(nbFixerBuildCase1);
                    googleSpreadSheetEndProcessSerializer4Bears.setNbFixerBuildCase2(nbFixerBuildCase2);
                    googleSpreadSheetEndProcessSerializer4Bears.serialize();
                }
            }

            Launcher.LOGGER.info("Start writing a JSON output...");

            jsonSerializer.createOutput();

            if (this.clean && completeWorkspace != null) {
                Launcher.LOGGER.info("Clean the workspace now...");
                FileUtils.deleteDirectory(completeWorkspace);
            }

            // to be sure the process is finished
            System.exit(0);
        }
    }

    private List<ProjectInspector> runInspectors(List<BuildToBeInspected> buildsToBeInspected, String workspace, boolean forBear) throws IOException {
        initWorkspace(workspace);

        Launcher.LOGGER.info("Start cloning and compiling projects...");

        List<ProjectInspector> projectInspectors = new ArrayList<ProjectInspector>();
        for (BuildToBeInspected buildToBeInspected : buildsToBeInspected) {
            ProjectInspector inspector;

            if (forBear) {
                inspector = new ProjectInspector4Bears(buildToBeInspected, workspace, this.serializers, null, push);
            } else {
                inspector = new ProjectInspector(buildToBeInspected, workspace, this.serializers, solverPath, push);
            }

            inspector.setAutoclean(clean);
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
        return projectInspectors;
    }

    public static void main(String[] args) throws Exception {
        Launcher launcher = new Launcher();
        launcher.run(args);
    }

}
