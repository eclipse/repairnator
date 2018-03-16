package fr.inria.spirals.repairnator.config;

import fr.inria.spirals.repairnator.states.LauncherMode;

/**
 * Created by urli on 08/03/2017.
 */
public class RepairnatorConfig {
    private String runId;
    private LauncherMode launcherMode;

    private boolean serializeJson;
    private String outputPath;
    private String mongodbHost;
    private String mongodbName;
    private String spreadsheetId;
    private String googleAccessToken;
    private String smtpServer;
    private String[] notifyTo;
    private boolean push;
    private String pushRemoteRepo;
    private boolean fork;
    private String z3solverPath;
    private String workspacePath;
    private String githubLogin;
    private String githubToken;

    private boolean clean;

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

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public LauncherMode getLauncherMode() {
        return launcherMode;
    }

    public void setLauncherMode(LauncherMode launcherMode) {
        this.launcherMode = launcherMode;
    }

    public boolean isSerializeJson() {
        return serializeJson;
    }

    public void setSerializeJson(boolean serializeJson) {
        this.serializeJson = serializeJson;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
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

    public String getSpreadsheetId() {
        return spreadsheetId;
    }

    public void setSpreadsheetId(String spreadsheetId) {
        this.spreadsheetId = spreadsheetId;
    }

    public String getGoogleAccessToken() {
        return googleAccessToken;
    }

    public void setGoogleAccessToken(String googleAccessToken) {
        this.googleAccessToken = googleAccessToken;
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

    public boolean isPush() {
        return push;
    }

    public void setPush(boolean push) {
        this.push = push;
    }

    public String getPushRemoteRepo() {
        return pushRemoteRepo;
    }

    public void setPushRemoteRepo(String pushRemoteRepo) {
        this.pushRemoteRepo = pushRemoteRepo;
    }

    public boolean isFork() {
        return fork;
    }

    public void setFork(boolean fork) {
        this.fork = fork;
    }

    public String getZ3solverPath() {
        return z3solverPath;
    }

    public void setZ3solverPath(String z3solverPath) {
        this.z3solverPath = z3solverPath;
    }

    public String getWorkspacePath() {
        return workspacePath;
    }

    public void setWorkspacePath(String workspacePath) {
        this.workspacePath = workspacePath;
    }

    public String getGithubLogin() {
        return githubLogin;
    }

    public void setGithubLogin(String githubLogin) {
        this.githubLogin = githubLogin;
    }

    public String getGithubToken() {
        return githubToken;
    }

    public void setGithubToken(String githubToken) {
        this.githubToken = githubToken;
    }

    public boolean isClean() {
        return clean;
    }

    public void setClean(boolean clean) {
        this.clean = clean;
    }

    public static void setInstance(RepairnatorConfig instance) {
        RepairnatorConfig.instance = instance;
    }
}
