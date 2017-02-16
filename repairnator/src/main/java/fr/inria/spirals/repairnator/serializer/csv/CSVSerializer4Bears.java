package fr.inria.spirals.repairnator.serializer.csv;

import java.io.BufferedWriter;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.serializer.SerializerUtils;

/**
 * Created by fermadeiral.
 */
public class CSVSerializer4Bears extends AbstractDataSerializer {
	private final static String FIRST_LINE = "BuildId" + CSVSerializerUtils.SEPARATOR + "PreviousBuildId"
			+ CSVSerializerUtils.SEPARATOR + "Slug" + CSVSerializerUtils.SEPARATOR + "Status"
			+ CSVSerializerUtils.SEPARATOR + "PRNumber" + CSVSerializerUtils.SEPARATOR + "Build Timestamp"
			+ CSVSerializerUtils.SEPARATOR + "Build Date" + CSVSerializerUtils.SEPARATOR + "Collection Timestamp";

	private final Logger logger = LoggerFactory.getLogger(CSVSerializer4Bears.class);

	private static BufferedWriter stream;

	public CSVSerializer4Bears(String outputPath) {
		super();
		stream = CSVSerializerUtils.openFile(outputPath, FIRST_LINE);
	}

	private void writeData(String buildId, String previousBuildId, String slug, String state, int prNumber, Date date) {
		String prNumberStr = prNumber + "";
		String line = buildId + CSVSerializerUtils.SEPARATOR + previousBuildId + CSVSerializerUtils.SEPARATOR + slug
				+ CSVSerializerUtils.SEPARATOR + state + CSVSerializerUtils.SEPARATOR + prNumberStr
				+ CSVSerializerUtils.SEPARATOR + SerializerUtils.formatCompleteDate(date) + CSVSerializerUtils.SEPARATOR
				+ SerializerUtils.formatOnlyDay(date) + CSVSerializerUtils.SEPARATOR
				+ SerializerUtils.formatCompleteDate(new Date());
		CSVSerializerUtils.writeNewLine(stream, line);
	}

	@Override
	public void serializeData(ProjectInspector inspector) {
		Build build = inspector.getBuild();

		Build previousBuild = inspector.getPreviousBuild();
		String previousBuildId = (previousBuild != null) ? previousBuild.getId() + "" : "";

		String state = this.getPrettyPrintState(inspector.getState(), inspector.getTestInformations());

		this.writeData(build.getId() + "", previousBuildId, build.getRepository().getSlug(), state,
				build.getPullRequestNumber(), build.getFinishedAt());
	}
}
