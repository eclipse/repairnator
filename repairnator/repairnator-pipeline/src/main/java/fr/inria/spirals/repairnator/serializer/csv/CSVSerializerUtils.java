package fr.inria.spirals.repairnator.serializer.csv;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by urli on 15/01/2017.
 */
public class CSVSerializerUtils {
    private static final Logger logger = LoggerFactory.getLogger(CSVSerializerUtils.class);

    public static final char SEPARATOR = ',';
    private static final String FILENAME = "all_data_builds.csv";

    public static BufferedWriter openFile(String outputPath, String firstLine) {
        File outputFile = new File(outputPath);

        if (!outputFile.isDirectory()) {
            outputFile = outputFile.getParentFile();
        }

        outputFile = new File(outputFile.getPath() + File.separator + FILENAME);

        boolean fileExists = outputFile.exists();
        BufferedWriter stream;
        try {
            stream = new BufferedWriter(new FileWriter(outputFile, true));
            if (!fileExists) {
                writeNewLine(stream, firstLine);
            }
        } catch (IOException e) {
            logger.error("Error while creating file writer for: " + outputFile.getPath() + " : " + e);
            stream = null;
        }
        return stream;
    }

    public static void writeNewLine(BufferedWriter stream, String line) {
        if (stream != null) {
            try {
                stream.write(line);
                stream.newLine();
                stream.flush();
            } catch (IOException e) {
                logger.error("Error while writing in file: " + e);
            }
        }
    }
}
