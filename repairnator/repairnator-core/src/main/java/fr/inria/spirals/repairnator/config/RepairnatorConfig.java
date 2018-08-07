package fr.inria.spirals.repairnator.config;

import fr.inria.jtravis.JTravis;
import fr.inria.spirals.repairnator.states.BearsMode;
import fr.inria.spirals.repairnator.states.LauncherMode;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.github.GitHub;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by urli on 08/03/2017.
 */
public class RepairnatorConfig {
    private String runId;
    private LauncherMode launcherMode = LauncherMode.REPAIR;

    private boolean serializeJson;
    private String inputPath;
    private String outputPath;
    private String mongodbHost;
    private String mongodbName;
    private String smtpServer;
    private String[] notifyTo;
    private boolean notifyEndProcess;
    private boolean push;
    private String pushRemoteRepo;
    private boolean fork;
    private boolean createPR;
    private boolean debug;

    // Scanner
    private Date lookFromDate;
    private Date lookToDate;
    private BearsMode bearsMode = BearsMode.BOTH;
    private boolean bearsDelimiter;

    // Pipeline
    private int buildId;
    private int nextBuildId;
    private String z3solverPath;
    private String workspacePath;
    private String githubToken;
    private String projectsToIgnoreFilePath;
    private Set<String> repairTools;
    private String githubUserName;
    private String githubUserEmail;

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

    private RepairnatorConfig() {
        this.repairTools = new HashSet<>();
    }

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

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public int getBuildId() {
        return buildId;
    }

    public void setBuildId(int buildId) {
        this.buildId = buildId;
    }

    public int getNextBuildId() {
        return nextBuildId;
    }

    public void setNextBuildId(int nextBuildId) {
        this.nextBuildId = nextBuildId;
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

    public String getGithubToken() {
        return githubToken;
    }

    public void setGithubToken(String githubToken) {
        this.githubToken = githubToken;
    }

    public String getProjectsToIgnoreFilePath() {
        return projectsToIgnoreFilePath;
    }

    public void setProjectsToIgnoreFilePath(String projectsToIgnoreFilePath) {
        this.projectsToIgnoreFilePath = projectsToIgnoreFilePath;
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

    public GitHub getGithub() throws IOException {
        return this.getJTravis().getGithub();
    }

    public JTravis getJTravis() {
        return JTravis.builder().setGithubToken(this.getGithubToken()).build();
    }

    public BearsMode getBearsMode() {
        return bearsMode;
    }

    public void setBearsMode(BearsMode bearsMode) {
        this.bearsMode = bearsMode;
    }

    public Set<String> getRepairTools() {
        return repairTools;
    }

    public void setRepairTools(Set<String> repairTools) {
        this.repairTools = repairTools;
    }

    public boolean isBearsDelimiter() {
        return bearsDelimiter;
    }

    public void setBearsDelimiter(boolean bearsDelimiter) {
        this.bearsDelimiter = bearsDelimiter;
    }

    public String getGithubUserName() {
        return githubUserName;
    }

    public void setGithubUserName(String githubUserName) {
        this.githubUserName = githubUserName;
    }

    public String getGithubUserEmail() {
        return githubUserEmail;
    }

    public void setGithubUserEmail(String githubUserEmail) {
        this.githubUserEmail = githubUserEmail;
    }

    public boolean isCreatePR() {
        return createPR;
    }

    public void setCreatePR(boolean createPR) {
        this.createPR = createPR;
    }

    @Override
    public String toString() {
        String ghToken = this.getGithubToken();
        if (ghToken != null && !ghToken.isEmpty()) {
            ghToken = (ghToken.length() > 10) ? ghToken.substring(0,10)+"[...]" : ghToken;
        }
        String mongoDbInfo = this.getMongodbHost();
        if (mongoDbInfo != null && !mongoDbInfo.isEmpty()) {
            int indexOfArobase = mongoDbInfo.indexOf('@');
            if (indexOfArobase != -1) {
                mongoDbInfo = "mongodb://[hidden]" + mongoDbInfo.substring(indexOfArobase);
            }
        }

        return "RepairnatorConfig{" +
                "runId='" + runId + '\'' +
                ", launcherMode=" + launcherMode +
                ", serializeJson=" + serializeJson +
                ", inputPath='" + inputPath + '\'' +
                ", outputPath='" + outputPath + '\'' +
                ", mongodbHost='" + mongoDbInfo + '\'' +
                ", mongodbName='" + mongodbName + '\'' +
                ", smtpServer='" + smtpServer + '\'' +
                ", notifyTo=" + Arrays.toString(notifyTo) +
                ", notifyEndProcess=" + notifyEndProcess +
                ", push=" + push +
                ", pushRemoteRepo='" + pushRemoteRepo + '\'' +
                ", fork=" + fork +
                ", lookFromDate=" + lookFromDate +
                ", lookToDate=" + lookToDate +
                ", buildId=" + buildId +
                ", z3solverPath='" + z3solverPath + '\'' +
                ", workspacePath='" + workspacePath + '\'' +
                ", githubToken='" + ghToken + '\'' +
                ", dockerImageName='" + dockerImageName + '\'' +
                ", skipDelete=" + skipDelete +
                ", createOutputDir=" + createOutputDir +
                ", logDirectory='" + logDirectory + '\'' +
                ", nbThreads=" + nbThreads +
                ", globalTimeout=" + globalTimeout +
                ", whiteList=" + whiteList +
                ", blackList=" + blackList +
                ", jobSleepTime=" + jobSleepTime +
                ", buildSleepTime=" + buildSleepTime +
                ", maxInspectedBuilds=" + maxInspectedBuilds +
                ", duration=" + duration +
                ", humanPatch=" + humanPatch +
                ", repository='" + repository + '\'' +
                ", clean=" + clean +
                ", bearsMode=" + bearsMode.name() +
                ", bearsDelimiter = " + bearsDelimiter +
                ", repairTools=" + StringUtils.join(this.repairTools, ",") +
                ", githubUserName= " + githubUserName +
                ", githubUserEmail=" + githubUserEmail +
                '}';
    }
}
