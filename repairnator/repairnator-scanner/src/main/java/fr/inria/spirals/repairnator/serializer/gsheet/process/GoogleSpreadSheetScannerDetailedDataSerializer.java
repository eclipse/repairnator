package fr.inria.spirals.repairnator.serializer.gsheet.process;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.ProcessSerializer;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.serializer.GoogleSpreadSheetFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by fernanda on 13/03/17.
 */
public class GoogleSpreadSheetScannerDetailedDataSerializer implements ProcessSerializer {
    private Logger logger = LoggerFactory.getLogger(GoogleSpreadSheetScannerDetailedDataSerializer.class);
    private static final String RANGE = "Scanner Detailed Data!A1:K1";

    private Sheets sheets;
    private List<BuildToBeInspected> buildsToBeInspected;

    public GoogleSpreadSheetScannerDetailedDataSerializer(List<BuildToBeInspected> buildsToBeInspected) {
        this.sheets = GoogleSpreadSheetFactory.getSheets();
        this.buildsToBeInspected = buildsToBeInspected;
    }

    public void serialize() {
        if (this.sheets != null) {
            if (this.buildsToBeInspected.size() > 0) {
                List<List<Object>> dataRows = new ArrayList<List<Object>>();
                Date date = new Date();

                for (BuildToBeInspected buildToBeInspected : this.buildsToBeInspected) {
                    Build build = buildToBeInspected.getBuild();

                    Build previousBuild = buildToBeInspected.getPreviousBuild();
                    int previousBuildId = (previousBuild != null) ? previousBuild.getId() : -1;

                    List<Object> dataCol = new ArrayList<Object>();

                    dataCol.add(build.getId() + "");
                    dataCol.add(previousBuildId + "");
                    dataCol.add(buildToBeInspected.getStatus().toString());
                    dataCol.add(build.getRepository().getSlug());
                    dataCol.add(Utils.formatCompleteDate(date));
                    dataCol.add(Utils.formatOnlyDay(date));
                    dataCol.add(Utils.getHostname());
                    dataCol.add("http://travis-ci.org/" + build.getRepository().getSlug() + "/builds/" + build.getId());
                    dataCol.add("http://travis-ci.org/" + build.getRepository().getSlug() + "/builds/" + previousBuildId);
                    String committerEmail = (build.getCommit().getCommitterEmail() != null) ? build.getCommit().getCommitterEmail() : "-";
                    dataCol.add(committerEmail);
                    dataCol.add(buildToBeInspected.getRunId());
                    dataRows.add(dataCol);
                }

                ValueRange valueRange = new ValueRange();
                valueRange.setValues(dataRows);
                try {
                    AppendValuesResponse response = this.sheets.spreadsheets().values()
                            .append(GoogleSpreadSheetFactory.getSpreadsheetID(), RANGE, valueRange)
                            .setInsertDataOption("INSERT_ROWS").setValueInputOption("USER_ENTERED").execute();
                    if (response != null && response.getUpdates().getUpdatedCells() > 0) {
                        this.logger.debug("Scanner detailed data have been inserted in Google Spreadsheet.");
                    }
                } catch (IOException e) {
                    this.logger.error("An error occured while inserting scanner detailed data in Google Spreadsheet.", e);
                }
            }
        } else {
            this.logger.warn("Cannot serialize data: the sheets is not initialized (certainly a credential error)");
        }
    }
}
