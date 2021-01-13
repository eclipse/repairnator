package fr.inria.spirals.repairnator;

import com.martiansoftware.jsap.*;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.states.LauncherMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TravisLauncherUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(TravisLauncherUtils.class);

    public static void registerTravisArgs(JSAP jsap) throws JSAPException {
        // --build
        jsap.registerParameter(TravisLauncherUtils.defineArgBuild());
        // --nextBuild
        jsap.registerParameter(TravisLauncherUtils.defineArgNextBuild());
        // --noTravisRepair
        jsap.registerParameter(TravisLauncherUtils.defineArgNoTravisRepair());
        // --jtravisendpoint
        jsap.registerParameter(TravisLauncherUtils.defineArgJTravisEndpoint());
        // --travistoken
        jsap.registerParameter(TravisLauncherUtils.defineArgTravisToken());
    }

    public static void initTravisConfig(RepairnatorConfig config, JSAPResult arguments) {
        if (TravisLauncherUtils.isArgBuildSpecified(arguments)) {
            config.setBuildId(TravisLauncherUtils.getArgBuild(arguments));
        }
        if (config.getLauncherMode() == LauncherMode.BEARS) {
            config.setNextBuildId(TravisLauncherUtils.getArgNextBuild(arguments));
        }
        config.setNoTravisRepair(TravisLauncherUtils.getArgNoTravisRepair(arguments));
        config.setJTravisEndpoint(TravisLauncherUtils.getArgJTravisEndpoint(arguments));
        config.setTravisToken(TravisLauncherUtils.getArgTravisToken(arguments));
    }


    public static FlaggedOption defineArgBuild() {
        FlaggedOption opt = new FlaggedOption("build");
        opt.setShortFlag('b');
        opt.setLongFlag("build");
        opt.setStringParser(JSAP.INTEGER_PARSER);
        opt.setHelp("Specify the build id to use.");
        return opt;
    }

    public static Integer getArgBuild(JSAPResult arguments) {
        return arguments.getInt("build");
    }

    public static boolean isArgBuildSpecified(JSAPResult arguments) {
        return arguments.contains("build");
    }

    public static FlaggedOption defineArgNextBuild() {
        FlaggedOption opt = new FlaggedOption("nextBuild");
        opt.setShortFlag('n');
        opt.setLongFlag("nextBuild");
        opt.setStringParser(JSAP.INTEGER_PARSER);
        opt.setDefault(InputBuildId.NO_PATCH + "");
        opt.setHelp("Specify the next build id to use (only in BEARS mode).");
        return opt;
    }

    public static Integer getArgNextBuild(JSAPResult arguments) {
        return arguments.getInt("nextBuild");
    }

    public static Switch defineArgNoTravisRepair() {
        Switch sw = new Switch("noTravisRepair");
        sw.setLongFlag("noTravisRepair");
        sw.setDefault("false");
        sw.setHelp("repair with git url , branch and commit instead of travis build ids");
        return sw;
    }

    public static boolean getArgNoTravisRepair(JSAPResult arguments) {
        return arguments.getBoolean("noTravisRepair");
    }

    public static FlaggedOption defineArgJTravisEndpoint() {
        FlaggedOption opt = new FlaggedOption("jtravisendpoint");
        opt.setLongFlag("jtravisendpoint");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setDefault("https://api.travis-ci.org");
        opt.setHelp("The endpoint where JTravis points its requests");
        return opt;
    }

    public static String getArgJTravisEndpoint(JSAPResult arguments) {
        return arguments.getString("jtravisendpoint");
    }

    public static FlaggedOption defineArgTravisToken() {
        FlaggedOption opt = new FlaggedOption("travistoken");
        opt.setLongFlag("travistoken");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setDefault("");
        opt.setHelp("TravisCI.com required token");
        return opt;
    }

    public static String getArgTravisToken(JSAPResult arguments) {
        return arguments.getString("travistoken");
    }

}