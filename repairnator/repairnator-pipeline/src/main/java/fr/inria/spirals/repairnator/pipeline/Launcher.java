package fr.inria.spirals.repairnator.pipeline;

import ch.qos.logback.classic.Level;
import com.martiansoftware.jsap.*;
import com.martiansoftware.jsap.stringparsers.EnumeratedStringParser;
import com.martiansoftware.jsap.stringparsers.FileStringParser;
import fr.inria.jtravis.JTravis;
import fr.inria.jtravis.entities.Build;
import fr.inria.jtravis.entities.StateType;
import fr.inria.spirals.repairnator.*;
import fr.inria.spirals.repairnator.notifier.ErrorNotifier;
import fr.inria.spirals.repairnator.serializer.*;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.AbstractNotifier;
import fr.inria.spirals.repairnator.notifier.BugAndFixerBuildsNotifier;
import fr.inria.spirals.repairnator.notifier.PatchNotifier;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector4Bears;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * This class is the main entry point for the repairnator pipeline.
 */
public class Launcher {
    private static Logger LOGGER = LoggerFactory.getLogger(Launcher.class);
    
    private BuildToBeInspected buildToBeInspected;
    private List<SerializerEngine> engines;
    private List<AbstractNotifier> notifiers;
    private PatchNotifier patchNotifier;

    private RepairnatorConfig getConfig() {
        return RepairnatorConfig.getInstance();
    }
    
    public Launcher(String[] args) throws JSAPException {
        InputStream propertyStream = getClass().getResourceAsStream("/version.properties");
        Properties properties = new Properties();
        if (propertyStream != null) {
            try {
                properties.load(propertyStream);
            } catch (IOException e) {
                LOGGER.error("Error while loading property file.", e);
            }
            LOGGER.info("PIPELINE VERSION: "+properties.getProperty("PIPELINE_VERSION"));
        } else {
            LOGGER.info("No information about PIPELINE VERSION has been found.");
        }

        JSAP jsap = this.defineArgs();
        JSAPResult arguments = jsap.parse(args);
        LauncherUtils.checkArguments(jsap, arguments, LauncherType.PIPELINE);
        this.initConfig(arguments);

        if (this.getConfig().getLauncherMode() == LauncherMode.REPAIR) {
            this.checkToolsLoaded(jsap);
            this.checkNopolSolverPath(jsap);
            LOGGER.info("The pipeline will try to repair the following build id: "+this.getConfig().getBuildId());
        } else {
            this.checkNextBuildId(jsap);
            LOGGER.info("The pipeline will try to reproduce a bug from build "+this.getConfig().getBuildId()+" and its corresponding patch from build "+this.getConfig().getNextBuildId());
        }

        if (this.getConfig().isDebug()) {
            Utils.setLoggersLevel(Level.DEBUG);
        } else {
            Utils.setLoggersLevel(Level.INFO);
        }

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
        // --bears
        jsap.registerParameter(LauncherUtils.defineArgBearsMode());
        // -o or --output
        jsap.registerParameter(LauncherUtils.defineArgOutput(LauncherType.PIPELINE, "Specify path to output serialized files"));
        // --dbhost
        jsap.registerParameter(LauncherUtils.defineArgMongoDBHost());
        // --dbname
        jsap.registerParameter(LauncherUtils.defineArgMongoDBName());
        // --smtpServer
        jsap.registerParameter(LauncherUtils.defineArgSmtpServer());
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

        FlaggedOption opt2 = new FlaggedOption("build");
        opt2.setShortFlag('b');
        opt2.setLongFlag("build");
        opt2.setStringParser(JSAP.INTEGER_PARSER);
        opt2.setRequired(true);
        opt2.setHelp("Specify the build id to use.");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("nextBuild");
        opt2.setShortFlag('n');
        opt2.setLongFlag("nextBuild");
        opt2.setStringParser(JSAP.INTEGER_PARSER);
        opt2.setDefault(InputBuildId.NO_PATCH+"");
        opt2.setHelp("Specify the next build id to use (only in BEARS mode).");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("z3");
        opt2.setLongFlag("z3");
        opt2.setDefault("./z3_for_linux");
        opt2.setStringParser(FileStringParser.getParser().setMustBeFile(true).setMustExist(true));
        opt2.setHelp("Specify path to Z3");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("workspace");
        opt2.setLongFlag("workspace");
        opt2.setShortFlag('w');
        opt2.setDefault("./workspace");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("Specify a path to be used by the pipeline at processing things like to clone the project of the build id being processed");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("projectsToIgnore");
        opt2.setLongFlag("projectsToIgnore");
        opt2.setDefault("./projects_to_ignore.txt");
        opt2.setStringParser(FileStringParser.getParser().setMustBeFile(true));
        opt2.setHelp("Specify the file containing a list of projects that the pipeline should deactivate serialization when processing builds from.");
        jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("repairTools");
        opt2.setLongFlag("repairTools");
        String repairTools = StringUtils.join(RepairToolsManager.getRepairToolsName(), ",");
        opt2.setStringParser(EnumeratedStringParser.getParser(repairTools.replace(',',';'), true));
        opt2.setList(true);
        opt2.setListSeparator(',');
        opt2.setHelp("Specify one or several repair tools to use among: "+repairTools);
        opt2.setRequired(true);
        opt2.setDefault(repairTools);
        jsap.registerParameter(opt2);

        return jsap;
    }

    private void initConfig(JSAPResult arguments) {
        if (LauncherUtils.getArgDebug(arguments)) {
            this.getConfig().setDebug(true);
        }
        this.getConfig().setClean(true);
        this.getConfig().setRunId(LauncherUtils.getArgRunId(arguments));
        this.getConfig().setGithubToken(LauncherUtils.getArgGithubOAuth(arguments));
        if (LauncherUtils.gerArgBearsMode(arguments)) {
            this.getConfig().setLauncherMode(LauncherMode.BEARS);
        } else {
            this.getConfig().setLauncherMode(LauncherMode.REPAIR);
        }
        if (LauncherUtils.getArgOutput(arguments) != null) {
            this.getConfig().setSerializeJson(true);
            this.getConfig().setOutputPath(LauncherUtils.getArgOutput(arguments).getPath());
        }
        this.getConfig().setMongodbHost(LauncherUtils.getArgMongoDBHost(arguments));
        this.getConfig().setMongodbName(LauncherUtils.getArgMongoDBName(arguments));
        this.getConfig().setSmtpServer(LauncherUtils.getArgSmtpServer(arguments));
        this.getConfig().setNotifyTo(LauncherUtils.getArgNotifyto(arguments));

        if (LauncherUtils.getArgPushUrl(arguments) != null) {
            this.getConfig().setPush(true);
            this.getConfig().setPushRemoteRepo(LauncherUtils.getArgPushUrl(arguments));
        }
        this.getConfig().setCreatePR(LauncherUtils.getArgCreatePR(arguments));

        // we fork if we need to create a PR or if we need to notify
        if (this.getConfig().isCreatePR() || (this.getConfig().getSmtpServer() != null && !this.getConfig().getSmtpServer().isEmpty() && this.getConfig().getNotifyTo() != null && this.getConfig().getNotifyTo().length > 0)) {
            this.getConfig().setFork(true);
        }
        this.getConfig().setBuildId(arguments.getInt("build"));
        if (this.getConfig().getLauncherMode() == LauncherMode.BEARS) {
            this.getConfig().setNextBuildId(arguments.getInt("nextBuild"));
        }
        this.getConfig().setZ3solverPath(arguments.getFile("z3").getPath());
        this.getConfig().setWorkspacePath(arguments.getString("workspace"));
        this.getConfig().setGithubUserEmail(LauncherUtils.getArgGithubUserEmail(arguments));
        this.getConfig().setGithubUserName(LauncherUtils.getArgGithubUserName(arguments));

        if (arguments.getFile("projectsToIgnore") != null) {
            this.getConfig().setProjectsToIgnoreFilePath(arguments.getFile("projectsToIgnore").getPath());
        }

        this.getConfig().setRepairTools(new HashSet<>(Arrays.asList(arguments.getStringArray("repairTools"))));
        if (this.getConfig().getLauncherMode() == LauncherMode.REPAIR) {
            LOGGER.info("The following repair tools will be used: " + StringUtils.join(this.getConfig().getRepairTools(), ", "));
        }
    }

    private void checkToolsLoaded(JSAP jsap) {
        URLClassLoader loader;

        try {
            loader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            loader.loadClass("com.sun.jdi.AbsentInformationException");
        } catch (ClassNotFoundException e) {
            System.err.println("Tools.jar must be loaded. The classpath given for your app is: "+System.getProperty("java.class.path"));
            LauncherUtils.printUsage(jsap, LauncherType.PIPELINE);
        }
    }

    private void checkNopolSolverPath(JSAP jsap) {
        String solverPath = this.getConfig().getZ3solverPath();

        if (solverPath != null) {
            File file = new File(solverPath);

            if (!file.exists()) {
                System.err.println("The Nopol solver path should be an existing file: " + file.getPath() + " does not exist.");
                LauncherUtils.printUsage(jsap, LauncherType.PIPELINE);
            }
        } else {
            System.err.println("The Nopol solver path should be provided.");
            LauncherUtils.printUsage(jsap, LauncherType.PIPELINE);
        }
    }

    private void checkNextBuildId(JSAP jsap) {
        if (this.getConfig().getNextBuildId() == InputBuildId.NO_PATCH) {
            System.err.println("A pair of builds needs to be provided in BEARS mode.");
            LauncherUtils.printUsage(jsap, LauncherType.PIPELINE);
        }
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
        List<NotifierEngine> notifierEngines = LauncherUtils.initNotifierEngines(LOGGER);
        ErrorNotifier.getInstance(notifierEngines);

        this.notifiers = new ArrayList<>();
        this.notifiers.add(new BugAndFixerBuildsNotifier(notifierEngines));

        this.patchNotifier = new PatchNotifier(notifierEngines);
    }

    private List<String> getListOfProjectsToIgnore() {
        List<String> result = new ArrayList<>();
        if (this.getConfig().getProjectsToIgnoreFilePath() != null) {
            try {
                List<String> lines = Files.readAllLines(new File(this.getConfig().getProjectsToIgnoreFilePath()).toPath());
                for (String line : lines) {
                    result.add(line.trim().toLowerCase());
                }
            } catch (IOException e) {
                LOGGER.error("Error while reading projects to be ignored from file "+this.getConfig().getProjectsToIgnoreFilePath(), e);
            }
        }
        return result;
    }

    private void getBuildToBeInspected() {
        JTravis jTravis = this.getConfig().getJTravis();
        Optional<Build> optionalBuild = jTravis.build().fromId(this.getConfig().getBuildId());
        if (!optionalBuild.isPresent()) {
            LOGGER.error("Error while retrieving the buggy build. The process will exit now.");
            System.exit(-1);
        }

        Build buggyBuild = optionalBuild.get();
        if (buggyBuild.getFinishedAt() == null) {
            LOGGER.error("Apparently the buggy build is not yet finished (maybe it has been restarted?). The process will exit now.");
            System.exit(-1);
        }
        String runId = this.getConfig().getRunId();

        if (this.getConfig().getLauncherMode() == LauncherMode.BEARS) {
            Optional<Build> optionalBuildPatch = jTravis.build().fromId(this.getConfig().getNextBuildId());
            if (!optionalBuildPatch.isPresent()) {
                LOGGER.error("Error while getting patched build: null value was obtained. The process will exit now.");
                System.exit(-1);
            }

            Build patchedBuild = optionalBuildPatch.get();
            LOGGER.info("The patched build (" + patchedBuild.getId() + ") was successfully retrieved from Travis.");

            if (buggyBuild.getState() == StateType.FAILED) {
                this.buildToBeInspected = new BuildToBeInspected(buggyBuild, patchedBuild, ScannedBuildStatus.FAILING_AND_PASSING, runId);
            } else {
                this.buildToBeInspected = new BuildToBeInspected(buggyBuild, patchedBuild, ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES, runId);
            }
        } else {
            Optional<Build> optionalNextPassing = jTravis.build().getAfter(buggyBuild, true, StateType.PASSED);
            if (optionalNextPassing.isPresent()) {
                this.buildToBeInspected = new BuildToBeInspected(buggyBuild, optionalNextPassing.get(), ScannedBuildStatus.FAILING_AND_PASSING, runId);
            } else {
                this.buildToBeInspected = new BuildToBeInspected(buggyBuild, null, ScannedBuildStatus.ONLY_FAIL, runId);
            }

            // switch off push mechanism in case of test project
            // and switch off serialization
            String project = buggyBuild.getRepository().getSlug().toLowerCase();
            if (this.getListOfProjectsToIgnore().contains(project)) {
                this.getConfig().setPush(false);
                this.getConfig().setFork(false);
                this.getConfig().setCreatePR(false);
                this.engines.clear();
                LOGGER.info("The build "+this.getConfig().getBuildId()+" is from a project to be ignored ("+project+"), thus the pipeline deactivated serialization for that build.");
            }
        }
    }

    private void mainProcess() {
        LOGGER.info("Start by getting the build (buildId: "+this.getConfig().getBuildId()+") with the following config: "+this.getConfig());
        this.getBuildToBeInspected();

        HardwareInfoSerializer hardwareInfoSerializer = new HardwareInfoSerializer(this.engines, this.getConfig().getRunId(), this.getConfig().getBuildId()+"");
        hardwareInfoSerializer.serialize();

        List<AbstractDataSerializer> serializers = new ArrayList<>();

        if (this.getConfig().getLauncherMode() == LauncherMode.BEARS) {
            serializers.add(new InspectorSerializer4Bears(this.engines));
        } else {
            serializers.add(new InspectorSerializer(this.engines));
        }

        serializers.add(new PropertiesSerializer(this.engines));
        serializers.add(new InspectorTimeSerializer(this.engines));
        serializers.add(new PipelineErrorSerializer(this.engines));
        serializers.add(new PatchesSerializer(this.engines));
        serializers.add(new ToolDiagnosticSerializer(this.engines));

        ProjectInspector inspector;

        if (this.getConfig().getLauncherMode() == LauncherMode.BEARS) {
            inspector = new ProjectInspector4Bears(buildToBeInspected, this.getConfig().getWorkspacePath(), serializers, this.notifiers);
        } else {
            inspector = new ProjectInspector(buildToBeInspected, this.getConfig().getWorkspacePath(), serializers, this.notifiers);
        }

        inspector.setPatchNotifier(this.patchNotifier);
        inspector.run();

        LOGGER.info("Inspector is finished. The process will exit now.");
        System.exit(0);
    }

    public static void main(String[] args) throws JSAPException {
        Launcher launcher = new Launcher(args);
        launcher.mainProcess();
    }

}
