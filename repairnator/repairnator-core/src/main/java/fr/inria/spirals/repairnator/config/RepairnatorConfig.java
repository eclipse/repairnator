package fr.inria.spirals.repairnator.config;

import fr.inria.spirals.repairnator.LauncherMode;

/**
 * Created by urli on 08/03/2017.
 */
public class RepairnatorConfig {
    private LauncherMode launcherMode;
    private boolean clean;
    private boolean push;
    private String workspacePath;
    private String z3solverPath;
    private boolean serializeJson;
    private String jsonOutputPath;
    private String pushRemoteRepo;
    private String googleAccessToken;
    private String runId;
    private String spreadsheetId;
    private String mongodbHost;
    private String mongodbName;
    private String smtpServer;
    private String[] notifyTo;

    private static RepairnatorConfig instance;

    private RepairnatorConfig() {}

    public void readFromFile() throws RepairnatorConfigException {
        RepairnatorConfigReader configReader = new RepairnatorConfigReader();
        configReader.readConfigFile(this);

    }

    // for test purpose
    public static void deleteInstance() {
        instance = null;
    }

    public static RepairnatorConfig getInstance() {
        if (instance == null) {
            instance = new RepairnatorConfig();
        }
        return instance;
    }

    public LauncherMode getLauncherMode() {
        return launcherMode;
    }

    public void setLauncherMode(LauncherMode launcherMode) {
        this.launcherMode = launcherMode;
    }

    public boolean isClean() {
        return clean;
    }

    public void setClean(boolean clean) {
        this.clean = clean;
    }

    public boolean isPush() {
        return push;
    }

    public void setPush(boolean push) {
        this.push = push;
    }

    public String getWorkspacePath() {
        return workspacePath;
    }

    public void setWorkspacePath(String workspacePath) {
        this.workspacePath = workspacePath;
    }

    public String getZ3solverPath() {
        return z3solverPath;
    }

    public void setZ3solverPath(String z3solverPath) {
        this.z3solverPath = z3solverPath;
    }

    public boolean isSerializeJson() {
        return serializeJson;
    }

    public void setSerializeJson(boolean serializeJson) {
        this.serializeJson = serializeJson;
    }

    public String getJsonOutputPath() {
        return jsonOutputPath;
    }

    public void setJsonOutputPath(String jsonOutputPath) {
        this.jsonOutputPath = jsonOutputPath;
    }

    public String getPushRemoteRepo() {
        return pushRemoteRepo;
    }

    public void setPushRemoteRepo(String pushRemoteRepo) {
        this.pushRemoteRepo = pushRemoteRepo;
    }

    public String getGoogleAccessToken() {
        return googleAccessToken;
    }

    public void setGoogleAccessToken(String googleAccessToken) {
        this.googleAccessToken = googleAccessToken;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getSpreadsheetId() {
        return spreadsheetId;
    }

    public void setSpreadsheetId(String spreadsheetId) {
        this.spreadsheetId = spreadsheetId;
    }

    public String getMongodbHost() {
        return mongodbHost;
    }

    public void setMongodbHost(String mongodbHost) {
        this.mongodbHost = mongodbHost;
    }

    public String getMongodbName() {
        return mongodbName;
    }

    public void setMongodbName(String mongodbName) {
        this.mongodbName = mongodbName;
    }

    public String getSmtpServer() {
        return smtpServer;
    }

    public void setSmtpServer(String smtpServer) {
        this.smtpServer = smtpServer;
    }

    public String[] getNotifyTo() {
        return notifyTo;
    }

    public void setNotifyTo(String[] notifyTo) {
        this.notifyTo = notifyTo;
    }
}
