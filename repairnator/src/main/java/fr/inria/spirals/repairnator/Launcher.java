package fr.inria.spirals.repairnator;


import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.helpers.AbstractHelper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by urli on 23/12/2016.
 */
public class Launcher {
    public static final Logger LOGGER = LogManager.getLogger();

    private JSAP jsap;
    private long dateBegin;
    private long dateEnd;
    private ProjectScanner scanner;

    public Launcher() {}

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
        opt2.setHelp("Specify where to find the list of projects to scan.");
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
        opt2.setHelp("Specify where to clone failing repository");
        opt2.setDefault("./workspace");
        opt2.setStringParser(JSAP.STRING_PARSER);
        jsap.registerParameter(opt2);

        // Number of day to consider for retrieving builds
        opt2 = new FlaggedOption("lookup");
        opt2.setShortFlag('l');
        opt2.setLongFlag("lookup");
        opt2.setHelp("Specify the number of day to lookup in past for builds");
        opt2.setDefault("1");
        opt2.setStringParser(JSAP.INTEGER_PARSER);
        jsap.registerParameter(opt2);
    }

    private static void setLevel(Level level) {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();

        LoggerConfig loggerConfig = config.getLoggerConfig(LOGGER.getName());
        loggerConfig.setLevel(level);

        loggerConfig = config.getLoggerConfig("fr.inria.spirals.jtravis.helpers.AbstractHelper");
        loggerConfig.setLevel(level);
        ctx.updateLoggers();
    }

    private static void initWorkspace(String path) throws IOException {
        File file = new File(path);

        if (file.exists()) {
            throw new IOException("The following directory already exists: "+path+". Please choose an empty directory.");
        }
    }

    private void run(String[] args) throws JSAPException, IOException {
        this.defineArgs();
        JSAPResult arguments = jsap.parse(args);

        if (!arguments.success()) {
            // print out specific error messages describing the problems
            for (java.util.Iterator<?> errs = arguments.getErrorMessageIterator(); errs.hasNext();) {
                System.err.println("Error: " + errs.next());
            }
        }
        if (!arguments.success() || arguments.getBoolean("help")) {
            System.err.println("Usage: java <repairnator name> [option(s)]");
            System.err.println();
            System.err.println("Options : ");
            System.err.println();
            System.err.println(jsap.getHelp());
            System.exit(-1);
        }
        if (arguments.success()) {
            mainProcess(arguments.getString("input"), arguments.getString("workspace"), arguments.getString("output"), arguments.getInt("lookup"), arguments.getBoolean("debug"));
        }
    }

    private void mainProcess(String input, String workspace, String output, int lookupDays, boolean debug) throws IOException {
        if (debug) {
            setLevel(Level.DEBUG);
        }

        this.dateBegin = new Date().getTime();

        Launcher.LOGGER.debug("Start to scan projects in travis for failing builds...");

        this.scanner = new ProjectScanner(lookupDays);
        List<Build> buildList = this.scanner.getListOfFailingBuildFromProjects(input);
        for (Build build : buildList) {
            System.out.println("Incriminated project : "+build.getRepository().getSlug());
        }

        Launcher.LOGGER.debug("Start cloning and compiling projects...");
        SimpleDateFormat dateFormat = new SimpleDateFormat("YYYYMMdd_HHmm");
        String completeWorkspace = workspace+File.separator+dateFormat.format(new Date());

        List<ProjectInspector> projectInspectors = cloneAndRepair(buildList, completeWorkspace);

        this.dateEnd = new Date().getTime();

        Launcher.LOGGER.debug("Start writing a JSON output...");
        buildFileFromResults(projectInspectors, output);
    }

    private List<ProjectInspector> cloneAndRepair(List<Build> results, String workspace) throws IOException {
        initWorkspace(workspace);

        List<ProjectInspector> projectInspectors = new ArrayList<ProjectInspector>();
        for (Build build : results) {
            ProjectInspector scanner = new ProjectInspector(build, workspace);
            projectInspectors.add(scanner);
            scanner.processRepair();
        }
        return projectInspectors;
    }

    private void buildFileFromResults(List<ProjectInspector> results, String output) throws IOException {
        Gson gson = new GsonBuilder().setExclusionStrategies(new ExclusionStrategy() {
            public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                return (fieldAttributes.getName().equals("lastBuild"));
            }

            public boolean shouldSkipClass(Class<?> aClass) {
                return false;
            }
        }).create();

        int duration = Math.round(this.dateEnd-this.dateBegin)/1000;

        JsonObject root = new JsonObject();

        JsonElement dateJson = gson.toJsonTree(new Date());
        root.add("date", dateJson);
        root.add("duration", gson.toJsonTree(duration));
        root.add("projects",gson.toJsonTree(this.scanner));

        List<ProjectInspector> testsFailing = new ArrayList<ProjectInspector>();
        List<ProjectInspector> buildableWithoutFailingTests = new ArrayList<ProjectInspector>();
        List<ProjectInspector> notBuildable = new ArrayList<ProjectInspector>();
        List<ProjectInspector> notClonable = new ArrayList<ProjectInspector>();

        for (ProjectInspector inspector : results) {
            inspector.cleanInspector();

            switch (inspector.getState()) {
                default:
                    notClonable.add(inspector);
                    break;

                case CLONABLE:
                    notBuildable.add(inspector);
                    break;

                case BUILDABLE:
                    buildableWithoutFailingTests.add(inspector);
                    break;

                case HASTESTFAILURE:
                    testsFailing.add(inspector);
                    break;
            }
        }

        JsonElement nbFailedJson = gson.toJsonTree(results.size());
        root.add("nbTravisFailDetected", nbFailedJson);

        JsonElement nbBuildableWithFailingTests = gson.toJsonTree(testsFailing.size());
        root.add("nbBuildableWithFailingTests", nbBuildableWithFailingTests);

        JsonElement nbBuildableWithoutFailingTests = gson.toJsonTree(buildableWithoutFailingTests.size());
        root.add("nbBuildableWithoutFailingTests", nbBuildableWithoutFailingTests);

        JsonElement nbFailNotCompilable = gson.toJsonTree(notBuildable.size());
        root.add("nbFailNotCompilable", nbFailNotCompilable);

        JsonElement nbFailNotClonable = gson.toJsonTree(notClonable.size());
        root.add("nbFailNotClonable", nbFailNotClonable);

        JsonElement compilableWithFailingTestsJSON = gson.toJsonTree(testsFailing, List.class);
        root.add("compilableWithFailingTests", compilableWithFailingTestsJSON);

        JsonElement compilableResultsJSON = gson.toJsonTree(buildableWithoutFailingTests, List.class);
        root.add("compilableWithoutFailingTests", compilableResultsJSON);

        JsonElement notCompilableResultsJSON = gson.toJsonTree(notBuildable, List.class);
        root.add("notCompilable", notCompilableResultsJSON);

        JsonElement notClonableResultsJSON = gson.toJsonTree(notClonable, List.class);
        root.add("notClonable", notClonableResultsJSON);

        String serialization = gson.toJson(root);

        File outputFile = new File(output);
        if (outputFile.isDirectory()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("YYYYMMdd_HHmmss");
            String formattedDate = dateFormat.format(new Date());
            String filename = "repairbot_"+formattedDate+".json";
            outputFile = new File(outputFile.getPath()+File.separator+filename);
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
        writer.write(serialization);
        writer.close();
    }

    public static void main(String[] args) throws Exception {
        Launcher launcher = new Launcher();
        launcher.run(args);
    }
}
