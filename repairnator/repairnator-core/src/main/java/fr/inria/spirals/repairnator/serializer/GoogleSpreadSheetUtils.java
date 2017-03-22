package fr.inria.spirals.repairnator.serializer;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

/**
 * Created by fernanda on 22/03/17.
 */
public class GoogleSpreadSheetUtils {

    public static void insertData(List<List<Object>> dataRows, Sheets sheets, String range, Logger logger) {
        ValueRange valueRange = new ValueRange();
        valueRange.setValues(dataRows);
        try {
            AppendValuesResponse response = sheets.spreadsheets().values()
                    .append(GoogleSpreadSheetFactory.getSpreadsheetID(), range, valueRange)
                    .setInsertDataOption("INSERT_ROWS").setValueInputOption("USER_ENTERED").execute();
            if (response != null && response.getUpdates().getUpdatedCells() > 0) {
                logger.debug("Data have been inserted in Google Spreadsheet.");
            }
        } catch (IOException e) {
            logger.error("An error occurred while inserting data in Google Spreadsheet.", e);
        }
    }

    public static void logWarningWhenSheetsIsNull(Logger logger) {
        logger.warn("Cannot serialize data: the sheets is not initialized (certainly a credential error)");
    }

}
