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
    public enum PIPELINE_MODE {
        DOCKER("fr.inria.spirals.repairnator.realtime.DockerPipelineRunner"),
        KUBERNETES("fr.inria.spirals.repairnator.realtime.ActiveMQPipelineRunner"),
        NOOP("fr.inria.spirals.repairnator.realtime.NoopRunner");

        private final String klass;

        PIPELINE_MODE(String s) {
            this.klass = s;
        }

        public String getKlass() {
            return klass;
        }
    }

    public enum LISTENER_MODE {
        KUBERNETES("fr.inria.spirals.repairnator.pipeline.PipelineBuildListener"),
        NOOP("fr.inria.spirals.repairnator.pipeline.NoopListener");

        private final String klass;

        LISTENER_MODE(String s) {
            this.klass = s;
        }

        public String getKlass() {
            return klass;
        }
    }

    public enum PATCH_CLASSIFICATION_MODE {
        NONE,
        ODS
    }

    public enum PATCH_FILTERING_MODE {
        NONE,
        ODS_CORRECT
    }
   
    private String runId;
    private LauncherMode launcherMode = LauncherMode.REPAIR;

    private String inputPath;
    private String outputPath;
    private String mongodbHost;
    private String mongodbName;
    private String smtpServer;
    private int smtpPort = 25;
    private boolean smtpTLS;
    private String smtpUsername;
    private String smtpPassword;
    private String[] notifyTo;
    private boolean notifyEndProcess;
    private boolean push;
    private String pushRemoteRepo;
    private boolean fork;
    private boolean createPR;
    private boolean debug;
    private boolean noTravisRepair;

    // Scanner
    private Date lookFromDate;
    private Date lookToDate;
    private BearsMode bearsMode = BearsMode.BOTH;
    private boolean bearsDelimiter;
    private String activeMQListenQueueName;

    // Pipeline
    private int buildId;
    private int nextBuildId;
    private String z3solverPath;
    private String workspacePath;
    private boolean tempWorkspace;
    private String githubToken;
    private String projectsToIgnoreFilePath;
    private Set<String> repairTools;
    private String githubUserName;
    private String githubUserEmail;
    private String[] experimentalPluginRepoList;
    private LISTENER_MODE listenerMode;
    private String gitUrl;
    private String gitBranch;
    private String gitCommitHash;
    private String mavenHome;
    private String localMavenRepository;
    private String jTravisEndpoint;
    private String travisToken;
    private String ODSPath;

    private String gitRepositoryUrl;
    private String gitRepositoryBranch;
    private String gitRepositoryIdCommit;
    private boolean gitRepositoryFirstCommit;

    private String[] sonarRules;
    private boolean isStaticAnalysis;
    private boolean measureSoraldTime;

    private String npeSelection;
    private Integer npeNbIteration;
    private String npeScope;
    private String npeRepairStrategy;

    private Double flacocoThreshold;

    private PATCH_CLASSIFICATION_MODE patchClassificationMode;
    private PATCH_FILTERING_MODE patchFilteringMode;
    private boolean patchClassification;
    private boolean patchFiltering;

    // Dockerpool
    private String dockerImageName;
    private boolean skipDelete;
    private boolean createOutputDir;
    private String logDirectory;
    private int nbThreads = 1; // safe default value
    private int globalTimeout;

    // Realtime
    private File whiteList;
    private File blackList;
    private int jobSleepTime;
    private int buildSleepTime;
    private int maxInspectedBuilds;
    private Duration duration;
    private Duration summaryFrequency;
    private String[] notifySummary;
    private int numberOfPatchedBuilds;
    private int numberOfPRs;
  
    private PIPELINE_MODE pipelineMode;
    private String activeMQUrl;
    private String activeMQSubmitQueueName;
    private String activeMQUsername;
    private String activeMQPassword;
  
    // BuildRainer
    private String webSocketUrl;
    private String jmxHostName;
    private int queueLimit;


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
    
    public void setListenerMode(String listenerMode) {
        for (LISTENER_MODE mode: LISTENER_MODE.values()) {
            if (listenerMode.equals(mode.name())) {
                this.listenerMode = LISTENER_MODE.valueOf(listenerMode);
                return;
            }
        }
        throw new RuntimeException("unknown listener "+listenerMode);
    }

    public LISTENER_MODE getListenerMode() {
        return this.listenerMode;
    }

    public void setPipelineMode(String pipelineMode) {
        for (PIPELINE_MODE mode: PIPELINE_MODE.values()) {
            if (pipelineMode.equals(mode.name())) {
                this.pipelineMode = PIPELINE_MODE.valueOf(pipelineMode);
                return;
            }
        }
        throw new RuntimeException("unknown pipeline "+pipelineMode);
    }

    public PIPELINE_MODE getPipelineMode() {
        return this.pipelineMode;
    }

    public void setActiveMQUrl(String activeMQUrl) {
        this.activeMQUrl = activeMQUrl;
    }

    public String getActiveMQUrl() {
        return this.activeMQUrl;
    }

    public void setActiveMQSubmitQueueName(String activeMQSubmitQueueName) {
        this.activeMQSubmitQueueName = activeMQSubmitQueueName;
    }

    public String getActiveMQSubmitQueueName() {
        return this.activeMQSubmitQueueName;
    }

    public void setActiveMQListenQueueName(String activeMQListenQueueName) {
        this.activeMQListenQueueName = activeMQListenQueueName;
    }

    public String getActiveMQListenQueueName() {
        return this.activeMQListenQueueName;
    }

    public void setActiveMQUsername(String activeMQUsername) {
        this.activeMQUsername = activeMQUsername;
    }

    public String getActiveMQUsername() { return activeMQUsername; }

    public void setActiveMQPassword(String activeMQPassword) {
        this.activeMQPassword = activeMQPassword;
    }

    public String getActiveMQPassword() { return activeMQPassword; }

    public void setWebSocketUrl(String webSocketUrl) {
        this.webSocketUrl = webSocketUrl;
    }

    public String getWebSocketUrl() {
        return this.webSocketUrl;
    }

    public void setJmxHostName(String jmxHostName) {
        this.jmxHostName = jmxHostName;
    }

    public String getJmxHostName() {
        return this.jmxHostName;
    }

    public void setQueueLimit(int queueLimit) {
        this.queueLimit = queueLimit;
    }

    public int getQueueLimit() {
        return this.queueLimit;
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
    
    public int getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(int smtpPort) {
        this.smtpPort = smtpPort;
    }    
    
    public boolean isSmtpTLS() {
        return smtpTLS;
    }

    public void setSmtpTLS(boolean smtpTLS) {
        this.smtpTLS = smtpTLS;
    }    
    
    public String getSmtpUsername() {
        return smtpUsername;
    }
    
    public void setSmtpUsername(String smtpUsername) {
        this.smtpUsername = smtpUsername;
    }
    
    public String getSmtpPassword() {
        return smtpPassword;
    }
    
    public void setSmtpPassword(String smtpPassword) {
        this.smtpPassword = smtpPassword;
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

    public boolean getTempWorkspace() {
        return tempWorkspace;
    }

    public void setTempWorkspace(boolean tempWorkspace) {
        this.tempWorkspace = tempWorkspace;
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
        JTravis.Builder builder = JTravis.builder().setGithubToken(this.getGithubToken());

        if(this.getJTravisEndpoint() != null && !this.getJTravisEndpoint().equals("")) {
            builder.setEndpoint(this.getJTravisEndpoint());
        }
        if(this.getTravisToken() != null && !this.getTravisToken().equals("")) {
            builder.setTravisToken("token " + this.getTravisToken());
        }
        return builder.build();
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


    public void setGitUrl(String gitUrl) {
        this.gitUrl = gitUrl;
    }

    public void setGitBranch(String gitBranch) {
        this.gitBranch = gitBranch;
    }

    public void setGitCommitHash(String gitCommitHash) {
        this.gitCommitHash = gitCommitHash;
    }

    public String getGitUrl() {
        return this.gitUrl;
    }

    public String getGitBranch() {
        return this.gitBranch;
    }

    public String getGitCommitHash() {
        return this.gitCommitHash;
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

    public void setMavenHome(String mavenHome) {
        this.mavenHome = mavenHome;
    }

    public String getMavenHome() {
        return this.mavenHome;
    }

    public String getLocalMavenRepository() {
        if (localMavenRepository == null) {
            setLocalMavenRepository(System.getProperty("user.home") + "/.m2/repository");
        }
        return localMavenRepository;
    }

    public void setLocalMavenRepository(String localMavenRepository) {
        this.localMavenRepository = localMavenRepository;
    }

    public boolean isCreatePR() {
        return createPR;
    }

    public void setCreatePR(boolean createPR) {
        this.createPR = createPR;
    }

    public void setNoTravisRepair(boolean noTravisRepair) {
        this.noTravisRepair = noTravisRepair;
    }

    public boolean isNoTravisRepair() {
        return this.noTravisRepair;
    }

    public void setSonarRules(String[] sonarRules) {
        this.sonarRules = sonarRules;
    }

    public String[] getSonarRules() {
        return this.sonarRules;
    }
        
    public void setIsStaticAnalysis(boolean isStaticAnalysis) {
        this.isStaticAnalysis = isStaticAnalysis;
    }

    public boolean isStaticAnalysis() {
        return this.isStaticAnalysis;
    }

    public String getNPESelection() {
        return npeSelection;
    }

    public void setNPESelection(String npeSelection) {
        this.npeSelection = npeSelection;
    }

    public Integer getNPENbIteration() {
        return npeNbIteration;
    }

    public void setNPENbIteration(Integer npeNbIteration) {
        this.npeNbIteration = npeNbIteration;
    }

    public String getNPEScope() {
        return npeScope;
    }

    public void setNPEScope(String npeScope) {
        this.npeScope = npeScope;
    }

    public String getNPERepairStrategy() {
        return npeRepairStrategy;
    }

    public void setNPERepairStrategy(String npeRepairStrategy) {
        this.npeRepairStrategy = npeRepairStrategy;
    }

    public Double getFlacocoThreshold() {
        return flacocoThreshold;
    }

    public void setFlacocoThreshold(Double flacocoThreshold) {
        this.flacocoThreshold = flacocoThreshold;
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
        // In case we have a password, print out stars, if there is no password print nothing
        String smtpPass = "";
        if(smtpPassword != null) smtpPass = "*****";

        return "RepairnatorConfig{" +
                "runId='" + runId + '\'' +
                ", launcherMode=" + launcherMode +
                ", inputPath='" + inputPath + '\'' +
                ", outputPath='" + outputPath + '\'' +
                ", mongodbHost='" + mongoDbInfo + '\'' +
                ", mongodbName='" + mongodbName + '\'' +
                ", smtpServer='" + smtpServer + '\'' +
                ", smtpPort='" + smtpPort + '\'' +
                ", smtpTLS='" + smtpTLS + '\'' +
                ", smtpUsername='" + smtpUsername + '\'' +
                ", smtpPassword='" + smtpPass + '\'' +
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
                ", pipelineMode=" + pipelineMode +
                ", listenerMode=" + listenerMode +
                ", activeMQUrl=" + activeMQUrl +
                ", activeMQSubmitQueueName=" + activeMQSubmitQueueName +
                ", gitUrl=" + gitUrl +
                ", gitBranch=" + gitBranch +
                ", gitCommitHash=" + gitCommitHash +
                ", mavenHome=" + mavenHome +
                ", localMavenRepository=" + localMavenRepository +
                ", noTravisRepair=" + noTravisRepair +
                ", jTravisEndpoint=" + jTravisEndpoint +
                ", travisToken=" + travisToken +
                ", flacocoThreshold=" + flacocoThreshold +
                '}';
    }

    public Duration getSummaryFrequency() {
        return summaryFrequency;
    }

    public void setSummaryFrequency(Duration summaryFrequency) {
        this.summaryFrequency = summaryFrequency;
    }

    public String[] getNotifySummary() {
        return notifySummary;
    }

    public void setNotifySummary(String[] notifySummary) {
        this.notifySummary = notifySummary;
    }

    public int getNumberOfPRs() {
        return numberOfPRs;
    }

    public void setNumberOfPRs(int numberOfPRs) {
        this.numberOfPRs = numberOfPRs;
    }

    public String[] getExperimentalPluginRepoList() {
        return experimentalPluginRepoList;
    }

    public void setExperimentalPluginRepoList(String[] experimentalPluginRepoList) {
        this.experimentalPluginRepoList = experimentalPluginRepoList;
    }

    public int getNumberOfPatchedBuilds() {
        return numberOfPatchedBuilds;
    }

    public void setNumberOfPatchedBuilds(int numberOfPatchedBuilds) {
        this.numberOfPatchedBuilds = numberOfPatchedBuilds;
    }

	public String getGitRepositoryUrl() {
		return gitRepositoryUrl;
	}

	public void setGitRepositoryUrl(String gitRepositoryUrl) {
		this.gitRepositoryUrl = gitRepositoryUrl;
	}

	public String getGitRepositoryBranch() {
		return gitRepositoryBranch;
	}

	public void setGitRepositoryBranch(String gitRepositoryBranch) {
		this.gitRepositoryBranch = gitRepositoryBranch;
	}

	public String getGitRepositoryIdCommit() {
		return this.gitRepositoryIdCommit;
	}

	public void setGitRepositoryIdCommit(String gitRepositoryIdCommit) {
		this.gitRepositoryIdCommit = gitRepositoryIdCommit;
	}

	public boolean isGitRepositoryFirstCommit() {
		return gitRepositoryFirstCommit;
	}

	public void setGitRepositoryFirstCommit(boolean gitRepositoryFirstCommit) {
		this.gitRepositoryFirstCommit = gitRepositoryFirstCommit;
	}

	public String getGitRepositoryId() {
		return getGitRepositoryUrl().split("https://github.com/",2)[1].replace(".git","").replace("/", "-") + "-" + (getGitRepositoryBranch() != null ? getGitRepositoryBranch() : "master") +
				(getGitRepositoryIdCommit() != null ? "-" + getGitRepositoryIdCommit() : "") +
				(isGitRepositoryFirstCommit() ? "-firstCommit" : "");
    }

    public String getJTravisEndpoint() {
        return jTravisEndpoint;
    }

    public void setJTravisEndpoint(String jTravisEndpoint) {
        this.jTravisEndpoint = jTravisEndpoint;
    }

    public String getTravisToken() {
        return travisToken;
    }

    public void setTravisToken(String travisToken) {
        this.travisToken = travisToken;
    }

    public String getODSPath() {
        return ODSPath;
    }

    public void setODSPath(String ODSPath) {
        this.ODSPath = ODSPath;
    }

    public PATCH_CLASSIFICATION_MODE getPatchClassificationMode() {
        return patchClassificationMode;
    }

    public void setPatchClassificationMode(PATCH_CLASSIFICATION_MODE patchClassificationMode) {
        this.patchClassificationMode = patchClassificationMode;
    }

    public boolean isPatchClassification() {
        return patchClassification;
    }

    public void setPatchClassification(boolean patchClassification) {
        this.patchClassification = patchClassification;
    }

    public PATCH_FILTERING_MODE getPatchFilteringMode() {
        return patchFilteringMode;
    }

    public void setPatchFilteringMode(PATCH_FILTERING_MODE patchFilteringMode) {
        this.patchFilteringMode = patchFilteringMode;
    }

    public boolean isPatchFiltering() {
        return patchFiltering;
    }

    public void setPatchFiltering(boolean patchFiltering) {
        this.patchFiltering = patchFiltering;
    }
}
