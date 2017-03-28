package fr.inria.spirals.repairnator.serializer.gspreadsheet;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.SheetsScopes;
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
 * Created by urli on 23/03/2017.
 */
public class ManageGoogleAccessToken {
    private Logger logger = LoggerFactory.getLogger(ManageGoogleAccessToken.class);

    public static final String APPLICATION_NAME = "RepairNator Bot";
    private static final File DATA_STORE_DIR = new File(System.getProperty("user.home"), ".credentials/sheets.googleapis.com-repairnator");

    private static ManageGoogleAccessToken instance;

    private FileDataStoreFactory dataStoreFactory;
    private JsonFactory jsonFactory;
    private HttpTransport httpTransport;
    private List<String> scopes;
    private Credential credential;

    private ManageGoogleAccessToken() throws GeneralSecurityException, IOException {
        this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        this.jsonFactory = JacksonFactory.getDefaultInstance();
        this.scopes = Arrays.asList(SheetsScopes.SPREADSHEETS);
    }

    public static ManageGoogleAccessToken getInstance() throws GeneralSecurityException, IOException {
        if (instance == null) {
            instance = new ManageGoogleAccessToken();
        }
        return instance;
    }

    public void initializeCredentialFromGoogleSecret(String googleSecretPath) throws IOException {
        this.dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);

        File secretFile = new File(googleSecretPath);
        if (!secretFile.exists()) {
            throw new IOException("File containing the token information to access Google API does not exist.");
        }

        // Load client secrets.
        InputStream in = new FileInputStream(secretFile);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(this.jsonFactory, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                this.httpTransport,
                this.jsonFactory,
                clientSecrets,
                this.scopes
        ).setDataStoreFactory(this.dataStoreFactory)
                .setAccessType("offline")
                .build();
        this.credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");

        this.logger.info("Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
    }

    public void initializeCredentialFromAccessToken(String accessToken) {
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(accessToken);
        tokenResponse.setTokenType("offline");
        this.credential = new GoogleCredential().setFromTokenResponse(tokenResponse);
    }

    public JsonFactory getJsonFactory() {
        return jsonFactory;
    }

    public HttpTransport getHttpTransport() {
        return httpTransport;
    }

    public Credential getCredential() {
        return credential;
    }

    private static void usage() {
        System.err.println("Usage: java -jar repairnator-core.jar path/to/googleSecretFile.json");
        System.exit(1);
    }

    public static void main(String[] args) throws GeneralSecurityException, IOException {
        if (args.length < 1) {
            usage();
        }

        File clientSecret = new File(args[0]);

        if (!clientSecret.exists()) {
            usage();
        }

        ManageGoogleAccessToken manageGoogleAccessToken = ManageGoogleAccessToken.getInstance();
        manageGoogleAccessToken.initializeCredentialFromGoogleSecret(clientSecret.getPath());

        System.out.println("Credentials have been initialized, here's your access token: "+manageGoogleAccessToken.getCredential().getAccessToken());
    }
}
