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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by urli on 16/02/2017.
 */
public class GoogleSpreadSheetEndProcessSerializer {
    private Logger logger = LoggerFactory.getLogger(GoogleSpreadSheetEndProcessSerializer.class);
    private static final String RANGE = "End Process!A1:I1";

    private Sheets sheets;
    private ProjectScanner scanner;
    private Date beginDate;
    private int reproducedFailures;
    private int reproducedErrors;

    public GoogleSpreadSheetEndProcessSerializer(ProjectScanner scanner, String googleSecretPath) throws IOException {
        this.sheets = GoogleSpreadSheetFactory.getSheets(googleSecretPath);
        this.scanner = scanner;
        this.beginDate = new Date();
    }

    public void setReproducedFailures(int reproducedFailures) {
        this.reproducedFailures = reproducedFailures;
    }

    public void setReproducedErrors(int reproducedErrors) {
        this.reproducedErrors = reproducedErrors;
    }

    public void serialize() {
        Date now = new Date();
        String humanDuration = SerializerUtils.getDuration(this.beginDate, now);

        List<Object> dataCol = new ArrayList<Object>();
        dataCol.add(SerializerUtils.getHostname());
        dataCol.add(SerializerUtils.formatOnlyDay(this.beginDate));
        dataCol.add(SerializerUtils.formatCompleteDate(this.beginDate));
        dataCol.add(SerializerUtils.formatCompleteDate(now));
        dataCol.add(humanDuration);
        dataCol.add(scanner.getTotalBuildInJavaFailingWithFailingTests());
        dataCol.add(reproducedFailures);
        dataCol.add(reproducedErrors);
        dataCol.add(reproducedErrors+reproducedFailures);

        List<List<Object>> dataRow = new ArrayList<List<Object>>();
        dataRow.add(dataCol);

        ValueRange valueRange = new ValueRange();
        valueRange.setValues(dataRow);

        try {
            AppendValuesResponse response = this.sheets.spreadsheets().values().append(GoogleSpreadSheetFactory.getSpreadsheetID(), RANGE, valueRange).setInsertDataOption("INSERT_ROWS").setValueInputOption("USER_ENTERED").execute();
            if (response != null && response.getUpdates().getUpdatedCells() > 0) {
                this.logger.debug("Scanner data have been inserted in Google Spreadsheet.");
            }
        } catch (IOException e) {
            this.logger.error("An error occured while inserting scanner data in Google Spreadsheet.",e);
        }
    }
}
