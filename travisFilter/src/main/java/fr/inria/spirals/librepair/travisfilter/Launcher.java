package fr.inria.spirals.librepair.travisfilter;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.stringparsers.EnumeratedStringParser;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.BuildTool;
import fr.inria.spirals.jtravis.entities.Repository;
import fr.inria.spirals.jtravis.helpers.RepositoryHelper;

import javax.xml.bind.annotation.XmlType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by urli on 17/01/2017.
 */
public class Launcher {

    private static final String DEFAULT_LANGUAGE = "java";
    private static final String DEFAULT_TOOL = "unknown";

    private static List<String> getFileContent(String path) throws IOException {
        List<String> result = new ArrayList<String>();
        File file = new File(path);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        while (reader.ready()) {
            result.add(reader.readLine().trim());
        }
        return result;
    }

    private static List<Repository> getListOfValidRepository(List<String> allSlugs, String language, BuildTool tool) {
        List<Repository> result = new ArrayList<Repository>();

        for (String slug : allSlugs) {
            Repository repo = RepositoryHelper.getRepositoryFromSlug(slug);
            if (repo != null) {
                Build lastBuild = repo.getLastBuild();
                if (lastBuild != null && lastBuild.getConfig().getLanguage().equals(language)) {
                    if (tool == null || lastBuild.getBuildTool() == tool) {
                        result.add(repo);
                    }
                }
            }
        }

        return result;
    }

    public static void writeResultToFile(List<Repository> repos, String outputPath) throws IOException {
        FileWriter writer = new FileWriter(outputPath);

        for (Repository repo : repos) {
            writer.write(repo.getSlug()+"\n");
            writer.flush();
        }
        writer.close();
    }

    public static void main(String[] args) throws Exception {
        JSAP jsap = new JSAP();

        FlaggedOption opt2 = new FlaggedOption("input");
        opt2.setShortFlag('i');
        opt2.setLongFlag("input");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setRequired(true);
        opt2.setHelp("Specify where to find the list of projects to filter.");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("output");
        opt2.setShortFlag('o');
        opt2.setLongFlag("output");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setRequired(true);
        opt2.setHelp("Specify where to place the filtered list of projects.");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("language");
        opt2.setShortFlag('l');
        opt2.setLongFlag("language");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setRequired(false);
        opt2.setDefault(DEFAULT_LANGUAGE);
        opt2.setHelp("Specify the language to filter.");
        jsap.registerParameter(opt2);

        String toolValues = "";
        for (BuildTool tool : BuildTool.values()) {
            toolValues += tool.name() + ";";
        }
        toolValues.substring(0, toolValues.length() - 2);

        // Tab size
        opt2 = new FlaggedOption("tool");
        opt2.setShortFlag('t');
        opt2.setLongFlag("tool");
        opt2.setStringParser(EnumeratedStringParser.getParser(toolValues));
        opt2.setRequired(false);
        opt2.setDefault(DEFAULT_TOOL);
        opt2.setHelp("Specify the tool to filter");
        jsap.registerParameter(opt2);

        JSAPResult arguments = jsap.parse(args);

        if (!arguments.success()) {
            // print out specific error messages describing the problems
            for (java.util.Iterator<?> errs = arguments.getErrorMessageIterator(); errs.hasNext();) {
                System.err.println("Error: " + errs.next());
            }
        }
        if (!arguments.success()) {
            System.err.println("Usage: java <travisFilter name> [option(s)]");
            System.err.println();
            System.err.println("Options : ");
            System.err.println();
            System.err.println(jsap.getHelp());
            System.exit(-1);
        }

        BuildTool toolMode = null;
        if (!arguments.getString("tool").equals(DEFAULT_TOOL)) {
            toolMode = BuildTool.valueOf(arguments.getString("tool").toUpperCase());
        }


        List<String> inputContent = getFileContent(arguments.getString("input"));
        List<Repository> result = getListOfValidRepository(inputContent, arguments.getString("language"), toolMode);

        writeResultToFile(result, arguments.getString("output"));

        System.out.println(result.size()+" results written in "+arguments.getString("output"));
    }


}
