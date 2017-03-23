package fr.inria.spirals.repairnator.serializer;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * Created by urli on 25/01/2017.
 */
public class GoogleSpreadSheetFactory {

    private static Logger logger = LoggerFactory.getLogger(GoogleSpreadSheetFactory.class);

    public static final String REPAIR_SPREADSHEET_ID = "1FUHOVx1Y3QZCAQpwcrnMzbpmMoWTUdNg0KBM3NVL_zA";
    public static final String BEAR_SPREADSHEET_ID = "1MnRwoZGCxxbmkiswc0O6Rg43wJFTBc3bIyrNdTiBhQ4";

    private static String spreadsheetID = REPAIR_SPREADSHEET_ID;
    private static GoogleSpreadSheetFactory instance;

    private Sheets sheets;

    private GoogleSpreadSheetFactory() {}

    public static void initWithAccessToken(String accessToken) throws GeneralSecurityException, IOException {
        if (instance == null) {
            ManageGoogleAccessToken manageGoogleAccessToken = ManageGoogleAccessToken.getInstance();
            manageGoogleAccessToken.initializeCredentialFromAccessToken(accessToken);
            instance = new GoogleSpreadSheetFactory();
            instance.initSheets(manageGoogleAccessToken);
        }
    }

    public static void initWithFileSecret(String googleSecretJson) throws IOException, GeneralSecurityException {
        if (instance == null) {
            ManageGoogleAccessToken manageGoogleAccessToken = ManageGoogleAccessToken.getInstance();
            manageGoogleAccessToken.initializeCredentialFromGoogleSecret(googleSecretJson);
            instance = new GoogleSpreadSheetFactory();
            instance.initSheets(manageGoogleAccessToken);
        }
    }

    public static void setSpreadsheetId(String spreadsheetID) {
        GoogleSpreadSheetFactory.spreadsheetID = spreadsheetID;
    }

    public static String getSpreadsheetID() {
        return GoogleSpreadSheetFactory.spreadsheetID;
    }

    private void initSheets(ManageGoogleAccessToken manageGoogleAccessToken) throws IOException {
        this.sheets = new Sheets.Builder(
                manageGoogleAccessToken.getHttpTransport(),
                manageGoogleAccessToken.getJsonFactory(),
                manageGoogleAccessToken.getCredential()
        ).setApplicationName(ManageGoogleAccessToken.APPLICATION_NAME).build();
    }

    public static Sheets getSheets() {
        if (instance == null) {
            logger.error("You first need to init the instance.");
            return null;
        }
        return instance.sheets;
    }

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
