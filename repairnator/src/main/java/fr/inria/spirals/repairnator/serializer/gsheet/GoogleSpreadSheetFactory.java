package fr.inria.spirals.repairnator.serializer.gsheet;

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

    private Logger logger = LoggerFactory.getLogger(GoogleSpreadSheetFactory.class);
    private static final String APPLICATION_NAME = "RepairNator Bot";
    private static final File DATA_STORE_DIR = new File(System.getProperty("user.home"), ".credentials/sheets.googleapis.com-repairnator");
    private static final String GOOGLE_SECRET_PATH = "/home/fernanda/git/librepair/repairnator/client_secret.json";
    //public static final String SPREADSHEET_ID = "1FUHOVx1Y3QZCAQpwcrnMzbpmMoWTUdNg0KBM3NVL_zA";
    public static final String SPREADSHEET_ID = "1MnRwoZGCxxbmkiswc0O6Rg43wJFTBc3bIyrNdTiBhQ4";

    private static GoogleSpreadSheetFactory instance;
    private FileDataStoreFactory dataStoreFactory;
    private final JsonFactory jsonFactory;
    private HttpTransport httpTransport;
    private List<String> scopes;
    private Sheets sheets;

    private GoogleSpreadSheetFactory() throws IOException {
        super();
        try {
            this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            this.dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (GeneralSecurityException e) {
            this.logger.error("Error when initiliazing Google Spreadsheet Serializer: ",e);
        } catch (IOException e) {
            this.logger.error("Error when initiliazing Google Spreadsheet Serializer: ",e);
        }

        this.jsonFactory = JacksonFactory.getDefaultInstance();
        this.scopes = Arrays.asList(SheetsScopes.SPREADSHEETS);

        this.initSheets();
    }

    private void initSheets() throws IOException {
        File secretFile = new File(GOOGLE_SECRET_PATH);
        if (!secretFile.exists()) {
            throw new IOException("File containing the token information to access Google API does not exist.");
        }

        // Load client secrets.
        InputStream in = new FileInputStream(secretFile);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(this.jsonFactory, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        this.httpTransport, this.jsonFactory, clientSecrets, this.scopes)
                        .setDataStoreFactory(this.dataStoreFactory)
                        .build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        this.logger.debug("Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());

        this.sheets = new Sheets.Builder(this.httpTransport, this.jsonFactory, credential).setApplicationName(APPLICATION_NAME).build();
    }

    public static Sheets getSheets() throws IOException {
        if (instance == null) {
            instance = new GoogleSpreadSheetFactory();
        }
        return instance.sheets;
    }

}
