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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
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

        // Tab size
        FlaggedOption opt2 = new FlaggedOption("input");
        opt2.setShortFlag('i');
        opt2.setLongFlag("input");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setRequired(true);
        opt2.setHelp("Specify where to find the list of projects to scan.");
        jsap.registerParameter(opt2);

        // Spooned output directory
        opt2 = new FlaggedOption("output");
        opt2.setShortFlag('o');
        opt2.setLongFlag("output");
        opt2.setHelp("Specify where to place JSON output.");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setRequired(true);
        jsap.registerParameter(opt2);

        return jsap;
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
            List<Build> buildList = ProjectScanner.getListOfFailingBuildFromProjects(arguments.getString("input"));
            for (Build build : buildList) {
                System.out.println("Incriminated project : "+build.getRepository().getSlug());
            }
            buildFileFromResults(buildList, arguments.getString("output"));
        }
    }

    private static void buildFileFromResults(List<Build> results, String output) throws IOException {
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

        JsonElement sizeJson = gson.toJsonTree(results.size());
        root.add("number", sizeJson);

        JsonElement allResults = gson.toJsonTree(results, List.class);
        root.add("results", allResults);



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
