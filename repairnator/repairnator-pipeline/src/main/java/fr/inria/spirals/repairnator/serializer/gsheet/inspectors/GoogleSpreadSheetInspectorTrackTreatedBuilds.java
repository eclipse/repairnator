package fr.inria.spirals.repairnator.serializer.gsheet.inspectors;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.serializer.GoogleSpreadSheetFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by urli on 03/03/2017.
 */
public class GoogleSpreadSheetInspectorTrackTreatedBuilds extends AbstractDataSerializer {

    private Logger logger = LoggerFactory.getLogger(GoogleSpreadSheetInspectorTrackTreatedBuilds.class);
    private static final String RANGE = "Treated Build Tracking!A1:E1";

    private Sheets sheets;
    private String runid;

    public GoogleSpreadSheetInspectorTrackTreatedBuilds(BuildToBeInspected buildToBeInspected, String googleSecretPath) throws IOException {
        super();
        this.runid = UUID.randomUUID().toString();
        this.sheets = GoogleSpreadSheetFactory.getSheets(googleSecretPath);
        this.serializeBuildToBeInspected(buildToBeInspected);
    }

    private void insertData(List<List<Object>> dataRows) {
        ValueRange valueRange = new ValueRange();
        valueRange.setValues(dataRows);
        try {
            AppendValuesResponse response = this.sheets.spreadsheets().values()
                    .append(GoogleSpreadSheetFactory.getSpreadsheetID(), RANGE, valueRange)
                    .setInsertDataOption("INSERT_ROWS").setValueInputOption("USER_ENTERED").execute();
            if (response != null && response.getUpdates().getUpdatedCells() > 0) {
                this.logger.debug("Data have been inserted in Google Spreadsheet.");
            }
        } catch (IOException e) {
            this.logger.error("An error occurred while inserting data in Google Spreadsheet.", e);
        }
    }

    private List<Object> getDataColumn(Build build, String status) {
        Date date = new Date();

        List<Object> dataCol = new ArrayList<Object>();
        dataCol.add(runid);
        dataCol.add(build.getId());
        dataCol.add(build.getRepository().getSlug());
        dataCol.add(Utils.formatCompleteDate(date));
        dataCol.add(Utils.formatOnlyDay(date));
        dataCol.add(Utils.getHostname());
        dataCol.add(status);

        return dataCol;
    }

    private void serializeBuildToBeInspected(BuildToBeInspected buildToBeInspected) {
        if (this.sheets != null) {
            Build build = buildToBeInspected.getBuild();

            List<Object> dataCol = getDataColumn(build, "DETECTED");

            List<List<Object>> dataRows = new ArrayList<List<Object>>();
            dataRows.add(dataCol);
            this.insertData(dataRows);
        } else {
            this.logger.warn("Cannot serialize data: the sheets is not initialized (certainly a credential error)");
        }
    }

    @Override
    public void serializeData(ProjectInspector inspector) {
        if (this.sheets != null) {
            Build build = inspector.getBuild();

            List<Object> dataCol = getDataColumn(build, "TREATED");

            List<List<Object>> dataRows = new ArrayList<List<Object>>();
            dataRows.add(dataCol);
            this.insertData(dataRows);
        } else {
            this.logger.warn("Cannot serialize data: the sheets is not initialized (certainly a credential error)");
        }
    }

}
