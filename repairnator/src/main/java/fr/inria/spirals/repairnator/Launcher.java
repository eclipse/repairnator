package fr.inria.spirals.repairnator;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.stringparsers.EnumeratedStringParser;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.process.ProjectInspector;
import fr.inria.spirals.repairnator.process.ProjectScanner;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.io.File;
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
    private JsonSerializer serializer;
    private JSAPResult arguments;

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

        // pushing builds
        sw1 = new Switch("push");
        sw1.setShortFlag('p');
        sw1.setLongFlag("push");
        sw1.setHelp("If set to true this flag push failing builds. (this argument allow to avoid push even if the step number is higher with -s argument)");
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
        opt2.setHelp("Specify the number of hours to lookup in past for builds");
        opt2.setDefault("1");
        opt2.setStringParser(JSAP.INTEGER_PARSER);
        jsap.registerParameter(opt2);

        // Steps to do
        opt2 = new FlaggedOption("steps");
        opt2.setShortFlag('s');
        opt2.setLongFlag("steps");
        opt2.setHelp("Specify the number of steps to realize (0: only scan projects, 1: try to clone, 2: try to build, 3: try to test, 4: gather info on test, 5: push build, 6: call Nopol)");
        opt2.setDefault("6");
        opt2.setStringParser(EnumeratedStringParser.getParser("0;1;2;3;4;5;6"));
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
        this.arguments = jsap.parse(args);

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
            mainProcess();
        }
    }

    private void mainProcess() throws IOException {
        String input = this.arguments.getString("input");
        String workspace = arguments.getString("workspace");
        int lookupDays = arguments.getInt("lookup");
        String output = arguments.getString("output");
        boolean debug = arguments.getBoolean("debug");
        int steps = Integer.parseInt(arguments.getString("steps"));

        if (debug) {
            setLevel(Level.DEBUG);
        }
        this.serializer = new JsonSerializer(output);

        Launcher.LOGGER.debug("Start to scan projects in travis for failing builds...");

        ProjectScanner scanner = new ProjectScanner(lookupDays);

        this.serializer.setScanner(scanner);

        List<Build> buildList = scanner.getListOfFailingBuildFromProjects(input);
        for (Build build : buildList) {
            System.out.println("Incriminated project : "+build.getRepository().getSlug()+":"+build.getId());
        }

        if (steps > 0) {
            Launcher.LOGGER.debug("Start cloning and compiling projects...");
            SimpleDateFormat dateFormat = new SimpleDateFormat("YYYYMMdd_HHmm");
            String completeWorkspace = workspace+File.separator+dateFormat.format(new Date());

            List<ProjectInspector> projectInspectors = cloneAndRepair(buildList, completeWorkspace);

            this.serializer.setInspectors(projectInspectors);
        }


        Launcher.LOGGER.debug("Start writing a JSON output...");

        this.serializer.createOutput();
    }

    private List<ProjectInspector> cloneAndRepair(List<Build> results, String workspace) throws IOException {
        boolean push = arguments.getBoolean("push");
        int steps = Integer.parseInt(arguments.getString("steps"));
        initWorkspace(workspace);

        List<ProjectInspector> projectInspectors = new ArrayList<ProjectInspector>();
        for (Build build : results) {
            ProjectInspector scanner = new ProjectInspector(build, workspace, push, steps);
            projectInspectors.add(scanner);
            scanner.processRepair();
        }
        return projectInspectors;
    }

    public static void main(String[] args) throws Exception {
        Launcher launcher = new Launcher();
        launcher.run(args);
    }
}
