package fr.inria.spirals.repairnator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.json.JSONFileSerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.table.CSVSerializerEngine;

public class GitRepositoryLauncherUtils {
	
	public static Switch defineArgGitRepositoryMode() {
        Switch sw = new Switch("gitRepo");
        sw.setLongFlag("gitrepo");
        sw.setDefault("false");
        sw.setHelp("This mode allows to use Repairnator to analyze bugs present in a Git repository.");
        return sw;
    }

    public static boolean getArgGitRepositoryMode(JSAPResult arguments) {
        return arguments.getBoolean("gitRepo");
    }

    public static FlaggedOption defineArgGitRepositoryUrl() {
        FlaggedOption opt = new FlaggedOption("gitRepositoryUrl");
        opt.setLongFlag("gitrepourl");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Specify a Git repository URL (only in GIT_REPOSITORY mode).");
        return opt;
    }

    public static String getArgGitRepositoryUrl(JSAPResult arguments) {
        return arguments.getString("gitRepositoryUrl");
    }

    public static FlaggedOption defineArgGitRepositoryBranch() {
        FlaggedOption opt = new FlaggedOption("gitRepositoryBranch");
        opt.setLongFlag("gitrepobranch");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setDefault("master");
        opt.setHelp("Specify a branch of the given repository (only in GIT_REPOSITORY mode) - (default: master branch).");
        return opt;
    }

    public static String getArgGitRepositoryBranch(JSAPResult arguments) {
        return arguments.getString("gitRepositoryBranch");
    }

    public static FlaggedOption defineArgGitRepositoryIdCommit() {
        FlaggedOption opt = new FlaggedOption("gitRepositoryIdCommit");
        opt.setLongFlag("gitrepoidcommit");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Specify the commit id of the given repository (only in GIT_REPOSITORY mode).");
        return opt;
    }

    public static String getArgGitRepositoryIdCommit(JSAPResult arguments) {
        return arguments.getString("gitRepositoryIdCommit");
    }

    public static Switch defineArgGitRepositoryFirstCommit() {
        Switch sw = new Switch("gitRepositoryFirstCommit");
        sw.setLongFlag("gitrepofirstcommit");
        sw.setDefault("false");
        sw.setHelp("Decides whether to clone the first commit of the specified branch (only in GIT_REPOSITORY mode).");
        return sw;
    }

    public static boolean getArgGitRepositoryFirstCommit(JSAPResult arguments) {
        return arguments.getBoolean("gitRepositoryFirstCommit");
    }

    public static List<SerializerEngine> initFileSerializerEngines(Logger logger) {
        List<SerializerEngine> fileSerializerEngines = new ArrayList<>();
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        if (config.getOutputPath() != null) {
            logger.info("Initialize file serializer engines.");

            String path = config.getOutputPath();

            if (config.getGitRepositoryId() != null) {
                path += File.separator + config.getGitRepositoryId();
            }
            
            fileSerializerEngines.add(new CSVSerializerEngine(path));
            fileSerializerEngines.add(new JSONFileSerializerEngine(path));
        } else {
            logger.info("File serializers won't be used.");
        }
        return fileSerializerEngines;
    }
}
