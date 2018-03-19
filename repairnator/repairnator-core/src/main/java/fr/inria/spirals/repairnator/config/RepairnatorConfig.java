package fr.inria.spirals.repairnator.config;

import fr.inria.spirals.repairnator.states.LauncherMode;

import java.io.File;
import java.time.Duration;
import java.util.Date;

/**
 * Created by urli on 08/03/2017.
 */
public class RepairnatorConfig {
    private String runId;
    private LauncherMode launcherMode;

    private boolean serializeJson;
    private String inputPath;
    private String outputPath;
    private String mongodbHost;
    private String mongodbName;
    private String spreadsheetId;
    private String googleSecretPath;
    private String googleAccessToken;
    private String smtpServer;
    private String[] notifyTo;
    private boolean notifyEndProcess;
    private boolean push;
    private String pushRemoteRepo;
    private boolean fork;

    // Scanner
    private Date lookFromDate;
    private Date lookToDate;

    // Pipeline
    private int buildId;
    private String z3solverPath;
    private String workspacePath;
    private String githubLogin;
    private String githubToken;

    // Dockerpool
    private String dockerImageName;
    private boolean skipDelete;
    private boolean createOutputDir;
    private String logDirectory;
    private int nbThreads;
    private int globalTimeout;

    // Realtime
    private File whiteList;
    private File blackList;
    private int jobSleepTime;
    private int buildSleepTime;
    private int maxInspectedBuilds;
    private Duration duration;

    // Checkbranches
    private boolean humanPatch;
    private String repository;

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

    public String getInputPath() {
        return inputPath;
    }

    public void setInputPath(String inputPath) {
        this.inputPath = inputPath;
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

    public String getGoogleSecretPath() {
        return googleSecretPath;
    }

    public void setGoogleSecretPath(String googleSecretPath) {
        this.googleSecretPath = googleSecretPath;
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

    public boolean isNotifyEndProcess() {
        return notifyEndProcess;
    }

    public void setNotifyEndProcess(boolean notifyEndProcess) {
        this.notifyEndProcess = notifyEndProcess;
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

    public int getBuildId() {
        return buildId;
    }

    public void setBuildId(int buildId) {
        this.buildId = buildId;
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

    public Date getLookFromDate() {
        return lookFromDate;
    }

    public void setLookFromDate(Date lookFromDate) {
        this.lookFromDate = lookFromDate;
    }

    public Date getLookToDate() {
        return lookToDate;
    }

    public void setLookToDate(Date lookToDate) {
        this.lookToDate = lookToDate;
    }

    public String getDockerImageName() {
        return dockerImageName;
    }

    public void setDockerImageName(String dockerImageName) {
        this.dockerImageName = dockerImageName;
    }

    public boolean isSkipDelete() {
        return skipDelete;
    }

    public void setSkipDelete(boolean skipDelete) {
        this.skipDelete = skipDelete;
    }

    public boolean isCreateOutputDir() {
        return createOutputDir;
    }

    public void setCreateOutputDir(boolean createOutputDir) {
        this.createOutputDir = createOutputDir;
    }

    public String getLogDirectory() {
        return logDirectory;
    }

    public void setLogDirectory(String logDirectory) {
        this.logDirectory = logDirectory;
    }

    public int getNbThreads() {
        return nbThreads;
    }

    public void setNbThreads(int nbThreads) {
        this.nbThreads = nbThreads;
    }

    public int getGlobalTimeout() {
        return globalTimeout;
    }

    public void setGlobalTimeout(int globalTimeout) {
        this.globalTimeout = globalTimeout;
    }

    public File getWhiteList() {
        return whiteList;
    }

    public void setWhiteList(File whiteList) {
        this.whiteList = whiteList;
    }

    public File getBlackList() {
        return blackList;
    }

    public void setBlackList(File blackList) {
        this.blackList = blackList;
    }

    public int getJobSleepTime() {
        return jobSleepTime;
    }

    public void setJobSleepTime(int jobSleepTime) {
        this.jobSleepTime = jobSleepTime;
    }

    public int getBuildSleepTime() {
        return buildSleepTime;
    }

    public void setBuildSleepTime(int buildSleepTime) {
        this.buildSleepTime = buildSleepTime;
    }

    public int getMaxInspectedBuilds() {
        return maxInspectedBuilds;
    }

    public void setMaxInspectedBuilds(int maxInspectedBuilds) {
        this.maxInspectedBuilds = maxInspectedBuilds;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public boolean isHumanPatch() {
        return humanPatch;
    }

    public void setHumanPatch(boolean humanPatch) {
        this.humanPatch = humanPatch;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
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
