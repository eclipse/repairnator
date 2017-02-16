package fr.inria.spirals.repairnator;

import ch.qos.logback.classic.Level;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.stringparsers.EnumeratedStringParser;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector4Bears;
import fr.inria.spirals.repairnator.process.ProjectScanner;
import ch.qos.logback.classic.Logger;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.serializer.gsheet.inspectors.GoogleSpreadSheetNopolSerializer;
import fr.inria.spirals.repairnator.serializer.gsheet.process.GoogleSpreadSheetEndProcessSerializer;
import fr.inria.spirals.repairnator.serializer.csv.CSVSerializer4Bears;
import fr.inria.spirals.repairnator.serializer.csv.CSVSerializer4RepairNator;
import fr.inria.spirals.repairnator.serializer.gsheet.GoogleSpreadSheetFactory;
import fr.inria.spirals.repairnator.serializer.gsheet.inspectors.GoogleSpreadSheetInspectorSerializer;
import fr.inria.spirals.repairnator.serializer.gsheet.inspectors.GoogleSpreadSheetInspectorTimeSerializer;
import fr.inria.spirals.repairnator.serializer.gsheet.process.GoogleSpreadSheetScannerSerializer;
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

    private static final String[] ENVIRONMENT_VARIABLES = new String[]{"M2_HOME", "GITHUB_LOGIN", "GITHUB_OAUTH"};
    private static final long TIMEOUT_PER_JOB = 1; // in hours
    private static final int NB_THREADS = 4;

    private JSAP jsap;
    private JSAPResult arguments;
    private List<AbstractDataSerializer> serializers;

    private String input;
    private String workspace;
    private int lookupDays;
    private String output;
    private boolean debug;
    private RepairMode mode;
    private boolean clean;
    private boolean push;
    private String solverPath;
    private String googleSecretPath;
    private GoogleSpreadSheetEndProcessSerializer endProcessSerializer;
    private int nbReproducedFails;
    private int nbReproducedErrors;

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

        // pushing builds
        sw1 = new Switch("push");
        sw1.setShortFlag('p');
        sw1.setLongFlag("push");
        sw1.setHelp("If set this flag push failing builds (bypass push even in conjunction with steps option)");
        jsap.registerParameter(sw1);

        // cleaning
        sw1 = new Switch("clean");
        sw1.setLongFlag("clean");
        sw1.setHelp("Clean workspace after each finished process.");
        jsap.registerParameter(sw1);

        // Tab size
        FlaggedOption opt2 = new FlaggedOption("input");
        opt2.setShortFlag('i');
        opt2.setLongFlag("input");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setRequired(true);
        opt2.setHelp("Specify where to find the list of projects or builds to scan.");
        jsap.registerParameter(opt2);


        String modeValues = "";
        for (RepairMode mode : RepairMode.values()) {
            modeValues += mode.name() + ";";
        }
        modeValues.substring(0, modeValues.length() - 2);

        // Tab size
        opt2 = new FlaggedOption("mode");
        opt2.setShortFlag('m');
        opt2.setLongFlag("mode");
        opt2.setStringParser(EnumeratedStringParser.getParser(modeValues));
        opt2.setRequired(true);
        opt2.setHelp("Specify if the input contains project names (SLUG), build ids (BUILD), path to repair (NOPOLONLY), or if it is to inspect passing builds (FORBEARS).");
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
        opt2.setHelp("Specify where to clone failing repository");
        opt2.setDefault("./workspace");
        opt2.setStringParser(JSAP.STRING_PARSER);
        jsap.registerParameter(opt2);

        // Number of day to consider for retrieving builds
        opt2 = new FlaggedOption("lookup");
        opt2.setShortFlag('l');
        opt2.setLongFlag("lookup");
        opt2.setRequired(true);
        opt2.setHelp("Specify the number of hours to lookup in past for builds");
        opt2.setDefault("1");
        opt2.setStringParser(JSAP.INTEGER_PARSER);
        jsap.registerParameter(opt2);

        // Solver path
        opt2 = new FlaggedOption("z3Path");
        opt2.setShortFlag('z');
        opt2.setLongFlag("z3Path");
        opt2.setHelp("Specify the solver path used by Nopol");
        opt2.setStringParser(JSAP.STRING_PARSER);
        jsap.registerParameter(opt2);

        // Google secret path
        opt2 = new FlaggedOption("googleSecretPath");
        opt2.setShortFlag('g');
        opt2.setLongFlag("googleSecretPath");
        opt2.setHelp("Specify the path of the google client secret file");
        opt2.setDefault("./client_secret.json");
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
            System.err.println("Tools.jar must be loaded, here the classpath given for your app: " + System.getProperty("java.class.path"));
            System.exit(-1);
        }
    }

    private void checkNopolSolverPath() {
        solverPath = arguments.getString("z3Path");

        if (solverPath != null) {
            File file = new File(this.arguments.getString("z3Path"));

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

        if (!arguments.success()) {
            // print out specific error messages describing the problems
            for (java.util.Iterator<?> errs = arguments.getErrorMessageIterator(); errs.hasNext(); ) {
                System.err.println("Error: " + errs.next());
            }
        }
        if (!arguments.success() || !checkEnvironmentVariables() || arguments.getBoolean("help")) {
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
        mode = RepairMode.valueOf(arguments.getString("mode").toUpperCase());
        if (mode != RepairMode.FORBEARS) {
            this.checkNopolSolverPath();
        }
        if (arguments.success() && checkEnvironmentVariables()) {
            System.out.println(arguments.toString());
            mainProcess();
        }
    }

    private void mainProcess() throws IOException {
        input = this.arguments.getString("input");
        workspace = arguments.getString("workspace");
        lookupDays = arguments.getInt("lookup");
        output = arguments.getString("output");
        debug = arguments.getBoolean("debug");
        clean = arguments.getBoolean("clean");
        push = arguments.getBoolean("push");
        solverPath = arguments.getString("z3Path");
        googleSecretPath = arguments.getString("googleSecretPath");

        if (debug) {
            setLevel(Level.DEBUG);
        }

        JsonSerializer jsonSerializer = new JsonSerializer(output, mode);
        AbstractDataSerializer csvSerializer;
        if (mode == RepairMode.FORBEARS) {
            csvSerializer = new CSVSerializer4Bears(output);
        } else {
            csvSerializer = new CSVSerializer4RepairNator(output);
        }
        GoogleSpreadSheetInspectorSerializer googleSpreadSheetInspectorSerializer = new GoogleSpreadSheetInspectorSerializer(googleSecretPath);
        GoogleSpreadSheetInspectorTimeSerializer googleSpreadSheetInspectorTimeSerializer = new GoogleSpreadSheetInspectorTimeSerializer(googleSecretPath);

        this.serializers.add(jsonSerializer);
        this.serializers.add(csvSerializer);
        this.serializers.add(googleSpreadSheetInspectorTimeSerializer);

        if (mode == RepairMode.FORBEARS) {
            GoogleSpreadSheetFactory.setSpreadsheetId(GoogleSpreadSheetFactory.BEAR_SPREADSHEET_ID);
        } else {
            this.serializers.add(new GoogleSpreadSheetNopolSerializer(googleSecretPath));
        }

        this.serializers.add(googleSpreadSheetInspectorSerializer);


        Launcher.LOGGER.debug("Start to scan projects in travis...");

        ProjectScanner scanner = new ProjectScanner(lookupDays);

        if (mode == RepairMode.SLUG) {
            this.endProcessSerializer = new GoogleSpreadSheetEndProcessSerializer(scanner, googleSecretPath);
        }

        jsonSerializer.setScanner(scanner);

        List<Build> buildList = null;

        String completeWorkspace = null;

        switch (mode) {
            case BUILD:
                buildList = scanner.getListOfFailingBuildFromGivenBuildIds(input);
                break;

            case SLUG:
                buildList = scanner.getListOfFailingBuildFromProjects(input);
                break;

            case NOPOLONLY:
                completeWorkspace = scanner.readWorkspaceFromInput(input);
                buildList = scanner.readBuildFromInput(input);
                break;

            case FORBEARS:
                buildList = scanner.getListOfPassingBuildsFromProjects(input);
                break;
        }

        if (mode == RepairMode.SLUG || mode == RepairMode.FORBEARS) {
            GoogleSpreadSheetScannerSerializer scannerSerializer = new GoogleSpreadSheetScannerSerializer(scanner, googleSecretPath);
            scannerSerializer.serialize();
        }

        if (buildList != null) {
            for (Build build : buildList) {
                System.out.println("Incriminated project : " + build.getRepository().getSlug() + ":" + build.getId());
            }
        }

        if (mode != RepairMode.NOPOLONLY) {
            Launcher.LOGGER.debug("Start cloning and compiling projects...");
            SimpleDateFormat dateFormat = new SimpleDateFormat("YYYYMMdd_HHmmss");
            completeWorkspace = workspace + File.separator + dateFormat.format(new Date());
        }


        if (completeWorkspace != null) {
            if (mode != RepairMode.FORBEARS) {
                List<ProjectInspector> inspectors = cloneAndRepair(buildList, completeWorkspace);

                for (ProjectInspector inspector : inspectors) {
                    if (inspector.isReproducedAsFail()) {
                        this.nbReproducedFails++;
                    }
                    if (inspector.isReproducedAsError()) {
                        this.nbReproducedErrors++;
                    }
                }

                if (this.endProcessSerializer != null) {
                    this.endProcessSerializer.setReproducedFailures(this.nbReproducedFails);
                    this.endProcessSerializer.setReproducedErrors(this.nbReproducedErrors);
                    this.endProcessSerializer.serialize();
                }
            } else {
                inspectBuildsForBears(buildList, completeWorkspace);
            }

            Launcher.LOGGER.debug("Start writing a JSON output...");

            jsonSerializer.createOutput();

            if (clean && completeWorkspace != null) {
                Launcher.LOGGER.debug("Clean the workspace now...");
                FileUtils.deleteDirectory(completeWorkspace);
            }
        }
    }

    private List<ProjectInspector> cloneAndRepair(List<Build> results, String workspace) throws IOException {
        if (mode != RepairMode.NOPOLONLY) {
            initWorkspace(workspace);
        }

        List<ProjectInspector> projectInspectors = new ArrayList<ProjectInspector>();
        for (Build build : results) {
            ProjectInspector inspector = new ProjectInspector(build, workspace, this.serializers, solverPath, push, mode);
            inspector.setAutoclean(clean);
            projectInspectors.add(inspector);
        }
        final ExecutorService pool = Executors.newWorkStealingPool();

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

    private void inspectBuildsForBears(List<Build> buildList, String workspace) throws IOException {
        initWorkspace(workspace);

        ProjectInspector4Bears inspector;
        for (Build build : buildList) {
            inspector = new ProjectInspector4Bears(build, workspace, this.serializers, null, push, mode);
            inspector.setAutoclean(clean);
            inspector.run();
        }
    }

    public static void main(String[] args) throws Exception {
        Launcher launcher = new Launcher();
        launcher.run(args);
    }
}
