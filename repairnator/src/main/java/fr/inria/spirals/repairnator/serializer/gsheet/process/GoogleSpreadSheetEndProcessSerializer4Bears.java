package fr.inria.spirals.repairnator.serializer.gsheet.process;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import fr.inria.spirals.repairnator.process.ProjectScanner;
import fr.inria.spirals.repairnator.serializer.SerializerUtils;
import fr.inria.spirals.repairnator.serializer.gsheet.GoogleSpreadSheetFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by fernanda on 27/02/17.
 */
public class GoogleSpreadSheetEndProcessSerializer4Bears {
    private Logger logger = LoggerFactory.getLogger(GoogleSpreadSheetEndProcessSerializer4Bears.class);
    private static final String RANGE = "End Process!A1:I1";

    private Sheets sheets;
    private ProjectScanner scanner;
    private Date beginDate;
    private int nbFixerBuildCase1;
    private int nbFixerBuildCase2;

    public GoogleSpreadSheetEndProcessSerializer4Bears(ProjectScanner scanner, String googleSecretPath) throws IOException {
        this.sheets = GoogleSpreadSheetFactory.getSheets(googleSecretPath);
        this.scanner = scanner;
        this.beginDate = new Date();
    }

    public void setNbFixerBuildCase1(int nbFixerBuildCase1) {
        this.nbFixerBuildCase1 = nbFixerBuildCase1;
    }

    public void setNbFixerBuildCase2(int nbFixerBuildCase2) {
        this.nbFixerBuildCase2 = nbFixerBuildCase2;
    }

    public void serialize() {
        if (this.sheets != null) {
            Date now = new Date();
            String humanDuration = SerializerUtils.getDuration(this.beginDate, now);

            List<Object> dataCol = new ArrayList<Object>();
            dataCol.add(SerializerUtils.getHostname());
            dataCol.add(SerializerUtils.formatOnlyDay(this.beginDate));
            dataCol.add(SerializerUtils.formatCompleteDate(this.beginDate));
            dataCol.add(SerializerUtils.formatCompleteDate(now));
            dataCol.add(humanDuration);
            dataCol.add(scanner.getTotalJavaPassingBuilds());
            dataCol.add(nbFixerBuildCase1);
            dataCol.add(nbFixerBuildCase2);
            dataCol.add(nbFixerBuildCase1 + nbFixerBuildCase2);

            List<List<Object>> dataRow = new ArrayList<List<Object>>();
            dataRow.add(dataCol);

            ValueRange valueRange = new ValueRange();
            valueRange.setValues(dataRow);

            try {
                AppendValuesResponse response = this.sheets.spreadsheets().values()
                        .append(GoogleSpreadSheetFactory.getSpreadsheetID(), RANGE, valueRange)
                        .setInsertDataOption("INSERT_ROWS").setValueInputOption("USER_ENTERED").execute();
                if (response != null && response.getUpdates().getUpdatedCells() > 0) {
                    this.logger.debug("Scanner data have been inserted in Google Spreadsheet.");
                }
            } catch (IOException e) {
                this.logger.error("An error occured while inserting scanner data in Google Spreadsheet.", e);
            }
        } else {
            this.logger.warn("Cannot serialize data: the sheets is not initialized (certainly a credential error)");
        }

    }
}
