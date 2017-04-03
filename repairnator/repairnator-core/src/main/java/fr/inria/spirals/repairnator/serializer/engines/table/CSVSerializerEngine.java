package fr.inria.spirals.repairnator.serializer.engines.table;

import fr.inria.spirals.repairnator.serializer.SerializerType;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import org.apache.commons.lang3.StringUtils;
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
public class CSVSerializerEngine implements SerializerEngine {
    private Logger logger = LoggerFactory.getLogger(CSVSerializerEngine.class);

    public static final char SEPARATOR = ',';
    private static final String FILE_EXTENSION = ".csv";
    private String repoOutputPath;

    public CSVSerializerEngine(String repoOutputPath) {
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

    private void writeNewLine(BufferedWriter stream, String line) {
        if (stream != null) {
            try {
                stream.write(line);
                stream.newLine();
                stream.flush();
            } catch (IOException e) {
                logger.error("Error while writing in file", e);
            }
        }
    }

    @Override
    public void serialize(List<SerializedData> data, SerializerType serializer) {
        String filename = serializer.getFilename()+FILE_EXTENSION;

        BufferedWriter writer = this.openFile(filename);

        if (writer != null) {
            for (SerializedData row : data) {
                String rowStr = StringUtils.join(row.getAsList(), SEPARATOR);
                this.writeNewLine(writer, rowStr);
            }

            try {
                writer.close();
            } catch (IOException e) {
                logger.error("Error while clonse file", e);
            }
        }
    }
}
