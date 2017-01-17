package fr.inria.spirals.librepair.travisfilter;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.Repository;
import fr.inria.spirals.jtravis.helpers.RepositoryHelper;

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
    private static final String USAGE = "<travis-filter.jar> <input> <output> [language (default: "+DEFAULT_LANGUAGE+")]";

    private static List<String> getFileContent(String path) throws IOException {
        List<String> result = new ArrayList<String>();
        File file = new File(path);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        while (reader.ready()) {
            result.add(reader.readLine().trim());
        }
        return result;
    }

    private static List<Repository> getListOfValidRepository(List<String> allSlugs, String language) {
        List<Repository> result = new ArrayList<Repository>();

        for (String slug : allSlugs) {
            Repository repo = RepositoryHelper.getRepositoryFromSlug(slug);
            if (repo != null) {
                Build lastBuild = repo.getLastBuild();
                if (lastBuild != null && lastBuild.getConfig().getLanguage().equals(language)) {
                    result.add(repo);
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

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println(USAGE);
            return;
        }

        String inputPath = args[0];
        String outputPath = args[1];
        String language = DEFAULT_LANGUAGE;

        if (args.length == 3) {
            language = args[2];
        }

        List<String> inputContent = getFileContent(inputPath);
        List<Repository> result = getListOfValidRepository(inputContent, language);

        writeResultToFile(result, outputPath);

        System.out.println(result.size()+" results written in "+outputPath);
    }


}
