package fr.inria.spirals.repairnator;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import fr.inria.spirals.repairnator.notifier.engines.EmailNotifierEngine;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fermadeiral
 */
public class LauncherUtils {

    public static void checkArguments(JSAP jsap, JSAPResult arguments, LauncherType launcherType) {
        if (!arguments.success()) {
            // print out specific error messages describing the problems
            for (java.util.Iterator<?> errs = arguments.getErrorMessageIterator(); errs.hasNext();) {
                System.err.println("Error: " + errs.next());
            }
            printUsage(jsap, launcherType);
        }

        if (arguments.getBoolean("help")) {
            printUsage(jsap, launcherType);
        }

        if (launcherType == LauncherType.SCANNER) {
            if (!arguments.getString("launcherMode").equals("bears") && arguments.getBoolean("skip-failing")) {
                printUsage(jsap, launcherType);
            }
        }
    }

    public static void checkEnvironmentVariables(JSAP jsap, LauncherType launcherType) {
        for (String envVar : Utils.ENVIRONMENT_VARIABLES) {
            if (System.getenv(envVar) == null || System.getenv(envVar).equals("")) {
                System.err.println("You must set the following environment variable: "+envVar);
                LauncherUtils.printUsage(jsap, launcherType);
            }
        }
    }

    public static void printUsage(JSAP jsap, LauncherType launcherType) {
        String moduleName = "repairnator-"+launcherType.name().toLowerCase();
        System.err.println("Usage: java <"+moduleName+" name> [option(s)]");
        System.err.println();
        System.err.println("Options: ");
        System.err.println();
        System.err.println(jsap.getHelp());

        if (launcherType == LauncherType.DOCKERPOOL || launcherType == LauncherType.REALTIME) {
            System.err.println("Please note that the following environment variables must be set: ");
            for (String env : Utils.ENVIRONMENT_VARIABLES) {
                System.err.println(" - " + env);
            }
            System.err.println("For using Nopol, you must add tools.jar in your classpath from your installed jdk");
        }

        System.exit(-1);
    }

    public static List<NotifierEngine> initNotifierEngines(JSAPResult arguments, Logger logger) {
        List<NotifierEngine> notifierEngines = new ArrayList<>();
        if (arguments.getString("smtpServer") != null && arguments.getStringArray("notifyto") != null) {
            logger.info("The email notifier engine will be used.");

            notifierEngines.add(new EmailNotifierEngine(arguments.getStringArray("notifyto"), arguments.getString("smtpServer")));
        } else {
            logger.info("The email notifier engine won't be used.");
        }
        return notifierEngines;
    }

}
