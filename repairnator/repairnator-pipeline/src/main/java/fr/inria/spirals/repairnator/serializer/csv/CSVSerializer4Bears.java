package fr.inria.spirals.repairnator.serializer.csv;

import java.io.BufferedWriter;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.Utils;

/**
 * Created by fermadeiral.
 */
public class CSVSerializer4Bears extends AbstractDataSerializer {

    private static final String FIRST_LINE = "BuildId" + CSVSerializerUtils.SEPARATOR + "PreviousBuildId"
            + CSVSerializerUtils.SEPARATOR + "Slug" + CSVSerializerUtils.SEPARATOR + "Status"
            + CSVSerializerUtils.SEPARATOR + "PRNumber" + CSVSerializerUtils.SEPARATOR + "BuildTimestamp"
            + CSVSerializerUtils.SEPARATOR + "BuildDate" + CSVSerializerUtils.SEPARATOR + "HostName"
            + CSVSerializerUtils.SEPARATOR + "CollectionTimestamp" + CSVSerializerUtils.SEPARATOR + "BuildTravisUrl"
            + CSVSerializerUtils.SEPARATOR + "PreviousBuildTravisUrl";

    private final Logger logger = LoggerFactory.getLogger(CSVSerializer4Bears.class);

    private static BufferedWriter stream;

    public CSVSerializer4Bears(String outputPath) {
        super();
        stream = CSVSerializerUtils.openFile(outputPath, FIRST_LINE);
    }

    private void writeData(int buildId, int previousBuildId, String slug, String state, int prNumber, Date date,
            String hostName, String buildTravisUrl, String previousBuildTravisUrl) {
        String buildIdStr = buildId + "";
        String previousBuildIdStr = previousBuildId + "";
        String prNumberStr = prNumber + "";
        String line = buildIdStr + CSVSerializerUtils.SEPARATOR + previousBuildIdStr + CSVSerializerUtils.SEPARATOR
                + slug + CSVSerializerUtils.SEPARATOR + state + CSVSerializerUtils.SEPARATOR + prNumberStr
                + CSVSerializerUtils.SEPARATOR + Utils.formatCompleteDate(date) + CSVSerializerUtils.SEPARATOR
                + Utils.formatOnlyDay(date) + CSVSerializerUtils.SEPARATOR + hostName
                + CSVSerializerUtils.SEPARATOR + Utils.formatCompleteDate(new Date())
                + CSVSerializerUtils.SEPARATOR + buildTravisUrl + CSVSerializerUtils.SEPARATOR + previousBuildTravisUrl;
        CSVSerializerUtils.writeNewLine(stream, line);
    }

    @Override
    public void serializeData(ProjectInspector inspector) {
        Build build = inspector.getBuild();

        Build previousBuild = inspector.getPreviousBuild();
        int previousBuildId = (previousBuild != null) ? previousBuild.getId() : -1;

        String state = this.getPrettyPrintState(inspector.getState(), inspector.getTestInformations());

        String previousBuildSlug = (previousBuild != null) ? previousBuild.getRepository().getSlug() : "";

        this.writeData(build.getId(), previousBuildId, build.getRepository().getSlug(), state,
                build.getPullRequestNumber(), build.getFinishedAt(), Utils.getHostname(),
                this.getTravisUrl(build.getId(), build.getRepository().getSlug()),
                this.getTravisUrl(previousBuildId, previousBuildSlug));
    }

}
