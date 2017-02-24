package fr.inria.spirals.repairnator.serializer.gsheet.inspectors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.serializer.SerializerUtils;
import fr.inria.spirals.repairnator.serializer.gsheet.GoogleSpreadSheetFactory;

/**
 * Created by fermadeiral.
 */
public class GoogleSpreadSheetInspectorSerializer4Bears extends AbstractDataSerializer {
    private Logger logger = LoggerFactory.getLogger(GoogleSpreadSheetInspectorSerializer4Bears.class);
    private static final String RANGE = "All data!A1:K1";

    private Sheets sheets;

    public GoogleSpreadSheetInspectorSerializer4Bears(String googleSecretPath) throws IOException {
        super();
        this.sheets = GoogleSpreadSheetFactory.getSheets(googleSecretPath);
    }

    @Override
    public void serializeData(ProjectInspector inspector) {
        if (this.sheets != null) {
            Build build = inspector.getBuild();

            Build previousBuild = inspector.getPreviousBuild();
            int previousBuildId = (previousBuild != null) ? previousBuild.getId() : -1;

            String state = this.getPrettyPrintState(inspector.getState(), inspector.getTestInformations());

            String previousBuildSlug = (previousBuild != null) ? previousBuild.getRepository().getSlug() : "";

            List<Object> dataCol = new ArrayList<Object>();
            dataCol.add(build.getId() + "");
            dataCol.add(previousBuildId + "");
            dataCol.add(build.getRepository().getSlug());
            dataCol.add(state);
            dataCol.add(build.getPullRequestNumber() + "");
            dataCol.add(SerializerUtils.formatCompleteDate(build.getFinishedAt()));
            dataCol.add(SerializerUtils.formatOnlyDay(build.getFinishedAt()));
            dataCol.add(SerializerUtils.getHostname());
            dataCol.add(SerializerUtils.formatCompleteDate(new Date()));
            dataCol.add(this.getTravisUrl(build.getId(), build.getRepository().getSlug()));
            dataCol.add(this.getTravisUrl(previousBuildId, previousBuildSlug));

            List<List<Object>> dataRow = new ArrayList<List<Object>>();
            dataRow.add(dataCol);

            ValueRange valueRange = new ValueRange();
            valueRange.setValues(dataRow);

            try {
                AppendValuesResponse response = this.sheets.spreadsheets().values()
                        .append(GoogleSpreadSheetFactory.getSpreadsheetID(), RANGE, valueRange)
                        .setInsertDataOption("INSERT_ROWS").setValueInputOption("USER_ENTERED").execute();
                if (response != null && response.getUpdates().getUpdatedCells() > 0) {
                    this.logger.debug("Data have been inserted in Google Spreadsheet.");
                }
            } catch (IOException e) {
                this.logger.error("An error occured while inserting data in Google Spreadsheet.", e);
            }
        } else {
            this.logger.warn("Cannot serialize data: the sheets is not initialized (certainly a credential error)");
        }
    }
}
