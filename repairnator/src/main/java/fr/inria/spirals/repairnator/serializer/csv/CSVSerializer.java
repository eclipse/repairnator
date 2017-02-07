package fr.inria.spirals.repairnator.serializer.csv;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.process.ProjectInspector;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.serializer.SerializerUtils;
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

    public CSVSerializer(String outputPath) {
        super();
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

    private void writeData(int buildid, String slug, String state, String realState, int prNumber, Date date) {
        String buildId = buildid+"";
        String prNumberStr = prNumber+"";
        String line = buildId+SEPARATOR+
                    slug+SEPARATOR+
                    state+SEPARATOR+
                    prNumberStr+SEPARATOR+
                    SerializerUtils.formatCompleteDate(date)+SEPARATOR+
                    SerializerUtils.formatOnlyDay(date)+SEPARATOR+
                    realState+SEPARATOR+
                    SerializerUtils.formatCompleteDate(new Date());
        this.writeNewLine(line);
    }


    @Override
    public void serializeData(ProjectInspector inspector) {
        Build build = inspector.getBuild();

        String state = this.getPrettyPrintState(inspector.getState());

        String realState = (inspector.getState() != null) ? inspector.getState().name() : "null";

        this.writeData(build.getId(), build.getRepository().getSlug(), state, realState, build.getPullRequestNumber(), build.getFinishedAt());
    }
}