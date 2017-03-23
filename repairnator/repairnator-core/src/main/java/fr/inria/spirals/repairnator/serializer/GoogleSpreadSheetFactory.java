package fr.inria.spirals.repairnator.serializer;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.client.json.JsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
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

}
