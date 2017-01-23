package fr.inria.spirals.repairnator.serializer.csv;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.process.ProjectInspector;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
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
public class CSVSerializer extends AbstractDataSerializer {
    private final static char SEPARATOR = ',';
    private final static String FIRST_LINE = "BuildIds"+SEPARATOR+"Slug"+SEPARATOR+"Status"+SEPARATOR+"Date";
    private final static String FILENAME = "all_data_builds.csv";

    private final Logger logger = LoggerFactory.getLogger(CSVSerializer.class);

    private BufferedWriter stream;
    private SimpleDateFormat tsvCompleteDateFormat;
    private SimpleDateFormat csvOnlyDayFormat;

    public CSVSerializer(String outputPath) {
        this.tsvCompleteDateFormat = new SimpleDateFormat("dd/MM/YY HH:mm");
        this.csvOnlyDayFormat = new SimpleDateFormat("dd/MM/YYYY");
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

    private void writeData(int buildid, String slug, String status, int prNumber, Date date) {
        String buildId = buildid+"";
        String prNumberStr = prNumber+"";
        String line = buildId+SEPARATOR+slug+SEPARATOR+status+SEPARATOR+prNumberStr+SEPARATOR+this.tsvCompleteDateFormat.format(date)+SEPARATOR+this.csvOnlyDayFormat.format(date);
        this.writeNewLine(line);
    }


    @Override
    public void serializeData(ProjectInspector inspector) {
        Build build = inspector.getBuild();
        this.writeData(build.getId(), build.getRepository().getSlug(), inspector.getState().name(), build.getPullRequestNumber(), build.getFinishedAt());
    }
}
