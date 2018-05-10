package fr.inria.spirals.repairnator.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by urli on 08/03/2017.
 */
public class RepairnatorConfigReader {
    private Logger logger = LoggerFactory.getLogger(RepairnatorConfigReader.class);
    protected static final String FILENAME = "repairconfig.ini";

    public void readConfigFile(RepairnatorConfig config) throws RepairnatorConfigException {
        String currentDir = System.getProperty("user.dir");

        InputStream inputStream;
        try {
            inputStream = new FileInputStream(currentDir+File.separator+FILENAME);
        } catch (FileNotFoundException e) {
            logger.info("Cannot find "+FILENAME+" file in current directory. It will use the default one.");
            inputStream = getClass().getClassLoader().getResourceAsStream(FILENAME);
            if (inputStream == null) {
                throw new RepairnatorConfigException("Cannot load default "+FILENAME+" from resources.");
            }
        }

        try {
            Properties properties = this.readPropertiesFromInputStream(inputStream);
            setConfigurationFromProperties(properties, config);
        } catch (IOException e) {
            logger.error("Error while reading the config.ini file. ",e);
            throw new RepairnatorConfigException("Error while reading the "+FILENAME+" file", e);
        }
    }

    private void setConfigurationFromProperties(Properties properties, RepairnatorConfig config) throws RepairnatorConfigException {
        try {
            config.setPush(Boolean.parseBoolean(properties.getProperty("push")));
            config.setWorkspacePath(properties.getProperty("workspacePath"));
            config.setZ3solverPath(properties.getProperty("z3path"));
            config.setSerializeJson(Boolean.parseBoolean(properties.getProperty("json")));
            config.setOutputPath(properties.getProperty("jsonOutputPath"));
            config.setPushRemoteRepo(properties.getProperty("pushRemoteRepo"));
        } catch (Exception e) {
            logger.error("Error while setting config values from properties.");
            throw new RepairnatorConfigException("Error while setting config values from properties.", e);
        }
    }

    private Properties readPropertiesFromInputStream(InputStream stream) throws IOException {
        Properties properties = new Properties();
        properties.load(stream);
        return properties;
    }
}
