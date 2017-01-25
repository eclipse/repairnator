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

import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.process.ProjectInspector;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import com.google.api.client.json.JsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by urli on 25/01/2017.
 */
public class GoogleSpreadsheetSerializer extends AbstractDataSerializer {

    private Logger logger = LoggerFactory.getLogger(GoogleSpreadsheetSerializer.class);
    private static final String APPLICATION_NAME = "RepairNator Bot";
    private static final File DATA_STORE_DIR = new File(System.getProperty("user.home"), ".credentials/sheets.googleapis.com-repairnator");
    private static final String GOOGLE_SECRET_PATH = "./client_secret.json";
    private static final String SPREADSHEET_ID = "1FUHOVx1Y3QZCAQpwcrnMzbpmMoWTUdNg0KBM3NVL_zA";
    private static final String RANGE = "All data!A1:H1";


    private FileDataStoreFactory dataStoreFactory;
    private final JsonFactory jsonFactory;
    private HttpTransport httpTransport;
    private List<String> scopes;
    private Sheets sheets;

    public GoogleSpreadsheetSerializer() throws IOException {
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


    @Override
    public void serializeData(ProjectInspector inspector) {
        Build build = inspector.getBuild();

        String state = this.getPrettyPrintState(inspector.getState());

        String realState = (inspector.getState() != null) ? inspector.getState().name() : "null";

        List<Object> dataCol = new ArrayList<Object>();
        dataCol.add(build.getId()+"");
        dataCol.add(build.getRepository().getSlug());
        dataCol.add(state);
        dataCol.add(build.getPullRequestNumber()+"");
        dataCol.add(this.tsvCompleteDateFormat.format(build.getFinishedAt()));
        dataCol.add(this.csvOnlyDayFormat.format(build.getFinishedAt()));
        dataCol.add(realState);
        dataCol.add(this.tsvCompleteDateFormat.format(new Date()));

        List<List<Object>> dataRow = new ArrayList<List<Object>>();
        dataRow.add(dataCol);

        ValueRange valueRange = new ValueRange();
        valueRange.setValues(dataRow);

        try {
            AppendValuesResponse response = this.sheets.spreadsheets().values().append(SPREADSHEET_ID, RANGE, valueRange).setInsertDataOption("INSERT_ROWS").setValueInputOption("USER_ENTERED").execute();
            if (response != null && response.getUpdates().getUpdatedCells() > 0) {
                this.logger.debug("Data have been inserted in Google Spreadsheet.");
            }
        } catch (IOException e) {
            this.logger.error("An error occured while inserting data in Google Spreadsheet.",e);
        }
    }

}
