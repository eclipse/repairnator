package fr.inria.spirals.repairnator.serializer.gsheet.process;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import fr.inria.spirals.repairnator.ProcessSerializer;
import fr.inria.spirals.repairnator.scanner.ProjectScanner;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.GoogleSpreadSheetFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by urli on 02/02/2017.
 */
public class GoogleSpreadSheetScannerSerializer  implements ProcessSerializer {
    private Logger logger = LoggerFactory.getLogger(GoogleSpreadSheetScannerSerializer.class);
    private static final String RANGE = "Scanner Data!A1:L1";

    private Sheets sheets;
    private ProjectScanner scanner;

    public GoogleSpreadSheetScannerSerializer(ProjectScanner scanner, String googleSecretPath) throws IOException {
        this.sheets = GoogleSpreadSheetFactory.getSheets(googleSecretPath);
        this.scanner = scanner;
    }

    public void serialize() {
        if (this.sheets != null) {
            List<Object> dataCol = new ArrayList<Object>();
            dataCol.add(Utils.getHostname());
            dataCol.add(Utils.formatCompleteDate(new Date()));
            dataCol.add(Utils.formatCompleteDate(this.scanner.getLimitDate()));
            dataCol.add(this.scanner.getTotalRepoNumber());
            dataCol.add(this.scanner.getTotalRepoUsingTravis());
            dataCol.add(this.scanner.getTotalScannedBuilds());
            dataCol.add(this.scanner.getTotalBuildInJava());
            dataCol.add(this.scanner.getTotalJavaPassingBuilds());
            dataCol.add(this.scanner.getTotalBuildInJavaFailing());
            dataCol.add(this.scanner.getTotalBuildInJavaFailingWithFailingTests());
            dataCol.add(this.scanner.getTotalPRBuilds());
            dataCol.add(Utils.formatOnlyDay(this.scanner.getLimitDate()));

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
