package fr.inria.spirals.repairnator.serializer.engines.json;

import fr.inria.spirals.repairnator.serializer.SerializerType;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Created by urli on 27/03/2017.
 */
public class JSONFileSerializerEngine implements SerializerEngine {
    private Logger logger = LoggerFactory.getLogger(JSONFileSerializerEngine.class);

    private static final String FILE_EXTENSION = ".json";
    private String repoOutputPath;

    public JSONFileSerializerEngine(String repoOutputPath) {
        this.repoOutputPath = repoOutputPath;
    }

    private BufferedWriter openFile(String filename) {
        File outputFile = new File(this.repoOutputPath);

        if (!outputFile.isDirectory()) {
            outputFile = outputFile.getParentFile();
        }

        outputFile = new File(outputFile.getPath() + File.separator + filename);

        BufferedWriter stream;
        try {
            stream = new BufferedWriter(new FileWriter(outputFile, true));
        } catch (IOException e) {
            logger.error("Error while creating file writer for: " + outputFile.getPath(), e);
            stream = null;
        }
        return stream;
    }


    @Override
    public void serialize(List<SerializedData> data, SerializerType serializer) {
        String filename = serializer.getFilename()+FILE_EXTENSION;
        BufferedWriter writer = this.openFile(filename);

        if (writer != null) {
            try {
                for (SerializedData oneData : data) {
                    writer.write(oneData.getAsJson().toString());
                    writer.newLine();
                    writer.flush();
                }
                writer.close();
            } catch (IOException e) {
                logger.error("Error while writing json serialization", e);
            }
        }
    }
}
