package fr.inria.spirals.repairnator.config;

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
    private static final String FILENAME = "config.ini";

    public boolean readConfigFile() {
        String currentDir = System.getProperty("user.dir");
        try {
            InputStream inputStream = new FileInputStream(currentDir+File.separator+FILENAME);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Properties readPropertiesFromInputStream(InputStream stream) throws IOException {
        Properties properties = new Properties();
        properties.load(stream);
    }
}
