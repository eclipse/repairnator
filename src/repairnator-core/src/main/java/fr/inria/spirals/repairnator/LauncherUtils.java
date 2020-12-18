package fr.inria.spirals.repairnator;

import ch.qos.logback.classic.Level;
import com.martiansoftware.jsap.*;
import com.martiansoftware.jsap.stringparsers.FileStringParser;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.engines.EmailNotifierEngine;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.json.JSONFileSerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.json.MongoDBSerializerEngine;
import fr.inria.spirals.repairnator.serializer.engines.table.CSVSerializerEngine;
import fr.inria.spirals.repairnator.serializer.mongodb.MongoConnection;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.spirals.repairnator.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by fermadeiral
 */
public class LauncherUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(LauncherUtils.class);

    public static void registerCommonArgs(JSAP jsap) throws JSAPException {
        // -h or --help
        jsap.registerParameter(LauncherUtils.defineArgHelp());
        // -d or --debug
        jsap.registerParameter(LauncherUtils.defineArgDebug());
        // -o or --output
        jsap.registerParameter(LauncherUtils.defineArgOutput(LauncherType.PIPELINE, "Specify path to output serialized files"));

        // --runId
        jsap.registerParameter(LauncherUtils.defineArgRunId());

        // --dbhost
        jsap.registerParameter(LauncherUtils.defineArgMongoDBHost());
        // --dbname
        jsap.registerParameter(LauncherUtils.defineArgMongoDBName());

        // --smtpServer
        jsap.registerParameter(LauncherUtils.defineArgSmtpServer());
        // --smtpPort
        jsap.registerParameter(LauncherUtils.defineArgSmtpPort());
        // --smtpTLS
        jsap.registerParameter(LauncherUtils.defineArgSmtpTLS());
        // --smtpUsername
        jsap.registerParameter(LauncherUtils.defineArgSmtpUsername());
        // --smtpPassword
        jsap.registerParameter(LauncherUtils.defineArgSmtpPassword());

        // --notifyto
        jsap.registerParameter(LauncherUtils.defineArgNotifyto());

        // --pushurl
        jsap.registerParameter(LauncherUtils.defineArgPushUrl());
        // --ghOauth
        jsap.registerParameter(LauncherUtils.defineArgGithubOAuth());
        // --githubUserName
        jsap.registerParameter(LauncherUtils.defineArgGithubUserName());
        // --githubUserEmail
        jsap.registerParameter(LauncherUtils.defineArgGithubUserEmail());
        // --createPR
        jsap.registerParameter(LauncherUtils.defineArgCreatePR());

        // --z3
        jsap.registerParameter(LauncherUtils.defineArgZ3());

        // --mavenHome
        jsap.registerParameter(LauncherUtils.defineArgMavenHome());
        // --localMavenRepository
        jsap.registerParameter(LauncherUtils.defineArgLocalMavenRepository());

        // --workspace
        jsap.registerParameter(LauncherUtils.defineArgWorkspace());

        // --projectsToIgnore
        jsap.registerParameter(LauncherUtils.defineArgProjectsToIgnore());

        // --listenerMode
        jsap.registerParameter(LauncherUtils.defineArgListenerMode());

        // --activemqurl
        jsap.registerParameter(LauncherUtils.defineArgActiveMQUrl());
        // --activemqlistenqueuename
        jsap.registerParameter(LauncherUtils.defineArgActiveMQListEnqueueName());
        // --activemqusername
        jsap.registerParameter(LauncherUtils.defineArgActiveMQUsername());
        // --activemqpassword
        jsap.registerParameter(LauncherUtils.defineArgActiveMQPassword());

        // --giturl
        jsap.registerParameter(LauncherUtils.defineArgGitUrl());
        // --gitbranch
        jsap.registerParameter(LauncherUtils.defineArgGitBranch());
        // --gitcommithash
        jsap.registerParameter(LauncherUtils.defineArgGitCommitHash());

        // --experimentalPluginRepoList
        jsap.registerParameter(LauncherUtils.defineArgExperimentalPluginRepoList());

        // --tmpDirAsWorkSpace
        jsap.registerParameter(LauncherUtils.defineArgTmpDirAsWorkSpace());

        // --sonarRules
        jsap.registerParameter(LauncherUtils.defineArgSonarRules());
        // --soraldRepairMode
        jsap.registerParameter(LauncherUtils.defineArgSoraldRepairMode());
        // --segmentSize
        jsap.registerParameter(LauncherUtils.defineArgSegmentSize());
        // --soraldMaxFixesPerRule
        jsap.registerParameter(LauncherUtils.defineArgSoraldMaxFixesPerRule());

        // --bears
        jsap.registerParameter(LauncherUtils.defineArgBearsMode());
        // --checkstyle
        jsap.registerParameter(LauncherUtils.defineArgCheckstyleMode());
        // --sequencerRepair
        jsap.registerParameter(LauncherUtils.defineArgSequencerRepairMode());

        // --patchRankingMode
        jsap.registerParameter(LauncherUtils.defineArgPatchRankingMode());
    }

    public static void initCommonConfig(RepairnatorConfig config, JSAPResult arguments) {
        if (LauncherUtils.getArgBearsMode(arguments)) {
            config.setLauncherMode(LauncherMode.BEARS);
        } else if (LauncherUtils.getArgCheckstyleMode(arguments)) {
            config.setLauncherMode(LauncherMode.CHECKSTYLE);
        } else if (LauncherUtils.getArgSequencerRepairMode(arguments)) {
            config.setLauncherMode(LauncherMode.SEQUENCER_REPAIR);
        } else {
            config.setLauncherMode(LauncherMode.REPAIR);
        }

        if (LauncherUtils.getArgDebug(arguments)) {
            config.setDebug(true);
            Utils.setLoggersLevel(Level.DEBUG);
        } else {
            Utils.setLoggersLevel(Level.INFO);
        }
        config.setClean(true);
        config.setRunId(LauncherUtils.getArgRunId(arguments));
        config.setGithubToken(LauncherUtils.getArgGithubOAuth(arguments));

        config.setOutputPath(LauncherUtils.getArgOutput(arguments).getAbsolutePath());
        config.setMongodbHost(LauncherUtils.getArgMongoDBHost(arguments));
        config.setMongodbName(LauncherUtils.getArgMongoDBName(arguments));
        config.setSmtpServer(LauncherUtils.getArgSmtpServer(arguments));
        config.setSmtpPort(LauncherUtils.getArgSmtpPort(arguments));
        config.setSmtpTLS(LauncherUtils.getArgSmtpTLS(arguments));
        config.setSmtpUsername(LauncherUtils.getArgSmtpUsername(arguments));
        config.setSmtpPassword(LauncherUtils.getArgSmtpPassword(arguments));
        config.setNotifyTo(LauncherUtils.getArgNotifyto(arguments));

        if (LauncherUtils.getArgPushUrl(arguments) != null) {
            config.setPush(true);
            config.setPushRemoteRepo(LauncherUtils.getArgPushUrl(arguments));
        }
        config.setCreatePR(LauncherUtils.getArgCreatePR(arguments));

        // we fork if we need to create a PR or if we need to notify (but only when we have a git token)
        if (config.isCreatePR() || (config.getSmtpServer() != null && !config.getSmtpServer().isEmpty() && config.getNotifyTo() != null && config.getNotifyTo().length > 0 && config.getGithubToken() != null)) {
            config.setFork(true);
        }

        config.setZ3solverPath(new File(LauncherUtils.getArgZ3(arguments)).getAbsolutePath());
        config.setWorkspacePath(LauncherUtils.getArgWorkspace(arguments));
        if (LauncherUtils.getArgTmpDirAsWorkSpace(arguments)) {
            File tempDir = com.google.common.io.Files.createTempDir();
            config.setTempWorkspace(true);
            config.setWorkspacePath(tempDir.getAbsolutePath());
            config.setOutputPath(tempDir.getAbsolutePath());
            config.setZ3solverPath(new File(tempDir.getAbsolutePath() + File.separator + "z3_for_linux").getAbsolutePath());
        }

        config.setGithubUserEmail(LauncherUtils.getArgGithubUserEmail(arguments));
        config.setGithubUserName(LauncherUtils.getArgGithubUserName(arguments));
        config.setListenerMode(LauncherUtils.getArgListenerMode(arguments));
        config.setActiveMQUrl(LauncherUtils.getArgActiveMQUrl(arguments));
        config.setActiveMQListenQueueName(LauncherUtils.getArgActiveMQListEnqueueName(arguments));
        config.setActiveMQUsername(LauncherUtils.getArgActiveMQUsername(arguments));
        config.setActiveMQPassword(LauncherUtils.getArgActiveMQPassword(arguments));

        config.setGitUrl(LauncherUtils.getArgGitUrl(arguments));
        config.setGitBranch(LauncherUtils.getArgGitBranch(arguments));
        config.setGitCommitHash(LauncherUtils.getArgGitCommitHash(arguments));

        config.setMavenHome(LauncherUtils.getArgMavenHome(arguments).getAbsolutePath());
        config.setLocalMavenRepository(LauncherUtils.getArgLocalMavenRepository(arguments).getAbsolutePath());

        if (LauncherUtils.getArgProjectsToIgnore(arguments) != null) {
            config.setProjectsToIgnoreFilePath(new File(LauncherUtils.getArgProjectsToIgnore(arguments).getAbsolutePath()).getPath());
        }

        // Make sure that it is a multiple of three in the list
        if (LauncherUtils.getArgExperimentalPluginRepoList(arguments).length % 3 == 0) {
            config.setExperimentalPluginRepoList(LauncherUtils.getArgExperimentalPluginRepoList(arguments));
        } else if (LauncherUtils.getArgExperimentalPluginRepoList(arguments).length != 0) {
            LOGGER.warn("The experimental plugin repo list is not correctly formed."
                    + " Please make sure you have provided id, name and url for all repos. "
                    + "Repairnator will continue without these repos.");
            config.setExperimentalPluginRepoList(null);
        } else {
            config.setExperimentalPluginRepoList(null);
        }

        config.setSonarRules(Arrays.stream(LauncherUtils.getArgSonarRules(arguments).split(",")).distinct().toArray(String[]::new));
        config.setSegmentSize(LauncherUtils.getArgSegmentSize(arguments));
        config.setSoraldRepairMode(RepairnatorConfig.SORALD_REPAIR_MODE.valueOf(LauncherUtils.getArgSoraldRepairMode(arguments)));
        config.setSoraldMaxFixesPerRule(LauncherUtils.getArgSoraldMaxFixesPerRule(arguments));

        config.setPatchRankingMode(LauncherUtils.getArgPatchRankingMode(arguments));
    }

    public static Switch defineArgHelp() {
        Switch sw = new Switch("help");
        sw.setShortFlag('h');
        sw.setLongFlag("help");
        sw.setDefault("false");
        return sw;
    }

    public static boolean getArgHelp(JSAPResult arguments) {
        return arguments.getBoolean("help");
    }

    public static Switch defineArgDebug() {
        Switch sw = new Switch("debug");
        sw.setShortFlag('d');
        sw.setLongFlag("debug");
        sw.setDefault("false");
        return sw;
    }

    public static boolean getArgDebug(JSAPResult arguments) {
        return arguments.getBoolean("debug");
    }

    public static Switch defineArgNotifyEndProcess() {
        Switch sw = new Switch("notifyEndProcess");
        sw.setLongFlag("notifyEndProcess");
        sw.setDefault("false");
        sw.setHelp("Activate the notification when the process ends.");
        return sw;
    }

    public static boolean getArgNotifyEndProcess(JSAPResult arguments) {
        return arguments.getBoolean("notifyEndProcess");
    }

    public static Switch defineArgCreatePR() {
        Switch sw = new Switch("createPR");
        sw.setLongFlag("createPR");
        sw.setDefault("false");
        sw.setHelp("Activate the creation of a Pull Request in case of patch.");
        return sw;
    }

    public static boolean getArgCreatePR(JSAPResult arguments) {
        return arguments.getBoolean("createPR");
    }

    public static Switch defineArgCheckstyleMode() {
        Switch sw = new Switch("checkstyle");
        sw.setLongFlag("checkstyle");
        sw.setDefault("false");
        sw.setHelp("This mode allows to use repairnator to analyze build failing because of checkstyle.");
        return sw;
    }

    public static boolean getArgCheckstyleMode(JSAPResult arguments) {
        return arguments.getBoolean("checkstyle");
    }

    public static Switch defineArgSequencerRepairMode() {
        Switch sw = new Switch("sequencerRepair");
        sw.setLongFlag("sequencerRepair");
        sw.setDefault("false");
        sw.setHelp("This mode allows to use repairnator to run a SequencerRepair specific pipeline.");
        return sw;
    }

    public static boolean getArgSequencerRepairMode(JSAPResult arguments) {
        return arguments.getBoolean("sequencerRepair");
    }

    public static Switch defineArgBearsMode() {
        Switch sw = new Switch("bears");
        sw.setLongFlag("bears");
        sw.setDefault("false");
        sw.setHelp("This mode allows to use repairnator to analyze pairs of bugs and human-produced patches.");
        return sw;
    }

    public static boolean getArgBearsMode(JSAPResult arguments) {
        return arguments.getBoolean("bears");
    }

    public static FlaggedOption defineArgRunId() {
        FlaggedOption opt = new FlaggedOption("runId");
        opt.setLongFlag("runId");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setDefault("1234");
        opt.setHelp("Specify the run id for this launch.");
        return opt;
    }

    public static String getArgRunId(JSAPResult arguments) {
        return arguments.getString("runId");
    }

    public static FlaggedOption defineArgInput() {
        /* ./builds.txt contain one build id per line

            346537408
            347223109
            348887356
            349620528
            351046304
            353774072
            358555930
            361311603
            368867994
            369727490
            369854684
            369859631
            370885458
            371144762
            371488143
            374841318
            376812142

            (this is the list from Expedition 2)
         */
        FlaggedOption opt = new FlaggedOption("input");
        opt.setShortFlag('i');
        opt.setLongFlag("input");
        opt.setDefault("./builds.txt");
        opt.setHelp("Specify the input file containing the list of build ids.");
        return opt;
    }

    public static FlaggedOption defineArgProjectInput() {
        /* ./projects.txt contain one slug per line

        INRIA/spoon
        rails/rails
        ....
         */
        FlaggedOption opt = new FlaggedOption("input");
        opt.setShortFlag('i');
        opt.setLongFlag("input");
        opt.setDefault("./projects.txt");
        opt.setHelp("Specify where to find the list of projects to scan.");
        return opt;
    }

    public static FlaggedOption defineArgBranchInput() {
        /* ./branches.txt contain one branch per line
            typically from https://github.com/Spirals-Team/librepair-experiments/
         */
        FlaggedOption opt = new FlaggedOption("input");
        opt.setShortFlag('i');
        opt.setLongFlag("input");
        opt.setDefault("./projects.txt");
        opt.setHelp("Specify the input file containing the list of branches to reproduce (one branch per line)");
        return opt;
    }

    public static File getArgInput(JSAPResult arguments) {
        return arguments.getObject("input") instanceof File ? (File) arguments.getObject("input") : new File(arguments.getString("input"));
    }

    public static FlaggedOption defineArgOutput(LauncherType launcherType, String helpMessage) {
        FlaggedOption opt = new FlaggedOption("output");
        opt.setShortFlag('o');
        opt.setLongFlag("output");
        // we don't assume the presence of "/tmp" (eg on windows) and the it is writable
        opt.setDefault("./repairnator-output");
        if (launcherType == LauncherType.DOCKERPOOL || launcherType == LauncherType.CHECKBRANCHES) {
            opt.setRequired(true);
        }

        opt.setHelp(helpMessage);

        return opt;
    }

    public static File getArgOutput(JSAPResult arguments) {
        File output = new File(arguments.getString("output"));
        if (!output.exists()) {
            output.mkdirs();
        }
        return output;
    }

    public static FlaggedOption defineArgMongoDBHost() {
        FlaggedOption opt = new FlaggedOption("mongoDBHost");
        opt.setLongFlag("dbhost");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Specify mongodb host.");
        return opt;
    }

    public static String getArgMongoDBHost(JSAPResult arguments) {
        return arguments.getString("mongoDBHost");
    }

    public static FlaggedOption defineArgMongoDBName() {
        FlaggedOption opt = new FlaggedOption("mongoDBName");
        opt.setLongFlag("dbname");
        opt.setDefault("repairnator");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Specify mongodb DB name.");
        return opt;
    }

    public static String getArgMongoDBName(JSAPResult arguments) {
        return arguments.getString("mongoDBName");
    }

    public static FlaggedOption defineArgSmtpServer() {
        FlaggedOption opt = new FlaggedOption("smtpServer");
        opt.setLongFlag("smtpServer");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Specify SMTP server to use for Email notification");
        return opt;
    }

    public static String getArgSmtpServer(JSAPResult arguments) {
        return arguments.getString("smtpServer");
    }

    public static FlaggedOption defineArgSmtpPort() {
        FlaggedOption opt = new FlaggedOption("smtpPort");
        opt.setLongFlag("smtpPort");
        opt.setStringParser(JSAP.INTEGER_PARSER);
        opt.setDefault("25");
        opt.setHelp("The port on which to contact the SMTP server. Default 25");
        return opt;
    }

    public static int getArgSmtpPort(JSAPResult arguments) {
        return arguments.getInt("smtpPort");
    }

    public static Switch defineArgSmtpTLS() {
        Switch sw = new Switch("smtpTLS");
        sw.setLongFlag("smtpTLS");
        sw.setDefault("false");
        sw.setHelp("Decides whether to use TLS for email communication.");
        return sw;
    }

    public static boolean getArgSmtpTLS(JSAPResult arguments) {
        return arguments.getBoolean("smtpTLS");
    }

    public static FlaggedOption defineArgSmtpUsername() {
        FlaggedOption opt = new FlaggedOption("smtpUsername");
        opt.setLongFlag("smtpUsername");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Username for authorized server");
        return opt;
    }

    public static String getArgSmtpUsername(JSAPResult arguments) {
        return arguments.getString("smtpUsername");
    }

    public static FlaggedOption defineArgSmtpPassword() {
        FlaggedOption opt = new FlaggedOption("smtpPassword");
        opt.setLongFlag("smtpPassword");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Password for authorized server");
        return opt;
    }

    public static String getArgSmtpPassword(JSAPResult arguments) {
        return arguments.getString("smtpPassword");
    }

    public static FlaggedOption defineArgNotifyto() {
        FlaggedOption opt = new FlaggedOption("notifyto");
        opt.setLongFlag("notifyto");
        opt.setList(true);
        opt.setListSeparator(',');
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Specify email addresses to notify");
        return opt;
    }

    public static String[] getArgNotifyto(JSAPResult arguments) {
        return arguments.getStringArray("notifyto");
    }

    public static Switch defineArgCreateOutputDir() {
        Switch sw = new Switch("createOutputDir");
        sw.setLongFlag("createOutputDir");
        sw.setDefault("false");
        sw.setHelp("Create a specific directory for output.");
        return sw;
    }

    public static boolean getArgCreateOutputDir(JSAPResult arguments) {
        return arguments.getBoolean("createOutputDir");
    }


    public static String getArgLogDirectory(JSAPResult arguments) {
        return arguments.getString("output");
    }

    public static Switch defineArgSkipDelete() {
        Switch sw = new Switch("skipDelete");
        sw.setLongFlag("skipDelete");
        sw.setDefault("false");
        sw.setHelp("Skip the deletion of docker container.");
        return sw;
    }

    public static boolean getArgSkipDelete(JSAPResult arguments) {
        return arguments.getBoolean("skipDelete");
    }

    public static FlaggedOption defineArgNbThreads() {
        FlaggedOption opt = new FlaggedOption("threads");
        opt.setShortFlag('t');
        opt.setLongFlag("threads");
        opt.setStringParser(JSAP.INTEGER_PARSER);
        opt.setDefault("1");
        opt.setHelp("Specify the number of threads to run in parallel");
        return opt;
    }

    public static int getArgNbThreads(JSAPResult arguments) {
        return arguments.getInt("threads");
    }

    public static FlaggedOption defineArgGlobalTimeout() {
        FlaggedOption opt = new FlaggedOption("globalTimeout");
        opt.setShortFlag('g');
        opt.setLongFlag("globalTimeout");
        opt.setStringParser(JSAP.INTEGER_PARSER);
        opt.setDefault("1");
        opt.setHelp("Specify the number of day before killing the whole pool.");
        return opt;
    }

    public static int getArgGlobalTimeout(JSAPResult arguments) {
        return arguments.getInt("globalTimeout");
    }

    public static FlaggedOption defineArgPushUrl() {
        FlaggedOption opt = new FlaggedOption("pushUrl");
        opt.setLongFlag("pushurl");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Specify repository URL to push data on the format https://github.com/user/repo.");
        return opt;
    }

    public static String getArgPushUrl(JSAPResult arguments) {
        return arguments.getString("pushUrl");
    }

    public static FlaggedOption defineArgDockerImageName() {
        FlaggedOption opt = new FlaggedOption("imageName");
        opt.setShortFlag('n');
        opt.setLongFlag("name");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setDefault("repairnator/pipeline:latest");
        opt.setHelp("Specify the docker image name to use.");
        return opt;
    }

    public static String getArgDockerImageName(JSAPResult arguments) {
        return arguments.getString("imageName");
    }

    public static FlaggedOption defineArgGithubOAuth() {
        FlaggedOption opt = new FlaggedOption("ghOauth");
        opt.setLongFlag("ghOauth");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setDefault(System.getenv("GITHUB_OAUTH"));
        opt.setHelp("Specify Github Token to use");
        return opt;
    }

    public static String getArgGithubOAuth(JSAPResult arguments) {
        return arguments.getString("ghOauth");
    }

    public static FlaggedOption defineArgGithubUserName() {
        FlaggedOption opt = new FlaggedOption("githubUserName");
        opt.setLongFlag("githubUserName");
        opt.setDefault("repairnator");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Specify the name of the user who commits");
        return opt;
    }

    public static String getArgGithubUserName(JSAPResult arguments) {
        return arguments.getString("githubUserName");
    }

    public static FlaggedOption defineArgGithubUserEmail() {
        FlaggedOption opt = new FlaggedOption("githubUserEmail");
        opt.setLongFlag("githubUserEmail");
        opt.setDefault("noreply@github.com");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Specify the email of the user who commits");
        return opt;
    }

    public static String getArgGithubUserEmail(JSAPResult arguments) {
        return arguments.getString("githubUserEmail");
    }

    public static void checkArguments(JSAP jsap, JSAPResult arguments, LauncherType launcherType) {
        if (!arguments.success()) {
            // print out specific error messages describing the problems
            for (java.util.Iterator<?> errs = arguments.getErrorMessageIterator(); errs.hasNext(); ) {
                System.err.println("Error: " + errs.next());
            }
        }

        if (getArgHelp(arguments)) {
            printUsage(jsap, launcherType);
        }


        checkPushUrlArg(jsap, arguments, launcherType);
    }

    public static FlaggedOption defineArgZ3() {
        FlaggedOption opt = new FlaggedOption("z3");
        opt.setLongFlag("z3");
        opt.setDefault("./z3_for_linux");
        opt.setHelp("Specify path to Z3");
        return opt;
    }

    public static String getArgZ3(JSAPResult arguments) {
        return arguments.getString("z3");
    }

    public static FlaggedOption defineArgMavenHome() {
        FlaggedOption opt = new FlaggedOption("mavenHome");
        opt.setLongFlag("mavenHome");
        opt.setStringParser(FileStringParser.getParser().setMustBeDirectory(true));
        opt.setHelp("Maven home folder, use in case if environment variable M2_HOME is null");
        opt.setDefault("/usr/share/maven");
        return opt;
    }

    public static File getArgMavenHome(JSAPResult arguments) {
        return arguments.getFile("mavenHome");
    }

    public static FlaggedOption defineArgLocalMavenRepository() {
        FlaggedOption opt = new FlaggedOption("localMavenRepository");
        opt.setLongFlag("localMavenRepository");
        opt.setStringParser(FileStringParser.getParser().setMustBeDirectory(true));
        opt.setHelp("Maven local repository folder");
        opt.setDefault(System.getenv("HOME") + "/.m2/repository");
        return opt;
    }

    public static File getArgLocalMavenRepository(JSAPResult arguments) {
        return arguments.getFile("localMavenRepository");
    }

    public static FlaggedOption defineArgWorkspace() {
        FlaggedOption opt = new FlaggedOption("workspace");
        opt.setLongFlag("workspace");
        opt.setShortFlag('w');
        opt.setDefault("./workspace");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Specify a path to be used by the pipeline at processing things like to clone the project of the repository id being processed");
        return opt;
    }

    public static String getArgWorkspace(JSAPResult arguments) {
        return arguments.getString("workspace");
    }

    public static FlaggedOption defineArgProjectsToIgnore() {
        FlaggedOption opt = new FlaggedOption("projectsToIgnore");
        opt.setLongFlag("projectsToIgnore");
        opt.setStringParser(FileStringParser.getParser().setMustBeFile(true));
        opt.setHelp("Specify the file containing a list of projects that the pipeline should deactivate serialization when processing builds from.");
        return opt;
    }

    public static File getArgProjectsToIgnore(JSAPResult arguments) {
        return arguments.getFile("projectsToIgnore");
    }

    public static FlaggedOption defineArgListenerMode() {
        FlaggedOption opt = new FlaggedOption("listenerMode");
        opt.setLongFlag("listenerMode");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setDefault(RepairnatorConfig.LISTENER_MODE.NOOP.name());
        opt.setHelp("Possible string values KUBERNETES,NOOP . KUBERNETES is for running ActiveMQListener and " + RepairnatorConfig.LISTENER_MODE.NOOP.name() + " is for NoopRunner.");
        return opt;
    }

    public static String getArgListenerMode(JSAPResult arguments) {
        return arguments.getString("listenerMode");
    }

    public static FlaggedOption defineArgActiveMQUrl() {
        FlaggedOption opt = new FlaggedOption("activemqurl");
        opt.setLongFlag("activemqurl");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setDefault("tcp://localhost:61616");
        opt.setHelp("format: 'tcp://IP_OR_DNSNAME:61616', default as 'tcp://localhost:61616'");
        return opt;
    }

    public static String getArgActiveMQUrl(JSAPResult arguments) {
        return arguments.getString("activemqurl");
    }

    public static FlaggedOption defineArgActiveMQListEnqueueName() {
        FlaggedOption opt = new FlaggedOption("activemqlistenqueuename");
        opt.setLongFlag("activemqlistenqueuename");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setDefault("pipeline");
        opt.setHelp("Just a name, default as 'pipeline'");
        return opt;
    }

    public static String getArgActiveMQListEnqueueName(JSAPResult arguments) {
        return arguments.getString("activemqlistenqueuename");
    }

    public static FlaggedOption defineArgActiveMQUsername() {
        FlaggedOption opt = new FlaggedOption("activemqusername");
        opt.setLongFlag("activemqusername");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setDefault("");
        opt.setHelp("The username to access ActiveMQ, which is blank by default");
        return opt;
    }

    public static String getArgActiveMQUsername(JSAPResult arguments) {
        return arguments.getString("activemqusername");
    }

    public static FlaggedOption defineArgActiveMQPassword() {
        FlaggedOption opt = new FlaggedOption("activemqpassword");
        opt.setLongFlag("activemqpassword");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setDefault("");
        opt.setHelp("The password to access ActiveMQ, which is blank by default");
        return opt;
    }

    public static String getArgActiveMQPassword(JSAPResult arguments) {
        return arguments.getString("activemqpassword");
    }

    public static FlaggedOption defineArgGitUrl() {
        FlaggedOption opt = new FlaggedOption("giturl");
        opt.setLongFlag("giturl");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Example: https://github.com/surli/failingProject.git");
        return opt;
    }

    public static String getArgGitUrl(JSAPResult arguments) {
        return arguments.getString("giturl");
    }

    public static FlaggedOption defineArgGitBranch() {
        FlaggedOption opt = new FlaggedOption("gitbranch");
        opt.setLongFlag("gitbranch");
        opt.setStringParser(JSAP.STRING_PARSER);
        return opt;
    }

    public static String getArgGitBranch(JSAPResult arguments) {
        return arguments.getString("gitbranch");
    }

    public static FlaggedOption defineArgGitCommitHash() {
        FlaggedOption opt = new FlaggedOption("gitcommithash");
        opt.setLongFlag("gitcommit");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("the hash of your git commit");
        return opt;
    }

    public static String getArgGitCommitHash(JSAPResult arguments) {
        return arguments.getString("gitcommithash");
    }

    public static FlaggedOption defineArgExperimentalPluginRepoList() {
        // This option will have a list and must have n*3 elements, otherwise the last will be ignored.
        FlaggedOption opt = new FlaggedOption("experimentalPluginRepoList");
        opt.setLongFlag("experimentalPluginRepoList");
        opt.setList(true);
        opt.setListSeparator(',');
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("The ids, names and urls of all experimental pluginrepos used. Must be a list of length n*3 in the order id, name, url, repeat.");
        return opt;
    }

    public static String[] getArgExperimentalPluginRepoList(JSAPResult arguments) {
        return arguments.getStringArray("experimentalPluginRepoList");
    }

    public static Switch defineArgTmpDirAsWorkSpace() {
        Switch sw = new Switch("tmpDirAsWorkSpace");
        sw.setLongFlag("tmpDirAsWorkSpace");
        sw.setDefault("false");
        sw.setHelp("Create tmp directory as workspace");
        return sw;
    }

    public static boolean getArgTmpDirAsWorkSpace(JSAPResult arguments) {
        return arguments.getBoolean("tmpDirAsWorkSpace");
    }

    public static FlaggedOption defineArgSonarRules() {
        FlaggedOption opt = new FlaggedOption("sonarRules");
        opt.setLongFlag("sonarRules");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setDefault("2116");
        opt.setHelp("Required if SonarQube is specified in the repairtools as argument. Format: 1948,1854,RuleNumber.. . Supported rules: https://github.com/kth-tcs/sonarqube-repair/blob/master/docs/HANDLED_RULES.md");
        return opt;
    }

    public static String getArgSonarRules(JSAPResult arguments) {
        return arguments.getString("sonarRules");
    }

    public static FlaggedOption defineArgSoraldRepairMode() {
        FlaggedOption opt = new FlaggedOption("soraldRepairMode");
        opt.setLongFlag("soraldRepairMode");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setDefault(RepairnatorConfig.SORALD_REPAIR_MODE.DEFAULT.name());
        opt.setHelp("DEFAULT - default mode , load everything in at once into Sorald. SEGMENT - repair segments of the projects instead, segmentsize can be specified.");
        return opt;
    }

    public static String getArgSoraldRepairMode(JSAPResult arguments) {
        return arguments.getString("soraldRepairMode");
    }

    public static FlaggedOption defineArgSegmentSize() {
        FlaggedOption opt = new FlaggedOption("segmentSize");
        opt.setLongFlag("segmentSize");
        opt.setStringParser(JSAP.INTEGER_PARSER);
        opt.setDefault("200");
        opt.setHelp("Segment size for the segment repair.");
        return opt;
    }

    public static Integer getArgSegmentSize(JSAPResult arguments) {
        return arguments.getInt("segmentSize");
    }

    public static FlaggedOption defineArgSoraldMaxFixesPerRule() {
        FlaggedOption opt = new FlaggedOption("soraldMaxFixesPerRule");
        opt.setLongFlag("soraldMaxFixesPerRule");
        opt.setStringParser(JSAP.INTEGER_PARSER);
        opt.setDefault("2000");
        opt.setHelp("Number of fixes per SonarQube rule.");
        return opt;
    }

    public static Integer getArgSoraldMaxFixesPerRule(JSAPResult arguments) {
        return arguments.getInt("soraldMaxFixesPerRule");
    }

    public static FlaggedOption defineArgPatchRankingMode() {
        FlaggedOption opt = new FlaggedOption("patchRankingMode");
        opt.setLongFlag("patchRankingMode");
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setDefault(RepairnatorConfig.PATCH_RANKING_MODE.NONE.name());
        opt.setHelp("Possible string values NONE, OVERFITTING.");
        return opt;
    }

    public static String getArgPatchRankingMode(JSAPResult arguments) {
        return arguments.getString("patchRankingMode");
    }

    public static void checkPushUrlArg(JSAP jsap, JSAPResult arguments, LauncherType launcherType) {
        if (getArgPushUrl(arguments) != null) {
            if (!Utils.matchesGithubRepoUrl(getArgPushUrl(arguments))) {
                System.err.println("The value of the argument pushurl is wrong.");
                LauncherUtils.printUsage(jsap, launcherType);
            }
        }
    }


    public static void printUsage(JSAP jsap, LauncherType launcherType) {
        String moduleName = "repairnator-" + launcherType.name().toLowerCase();
        System.err.println("Usage: java <" + moduleName + " name> [option(s)]");
        System.err.println();
        System.err.println("Options: ");
        System.err.println();
        System.err.println(jsap.getHelp());

        if (launcherType == LauncherType.PIPELINE) {
            System.err.println("The environment variable " + Utils.M2_HOME + " should be set and refer to the path of your maven home installation.");
            System.err.println("For using Nopol, you must add tools.jar in your classpath from your installed jdk");
        }

        System.exit(-1);
    }

    public static List<NotifierEngine> initNotifierEngines(Logger logger) {
        List<NotifierEngine> notifierEngines = new ArrayList<>();
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        if (config.getSmtpServer() != null && config.getNotifyTo() != null) {
            logger.info("The email notifier engine will be used.");

            notifierEngines.add(new EmailNotifierEngine(config.getNotifyTo(), config.getSmtpServer(), config.getSmtpPort(), config.isSmtpTLS(), config.getSmtpUsername(), config.getSmtpPassword()));
        } else {
            logger.info("The email notifier engine won't be used.");
        }
        return notifierEngines;
    }

    public static List<NotifierEngine> initEmailSummaryEngines(Logger logger) {
        List<NotifierEngine> summaryEngines = new ArrayList<>();
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        if (config.getSmtpServer() != null && config.getSummaryFrequency() != null && config.getNotifySummary() != null) {
            logger.info("The summary email engine will be used.");

            summaryEngines.add(new EmailNotifierEngine(config.getNotifySummary(), config.getSmtpServer(), config.getSmtpPort(), config.isSmtpTLS(), config.getSmtpUsername(), config.getSmtpPassword()));
        } else {
            logger.info("The summary email engine will not be used.");
        }
        return summaryEngines;
    }

    public static SerializerEngine initMongoDBSerializerEngine(Logger logger) {
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        if (config.getMongodbHost() != null) {
            logger.info("Initialize mongoDB serializer engine.");
            MongoConnection mongoConnection = new MongoConnection(config.getMongodbHost(), config.getMongodbName());
            if (mongoConnection.isConnected()) {
                return new MongoDBSerializerEngine(mongoConnection);
            } else {
                logger.error("Error while connecting to mongoDB.");
            }
        } else {
            logger.info("MongoDB won't be used for serialization.");
        }
        return null;
    }

    public static List<SerializerEngine> initFileSerializerEngines(Logger logger) {
        List<SerializerEngine> fileSerializerEngines = new ArrayList<>();
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        if (config.getOutputPath() != null) {
            logger.info("Initialize file serializer engines.");

            String path = config.getOutputPath();
            path += config.getBuildId() > 0 ? "/" + config.getBuildId() : "";

            fileSerializerEngines.add(new CSVSerializerEngine(path));
            fileSerializerEngines.add(new JSONFileSerializerEngine(path));
        } else {
            logger.info("File serializers won't be used.");
        }
        return fileSerializerEngines;
    }

}
