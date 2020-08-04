package fr.inria.spirals.repairnator.realtime;

import static fr.inria.spirals.repairnator.config.RepairnatorConfig.PIPELINE_MODE;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.stringparsers.FileStringParser;
import fr.inria.spirals.repairnator.LauncherType;
import fr.inria.spirals.repairnator.LauncherUtils;
import fr.inria.spirals.repairnator.PeriodStringParser;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.EndProcessNotifier;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.realtime.notifier.TimedSummaryNotifier;
import fr.inria.spirals.repairnator.serializer.HardwareInfoSerializer;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.states.LauncherMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * This is the launcher class for Repairnator realtime.
 */
public class RTLauncher {

    private static Logger LOGGER = LoggerFactory.getLogger(RTLauncher.class);
    private List<SerializerEngine> engines;
    private RepairnatorConfig config;
    private EndProcessNotifier endProcessNotifier;
    private TimedSummaryNotifier summaryNotifier;

    private RTLauncher(String[] args) throws JSAPException {
        JSAP jsap = this.defineArgs();
        JSAPResult arguments = jsap.parse(args);
        LauncherUtils.checkArguments(jsap, arguments, LauncherType.REALTIME);

        this.initConfig(arguments);
        this.initSerializerEngines();
        this.initNotifiers();
        this.initSummaryEmails();
    }

    private JSAP defineArgs() throws JSAPException {
        // Verbose output
        JSAP jsap = new JSAP();

        // -h or --help
        jsap.registerParameter(LauncherUtils.defineArgHelp());
        // -d or --debug
        jsap.registerParameter(LauncherUtils.defineArgDebug());
        // --runId
        jsap.registerParameter(LauncherUtils.defineArgRunId());
        // -o or --output
        jsap.registerParameter(LauncherUtils.defineArgOutput(LauncherType.REALTIME, "Specify where to put serialized files from dockerpool"));
        // --checkstyle
        jsap.registerParameter(LauncherUtils.defineArgCheckstyleMode());
        // --dbhost
        jsap.registerParameter(LauncherUtils.defineArgMongoDBHost());
        // --dbname
        jsap.registerParameter(LauncherUtils.defineArgMongoDBName());
        // --notifyEndProcess
        jsap.registerParameter(LauncherUtils.defineArgNotifyEndProcess());
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
        // -n or --name
        jsap.registerParameter(LauncherUtils.defineArgDockerImageName());
        // --skipDelete
        jsap.registerParameter(LauncherUtils.defineArgSkipDelete());
        // --createOutputDir
        // the output directory contains CSV files, JSON files and LOG files
        jsap.registerParameter(LauncherUtils.defineArgCreateOutputDir());
        // -t or --threads
        jsap.registerParameter(LauncherUtils.defineArgNbThreads());
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

        FlaggedOption opt2 = new FlaggedOption("whitelist");
        opt2.setShortFlag('w');
        opt2.setLongFlag("whitelist");
        opt2.setStringParser(FileStringParser.getParser().setMustBeDirectory(false).setMustExist(true));
        opt2.setHelp("Specify the path of whitelisted repository");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("blacklist");
        opt2.setShortFlag('b');
        opt2.setLongFlag("blacklist");
        opt2.setStringParser(FileStringParser.getParser().setMustBeDirectory(false).setMustExist(true));
        opt2.setHelp("Specify the path of blacklisted repository");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("jobsleeptime");
        opt2.setLongFlag("jobsleeptime");
        opt2.setStringParser(JSAP.INTEGER_PARSER);
        opt2.setDefault(InspectJobs.JOB_SLEEP_TIME_IN_SECOND +"");
        opt2.setHelp("Specify the sleep time between two requests to Travis Job endpoint (in seconds)");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("buildsleeptime");
        opt2.setLongFlag("buildsleeptime");
        opt2.setStringParser(JSAP.INTEGER_PARSER);
        opt2.setDefault(InspectBuilds.BUILD_SLEEP_TIME_IN_SECOND +"");
        opt2.setHelp("Specify the sleep time between two refresh of build statuses (in seconds)");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("maxinspectedbuilds");
        opt2.setLongFlag("maxinspectedbuilds");
        opt2.setStringParser(JSAP.INTEGER_PARSER);
        opt2.setDefault(InspectBuilds.LIMIT_WAITING_BUILDS +"");
        opt2.setHelp("Specify the maximum number of watched builds");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("duration");
        opt2.setLongFlag("duration");
        opt2.setStringParser(PeriodStringParser.getParser());
        opt2.setHelp("Duration of the execution. If not given, the execution never stop. This argument should be given on the ISO-8601 duration format: PWdTXhYmZs where W, X, Y, Z respectively represents number of Days, Hours, Minutes and Seconds. T is mandatory before the number of hours and P is always mandatory.");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("repairTools");
        opt2.setLongFlag("repairTools");
        opt2.setList(true);
        opt2.setListSeparator(',');
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("Specify one or several repair tools to use separated by commas (available tools might depend of your docker image)");
        opt2.setDefault("AstorJKali");
        jsap.registerParameter(opt2);
        
        opt2 = new FlaggedOption("notifysummary");
        opt2.setLongFlag("notifysummary");
        opt2.setList(true);
        opt2.setListSeparator(',');
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("The email addresses to notify with a summary email.");
        jsap.registerParameter(opt2);
        
        opt2 = new FlaggedOption("summaryfrequency");
        opt2.setLongFlag("summaryfrequency");
        opt2.setStringParser(PeriodStringParser.getParser());
        opt2.setHelp("Duration between summary emails. If not given, the emails will never be sent. This argument should be given on the ISO-8601 duration format: PWdTXhYmZs where W, X, Y, Z respectively represents number of Days, Hours, Minutes and Seconds. T is mandatory before the number of hours and P is always mandatory.");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("numberofprs");
        opt2.setLongFlag("numberofprs");
        opt2.setStringParser(JSAP.INTEGER_PARSER);
        opt2.setDefault(0 + "");
        opt2.setHelp("The number of pull request that Repairnator should create before turning itself off. If 0, it will run indefinitely.");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("pipelinemode");
        opt2.setLongFlag("pipelinemode");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setDefault(PIPELINE_MODE.NOOP.name());
        opt2.setHelp("Possible string values DOCKER,KUBERNETES,NOOP . DOCKER is for running DockerPipeline, KUBERNETES is for running ActiveMQPipeline and "+PIPELINE_MODE.NOOP.name()+" is for NoopRunner. The last two options do not use docker during run.");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("activemqurl");
        opt2.setLongFlag("activemqurl");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setDefault("tcp://localhost:61616");
        opt2.setHelp("format: 'tcp://IP_OR_DNSNAME:61616', default as 'tcp://localhost:61616'");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("activemqsubmitqueuename");
        opt2.setLongFlag("activemqsubmitqueuename");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setDefault("pipeline");
        opt2.setHelp("Just a name, default as 'pipeline'");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("websocketurl");
        opt2.setLongFlag("websocketurl");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setDefault("ws://localhost:9080");
        opt2.setHelp("websocket url of the nodejs websocket, default: ws://localhost:9080");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("jmxhost");
        opt2.setLongFlag("jmxhost");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setDefault("localhost");
        opt2.setHelp("HostName of the activemq jmxhost, default: localhost");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("queuelimit");
        opt2.setLongFlag("queuelimit");
        opt2.setStringParser(JSAP.INTEGER_PARSER);
        opt2.setDefault("100");
        opt2.setHelp("limit before stop submitting new builds to queue, default: 100 enqueued build ids");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("scannermode");
        opt2.setLongFlag("scannermode");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setDefault(PIPELINE_MODE.NOOP.name());
        opt2.setHelp("Possible string values RTSCANNER and BUILBRAINER . RTSCANNER is the usual RTSCANNER which can be run as KUBERNETES, NOOP or DOCKER mode, BUILDRAINER requires travis-lister to fetch build and submit on activeMQ");
        jsap.registerParameter(opt2);

        return jsap;
    }

    private void initConfig(JSAPResult arguments) {
        this.config = RepairnatorConfig.getInstance();

        if (LauncherUtils.getArgDebug(arguments)) {
            this.config.setDebug(true);
        }
        if (LauncherUtils.getArgCheckstyleMode(arguments)) {
            this.config.setLauncherMode(LauncherMode.CHECKSTYLE);
        } else {
            this.config.setLauncherMode(LauncherMode.REPAIR);
        }
        this.config.setRunId(LauncherUtils.getArgRunId(arguments));
        this.config.setOutputPath(LauncherUtils.getArgOutput(arguments).getPath());
        this.config.setMongodbHost(LauncherUtils.getArgMongoDBHost(arguments));
        this.config.setMongodbName(LauncherUtils.getArgMongoDBName(arguments));
        this.config.setNotifyEndProcess(LauncherUtils.getArgNotifyEndProcess(arguments));
        this.config.setSmtpServer(LauncherUtils.getArgSmtpServer(arguments));
        this.config.setSmtpPort(LauncherUtils.getArgSmtpPort(arguments));
        this.config.setSmtpTLS(LauncherUtils.getArgSmtpTLS(arguments));
        this.config.setSmtpUsername(LauncherUtils.getArgSmtpUsername(arguments));
        this.config.setSmtpPassword(LauncherUtils.getArgSmtpPassword(arguments));
        this.config.setNotifyTo(LauncherUtils.getArgNotifyto(arguments));
        this.config.setDockerImageName(LauncherUtils.getArgDockerImageName(arguments));
        this.config.setSkipDelete(LauncherUtils.getArgSkipDelete(arguments));
        this.config.setCreateOutputDir(LauncherUtils.getArgCreateOutputDir(arguments));
        this.config.setLogDirectory(LauncherUtils.getArgLogDirectory(arguments));
        this.config.setNbThreads(LauncherUtils.getArgNbThreads(arguments));
        this.config.setGithubToken(LauncherUtils.getArgGithubOAuth(arguments));
        this.config.setGithubUserEmail(LauncherUtils.getArgGithubUserEmail(arguments));
        this.config.setGithubUserName(LauncherUtils.getArgGithubUserName(arguments));
        if (LauncherUtils.getArgPushUrl(arguments) != null) {
            this.config.setPush(true);
            this.config.setPushRemoteRepo(LauncherUtils.getArgPushUrl(arguments));
        }
        this.config.setWhiteList(arguments.getFile("whitelist"));
        this.config.setBlackList(arguments.getFile("blacklist"));
        this.config.setJobSleepTime(arguments.getInt("jobsleeptime"));
        this.config.setBuildSleepTime(arguments.getInt("buildsleeptime"));
        this.config.setMaxInspectedBuilds(arguments.getInt("maxinspectedbuilds"));
        if (arguments.getObject("duration") != null) {
            this.config.setDuration((Duration) arguments.getObject("duration"));
        }
        this.config.setNotifySummary(arguments.getStringArray("notifysummary"));
        if (arguments.getObject("summaryfrequency") != null) {
            this.config.setSummaryFrequency((Duration) arguments.getObject("summaryfrequency"));
        }
        this.config.setCreatePR(LauncherUtils.getArgCreatePR(arguments));
        this.config.setRepairTools(new HashSet<>(Arrays.asList(arguments.getStringArray("repairTools"))));
        this.config.setNumberOfPRs(arguments.getInt("numberofprs"));
        //this.config.setPipelineMode(arguments.getString("pipelinemode"));
        this.config.setPipelineMode("DOCKER");

        this.config.setActiveMQUrl(arguments.getString("activemqurl"));
        this.config.setActiveMQSubmitQueueName(arguments.getString("activemqsubmitqueuename"));
        this.config.setWebSocketUrl(arguments.getString("websocketurl"));
        this.config.setJmxHostName(arguments.getString("jmxhost"));
        this.config.setQueueLimit(arguments.getInt("queuelimit"));
    }

    private void initSerializerEngines() {
        this.engines = new ArrayList<>();

        List<SerializerEngine> fileSerializerEngines = LauncherUtils.initFileSerializerEngines(LOGGER);
        this.engines.addAll(fileSerializerEngines);

        SerializerEngine mongoDBSerializerEngine = LauncherUtils.initMongoDBSerializerEngine(LOGGER);
        if (mongoDBSerializerEngine != null) {
            this.engines.add(mongoDBSerializerEngine);
        }
    }

    private void initNotifiers() {
        if (this.config.isNotifyEndProcess()) {
            List<NotifierEngine> notifierEngines = LauncherUtils.initNotifierEngines(LOGGER);
            this.endProcessNotifier = new EndProcessNotifier(notifierEngines, LauncherType.REALTIME.name().toLowerCase()+" (runid: "+this.config.getRunId()+")");
        }
    }
    
    private void initSummaryEmails() {
        List<NotifierEngine> summaryEngines = LauncherUtils.initEmailSummaryEngines(LOGGER);
        if(summaryEngines.size() > 0) {
            this.summaryNotifier = new TimedSummaryNotifier(summaryEngines,
                    config.getSummaryFrequency(),
                    config.getRepairTools().toArray(new String[config.getRepairTools().size()]),
                    config.getMongodbHost(),
                    config.getMongodbName());
        }
    }

    private void initAndRunRTScanner() {
        LOGGER.info("Init RTScanner...");
        LOGGER.info("RTScanner mode : " + this.config.getLauncherMode());
        LOGGER.info("Number of tools" + config.getRepairTools().toArray(new String[0]));
        String runId = this.config.getRunId();
        HardwareInfoSerializer hardwareInfoSerializer = new HardwareInfoSerializer(this.engines, runId, "rtScanner");
        hardwareInfoSerializer.serialize();
        RTScanner rtScanner = null;

        PipelineRunner runner = null;
        try {
            runner = (PipelineRunner) Class.forName(this.config.getPipelineMode().getKlass()).newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        rtScanner = new RTScanner(runId, engines, runner);

        if (this.summaryNotifier != null) {
            rtScanner.setSummaryNotifier(this.summaryNotifier);
        }

        if (this.config.getDuration() != null && this.endProcessNotifier != null) {
            rtScanner.setEndProcessNotifier(this.endProcessNotifier);
        }

        if (this.config.getWhiteList() != null) {
            rtScanner.initWhiteListedRepository(this.config.getWhiteList());
        }

        if (this.config.getBlackList() != null) {
            rtScanner.initBlackListedRepository(this.config.getBlackList());
        }
        LOGGER.info("Start RTScanner...");
        rtScanner.launch();
    }

    public static void main(String[] args) throws JSAPException {
        RTLauncher rtLauncher = new RTLauncher(args);
        /* If scanner mode is RTSCanner */
        rtLauncher.initAndRunRTScanner();
        /* If scanner mode is BuildRainer */
        /* Create and connect BuildRainer */
    }

}
