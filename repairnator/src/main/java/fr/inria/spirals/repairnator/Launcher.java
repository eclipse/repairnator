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

    protected static JSAP defineArgs() throws JSAPException {
        // Verbose output
        JSAP jsap = new JSAP();

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

        // Spooned output directory
        opt2 = new FlaggedOption("workspace");
        opt2.setShortFlag('w');
        opt2.setLongFlag("workspace");
        opt2.setHelp("Specify where to clone failing repository");
        opt2.setDefault("./workspace");
        opt2.setStringParser(JSAP.STRING_PARSER);
        jsap.registerParameter(opt2);

        return jsap;
    }

    private static void setLevel(Logger logger, Level level) {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();

        LoggerConfig loggerConfig = config.getLoggerConfig(logger.getName());
        LoggerConfig specificConfig = loggerConfig;

        // We need a specific configuration for this logger,
        // otherwise we would change the level of all other loggers
        // having the original configuration as parent as well

        if (!loggerConfig.getName().equals(logger.getName())) {
            specificConfig = new LoggerConfig(logger.getName(), level, true);
            specificConfig.setParent(loggerConfig);
            config.addLogger(logger.getName(), specificConfig);
        }
        specificConfig.setLevel(level);
        ctx.updateLoggers();
    }

    private static void initWorkspace(String path) throws IOException {
        File file = new File(path);

        if (file.exists()) {
            throw new IOException("The following directory already exists: "+path+". Please choose an empty directory.");
        }
    }

    private static void run(String[] args) throws JSAPException, IOException {
        JSAP jsapSpec = defineArgs();
        JSAPResult arguments = jsapSpec.parse(args);

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
            System.err.println(jsapSpec.getHelp());
            System.exit(-1);
        }
        if (arguments.success()) {
            if (arguments.getBoolean("debug")) {
                setLevel(AbstractHelper.LOGGER, Level.DEBUG);
                setLevel(LogManager.getContext().getLogger("fr.inria.spirals.jtravis.helpers.AbstractHelper"), Level.DEBUG);
            }

            Launcher.LOGGER.debug("Start to scan projects in travis for failing builds...");
            List<Build> buildList = ProjectScanner.getListOfFailingBuildFromProjects(arguments.getString("input"));
            for (Build build : buildList) {
                System.out.println("Incriminated project : "+build.getRepository().getSlug());
            }

            Launcher.LOGGER.debug("Start cloning and compiling projects...");
            String workspace = arguments.getString("workspace");
            SimpleDateFormat dateFormat = new SimpleDateFormat("YYYYMMdd_HHmm");
            String completeWorkspace = workspace+File.separator+dateFormat.format(new Date());

            List<ProjectInspector> projectInspectors = cloneAndRepair(buildList, completeWorkspace);

            Launcher.LOGGER.debug("Start writing a JSON output...");
            buildFileFromResults(projectInspectors, arguments.getString("output"));
        }
    }

    private static List<ProjectInspector> cloneAndRepair(List<Build> results, String workspace) throws IOException {
        initWorkspace(workspace);

        List<ProjectInspector> projectInspectors = new ArrayList<ProjectInspector>();
        for (Build build : results) {
            ProjectInspector scanner = new ProjectInspector(build);
            projectInspectors.add(scanner);
            scanner.cloneInWorkspace(workspace);
        }
        return projectInspectors;
    }

    private static void buildFileFromResults(List<ProjectInspector> results, String output) throws IOException {
        Gson gson = new GsonBuilder().setExclusionStrategies(new ExclusionStrategy() {
            public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                return (fieldAttributes.getName().equals("lastBuild"));
            }

            public boolean shouldSkipClass(Class<?> aClass) {
                return false;
            }
        }).create();

        JsonObject root = new JsonObject();

        JsonElement dateJson = gson.toJsonTree(new Date());
        root.add("date", dateJson);

        List<Build> buildable = new ArrayList<Build>();
        List<Build> notBuildable = new ArrayList<Build>();
        List<Build> notClonable = new ArrayList<Build>();

        for (ProjectInspector inspector : results) {
            if (inspector.canBeCloned()) {
                if (inspector.canBeBuilt()) {
                    buildable.add(inspector.getBuild());
                } else {
                    notBuildable.add(inspector.getBuild());
                }
            } else {
                notClonable.add(inspector.getBuild());
            }

        }

        JsonElement nbFailedJson = gson.toJsonTree(results.size());
        root.add("nbFailDetected", nbFailedJson);

        JsonElement nbFailCompilable = gson.toJsonTree(buildable.size());
        root.add("nbFailCompilable", nbFailCompilable);

        JsonElement nbFailNotCompilable = gson.toJsonTree(notBuildable.size());
        root.add("nbFailNotCompilable", nbFailNotCompilable);

        JsonElement nbFailNotClonable = gson.toJsonTree(notClonable.size());
        root.add("nbFailNotClonable", nbFailNotClonable);

        JsonElement compilableResultsJSON = gson.toJsonTree(buildable, List.class);
        root.add("compilable", compilableResultsJSON);

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
        Launcher.run(args);
    }
}
