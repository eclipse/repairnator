package fr.inria.spirals.repairnator.pipeline;

import fr.inria.spirals.repairnator.InputBuildId;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.states.LauncherMode;

import java.net.URLClassLoader;
import java.io.InputStream;
import java.util.Properties;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.martiansoftware.jsap.JSAP;

/* General helper methods which all launchers use */
public class LauncherHelpers {
	private static LauncherHelpers launcherHelper;
	private static Logger LOGGER = LoggerFactory.getLogger(LauncherHelpers.class);

	private LauncherHelpers(){}

	public static LauncherHelpers getInstance() {
		if (launcherHelper == null) {
			launcherHelper = new LauncherHelpers();
		}
		return launcherHelper;
	}

	public void checkToolsLoaded(JSAP jsap) {
        URLClassLoader loader;
        try {
            loader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            loader.loadClass("com.sun.jdi.AbsentInformationException");
        } catch (ClassNotFoundException e) {
            System.err.println("Tools.jar must be loaded. The classpath given for your app is: "+System.getProperty("java.class.path"));
        }
    }

    public void checkNopolSolverPath(JSAP jsap) {
        String solverPath = RepairnatorConfig.getInstance().getZ3solverPath();
        // by default Nopol run in Dynamoth mode
        // so no solver is mandatory
    }

    public void checkNextBuildId(JSAP jsap) {
        if (RepairnatorConfig.getInstance().getNextBuildId() == InputBuildId.NO_PATCH) {
            System.err.println("A pair of builds needs to be provided in BEARS mode.");
        }
    }
		
    public void loadProperty() {
    	InputStream propertyStream = getClass().getResourceAsStream("/version.properties");
        Properties properties = new Properties();
        if (propertyStream != null) {
            try {
                properties.load(propertyStream);
            } catch (IOException e) {
                LOGGER.error("Error while loading property file.", e);
            }
            LOGGER.info("PIPELINE VERSION: "+properties.getProperty("PIPELINE_VERSION"));
        } else {
            LOGGER.info("No information about PIPELINE VERSION has been found.");
        }
    }
}