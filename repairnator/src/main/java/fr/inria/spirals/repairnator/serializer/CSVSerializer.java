package fr.inria.spirals.repairnator.serializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by urli on 15/01/2017.
 */
public class CSVSerializer {
    private final static char SEPARATOR = ',';
    private final static String FIRST_LINE = "BuildIds"+SEPARATOR+"Slug"+SEPARATOR+"Status"+SEPARATOR+"Date";
    private final static String FILENAME = "all_data_builds.csv";

    private final Logger logger = LoggerFactory.getLogger(CSVSerializer.class);

    private BufferedWriter stream;
    private SimpleDateFormat tsvDateFormat;

    public CSVSerializer(String outputPath) {
        this.tsvDateFormat = new SimpleDateFormat("dd/MM/YY HH:mm");
        File outputFile = new File(outputPath);

        if (!outputFile.isDirectory()) {
            outputFile = outputFile.getParentFile();
        }

        outputFile = new File(outputFile.getPath()+File.separator+FILENAME);

        boolean fileExists = outputFile.exists();
        try {
            this.stream = new BufferedWriter(new FileWriter(outputFile, true));
            if (!fileExists) {
                this.writeNewLine(FIRST_LINE);
            }
        } catch (IOException e) {
            this.logger.error("Error while creating file writer for: "+outputFile.getPath()+" : "+e);
            this.stream = null;
        }
    }

    private void writeNewLine(String line) {
        if (this.stream != null) {
            try {
                this.stream.write(line);
                this.stream.newLine();
                this.stream.flush();
            } catch (IOException e) {
                this.logger.error("Error while writing in file: "+e);
            }
        }
    }

    public void writeData(int buildid, String slug, String status, Date date) {
        String buildId = buildid+"";
        String line = buildId+SEPARATOR+slug+SEPARATOR+status+SEPARATOR+this.tsvDateFormat.format(date);
        this.writeNewLine(line);
    }


}
