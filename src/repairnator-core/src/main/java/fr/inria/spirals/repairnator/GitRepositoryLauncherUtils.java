package fr.inria.spirals.repairnator;

import com.martiansoftware.jsap.*;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.json.JSONFileSerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.table.CSVSerializerEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GitRepositoryLauncherUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitRepositoryLauncherUtils.class);

    public static void registerGitArgs(JSAP jsap) throws JSAPException {
        // --gitRepoUrl
        jsap.registerParameter(GitRepositoryLauncherUtils.defineArgGitRepositoryUrl());
        // --gitRepoBranch
        jsap.registerParameter(GitRepositoryLauncherUtils.defineArgGitRepositoryBranch());
        // --gitRepoIdCommit
        jsap.registerParameter(GitRepositoryLauncherUtils.defineArgGitRepositoryIdCommit());
        // --gitRepoFirstCommit
        jsap.registerParameter(GitRepositoryLauncherUtils.defineArgGitRepositoryFirstCommit());
    }

    public static void initGitConfig(RepairnatorConfig config, JSAPResult arguments, JSAP jsap) {
        if (GitRepositoryLauncherUtils.getArgGitRepositoryFirstCommit(arguments)) {
            config.setGitRepositoryFirstCommit(true);
        }

        if (arguments.getString("gitRepositoryUrl") == null) {
            LOGGER.error("Parameter 'gitrepourl' is required in GIT_REPOSITORY launcher mode.");
            LauncherUtils.printUsage(jsap, LauncherType.PIPELINE);
        }

        if (config.isGitRepositoryFirstCommit() && arguments.getString("gitRepositoryIdCommit") != null) {
            LOGGER.error("Parameters 'gitrepofirstcommit' and 'gitrepoidcommit' cannot be used at the same time.");
            LauncherUtils.printUsage(jsap, LauncherType.PIPELINE);
        }

        config.setGitRepositoryUrl(GitRepositoryLauncherUtils.getArgGitRepositoryUrl(arguments));
        config.setGitRepositoryBranch(GitRepositoryLauncherUtils.getArgGitRepositoryBranch(arguments));
        config.setGitRepositoryIdCommit(GitRepositoryLauncherUtils.getArgGitRepositoryIdCommit(arguments));
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
        opt.setHelp("Specify a branch of the given repository (only in GIT_REPOSITORY mode)");
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
