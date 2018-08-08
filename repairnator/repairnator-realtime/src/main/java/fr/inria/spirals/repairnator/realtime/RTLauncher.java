package fr.inria.spirals.repairnator.realtime;

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
    private final LauncherMode launcherMode;
    private List<SerializerEngine> engines;
    private RepairnatorConfig config;
    private EndProcessNotifier endProcessNotifier;

    private RTLauncher(String[] args) throws JSAPException {
        JSAP jsap = this.defineArgs();
        JSAPResult arguments = jsap.parse(args);
        LauncherUtils.checkArguments(jsap, arguments, LauncherType.REALTIME);
        this.launcherMode = LauncherMode.REPAIR;

        this.initConfig(arguments);
        this.initSerializerEngines();
        this.initNotifiers();
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
        // --dbhost
        jsap.registerParameter(LauncherUtils.defineArgMongoDBHost());
        // --dbname
        jsap.registerParameter(LauncherUtils.defineArgMongoDBName());
        // --notifyEndProcess
        jsap.registerParameter(LauncherUtils.defineArgNotifyEndProcess());
        // --smtpServer
        jsap.registerParameter(LauncherUtils.defineArgSmtpServer());
        // --notifyto
        jsap.registerParameter(LauncherUtils.defineArgNotifyto());
        // -n or --name
        jsap.registerParameter(LauncherUtils.defineArgDockerImageName());
        // --skipDelete
        jsap.registerParameter(LauncherUtils.defineArgSkipDelete());
        // --createOutputDir
        jsap.registerParameter(LauncherUtils.defineArgCreateOutputDir());
        // -l or --logDirectory
        jsap.registerParameter(LauncherUtils.defineArgLogDirectory());
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
        opt2.setDefault(InspectJobs.JOB_SLEEP_TIME+"");
        opt2.setHelp("Specify the sleep time between two requests to Travis Job endpoint (in seconds)");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("buildsleeptime");
        opt2.setLongFlag("buildsleeptime");
        opt2.setStringParser(JSAP.INTEGER_PARSER);
        opt2.setDefault(InspectBuilds.BUILD_SLEEP_TIME+"");
        opt2.setHelp("Specify the sleep time between two refresh of build statuses (in seconds)");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("maxinspectedbuilds");
        opt2.setLongFlag("maxinspectedbuilds");
        opt2.setStringParser(JSAP.INTEGER_PARSER);
        opt2.setDefault(InspectBuilds.LIMIT_SUBMITTED_BUILDS+"");
        opt2.setHelp("Specify the maximum number of watched builds");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("duration");
        opt2.setLongFlag("duration");
        opt2.setStringParser(PeriodStringParser.getParser());
        opt2.setHelp("Duration of the execution. If not given, the execution never stop. This argument should be given on the ISO-8601 duration format: PWdTXhYmZs where W, X, Y, Z respectively represents number of Days, Hours, Minutes and Seconds. T is mandatory before the number of hours and P is always mandatory.");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("repairTools");
        opt2.setLongFlag("repairTools");
        opt2.setListSeparator(',');
        opt2.setHelp("Specify one or several repair tools to use separated by commas (available tools might depend of your docker image)");
        opt2.setRequired(true);
        jsap.registerParameter(opt2);

        return jsap;
    }

    private void initConfig(JSAPResult arguments) {
        this.config = RepairnatorConfig.getInstance();

        if (LauncherUtils.getArgDebug(arguments)) {
            this.config.setDebug(true);
        }
        this.config.setRunId(LauncherUtils.getArgRunId(arguments));
        this.config.setLauncherMode(this.launcherMode);
        this.config.setOutputPath(LauncherUtils.getArgOutput(arguments).getPath());
        this.config.setMongodbHost(LauncherUtils.getArgMongoDBHost(arguments));
        this.config.setMongodbName(LauncherUtils.getArgMongoDBName(arguments));
        this.config.setNotifyEndProcess(LauncherUtils.getArgNotifyEndProcess(arguments));
        this.config.setSmtpServer(LauncherUtils.getArgSmtpServer(arguments));
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
        if (arguments.contains("whitelist")) {
            this.config.setWhiteList(arguments.getFile("whitelist"));
        }
        if (arguments.contains("blacklist")) {
            this.config.setBlackList(arguments.getFile("blacklist"));
        }
        this.config.setJobSleepTime(arguments.getInt("jobsleeptime"));
        this.config.setBuildSleepTime(arguments.getInt("buildsleeptime"));
        this.config.setMaxInspectedBuilds(arguments.getInt("maxinspectedbuilds"));
        if (arguments.getObject("duration") != null) {
            this.config.setDuration((Duration) arguments.getObject("duration"));
        }
        this.config.setCreatePR(LauncherUtils.getArgCreatePR(arguments));
        this.config.setRepairTools(new HashSet<>(Arrays.asList(arguments.getStringArray("repairTools"))));
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

    private void initAndRunRTScanner() {
        LOGGER.info("Init RTScanner...");
        String runId = this.config.getRunId();
        HardwareInfoSerializer hardwareInfoSerializer = new HardwareInfoSerializer(this.engines, runId, "rtScanner");
        hardwareInfoSerializer.serialize();
        RTScanner rtScanner = new RTScanner(runId, this.engines);

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
        rtLauncher.initAndRunRTScanner();
    }

}
