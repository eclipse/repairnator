package fr.inria.spirals.repairnator.dockerpool;
import com.google.api.client.auth.oauth2.Credential;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import fr.inria.spirals.repairnator.LauncherType;
import fr.inria.spirals.repairnator.LauncherUtils;
import fr.inria.spirals.repairnator.notifier.EndProcessNotifier;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.dockerpool.serializer.EndProcessSerializer;
import fr.inria.spirals.repairnator.serializer.HardwareInfoSerializer;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.serializer.gspreadsheet.ManageGoogleAccessToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by urli on 13/03/2017.
 */
public class Launcher extends AbstractPoolManager {
    private static Logger LOGGER = LoggerFactory.getLogger(Launcher.class);
    private final LauncherMode launcherMode;
    private JSAP jsap;
    private JSAPResult arguments;
    private String accessToken;
    private List<SerializerEngine> engines;
    private RepairnatorConfig config;
    private EndProcessNotifier endProcessNotifier;

    private void defineArgs() throws JSAPException {
        // Verbose output
        this.jsap = new JSAP();

        // -h or --help
        this.jsap.registerParameter(LauncherUtils.defineArgHelp());
        // -d or --debug
        this.jsap.registerParameter(LauncherUtils.defineArgDebug());
        // --runId
        this.jsap.registerParameter(LauncherUtils.defineArgRunId());
        // -m or --launcherMode
        this.jsap.registerParameter(LauncherUtils.defineArgLauncherMode("Specify if the dockerpool intends to repair failing builds (REPAIR) or gather builds info (BEARS)."));
        // -i or --input
        this.jsap.registerParameter(LauncherUtils.defineArgInput("Specify the input file containing the list of build ids."));
        // -o or --output
        this.jsap.registerParameter(LauncherUtils.defineArgOutput(LauncherType.DOCKERPOOL,"Specify where to put serialized files from dockerpool"));
        // --dbhost
        this.jsap.registerParameter(LauncherUtils.defineArgMongoDBHost());
        // --dbname
        this.jsap.registerParameter(LauncherUtils.defineArgMongoDBName());
        // --spreadsheet
        this.jsap.registerParameter(LauncherUtils.defineArgSpreadsheetId());
        // --googleSecretPath
        this.jsap.registerParameter(LauncherUtils.defineArgGoogleSecretPath());
        // --notifyEndProcess
        this.jsap.registerParameter(LauncherUtils.defineArgNotifyEndProcess());
        // --smtpServer
        this.jsap.registerParameter(LauncherUtils.defineArgSmtpServer());
        // --notifyto
        this.jsap.registerParameter(LauncherUtils.defineArgNotifyto());
        // -n or --name
        this.jsap.registerParameter(LauncherUtils.defineArgDockerImageName());
        // --skipDelete
        this.jsap.registerParameter(LauncherUtils.defineArgSkipDelete());
        // --createOutputDir
        this.jsap.registerParameter(LauncherUtils.defineArgCreateOutputDir());
        // -l or --logDirectory
        this.jsap.registerParameter(LauncherUtils.defineArgLogDirectory());
        // -t or --threads
        this.jsap.registerParameter(LauncherUtils.defineArgNbThreads());
        // -g or --globalTimeout
        this.jsap.registerParameter(LauncherUtils.defineArgGlobalTimeout());
        // --pushurl
        this.jsap.registerParameter(LauncherUtils.defineArgPushUrl());
    }

    private List<Integer> readListOfBuildIds() {
        List<Integer> result = new ArrayList<>();
        File inputFile = LauncherUtils.getArgInput(this.arguments);

        try {
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            while (reader.ready()) {
                String line = reader.readLine().trim();
                result.add(Integer.parseInt(line));
            }

            reader.close();
        } catch (IOException e) {
            throw new RuntimeException("Error while reading build ids from file: "+inputFile.getPath(),e);
        }

        return result;
    }

    private void runPool() throws IOException {
        String runId = LauncherUtils.getArgRunId(this.arguments);
        HardwareInfoSerializer hardwareInfoSerializer = new HardwareInfoSerializer(this.engines, runId, "dockerPool");
        hardwareInfoSerializer.serialize();

        EndProcessSerializer endProcessSerializer = new EndProcessSerializer(this.engines, runId);
        List<Integer> buildIds = this.readListOfBuildIds();
        LOGGER.info("Find "+buildIds.size()+" builds to run.");

        endProcessSerializer.setNbBuilds(buildIds.size());

        String imageId = this.findDockerImage(LauncherUtils.getArgDockerImageName(this.arguments));
        LOGGER.info("Found the following docker image id: "+imageId);

        this.setDockerOutputDir(LauncherUtils.getArgLogDirectory(this.arguments));
        this.setRunId(runId);
        this.setCreateOutputDir(LauncherUtils.getArgCreateOutputDir(this.arguments));
        this.setSkipDelete(LauncherUtils.getArgSkipDelete(this.arguments));
        this.setEngines(this.engines);

        ExecutorService executorService = Executors.newFixedThreadPool(LauncherUtils.getArgNbThreads(this.arguments));

        for (Integer builId : buildIds) {
            executorService.submit(this.submitBuild(imageId, builId));
        }

        executorService.shutdown();
        try {
            if (executorService.awaitTermination(LauncherUtils.getArgGlobalTimeout(this.arguments), TimeUnit.DAYS)) {
                LOGGER.info("Job finished within time.");
                endProcessSerializer.setStatus("ok");
            } else {
                LOGGER.warn("Timeout launched: the job is running for one day. Force stopped "+ submittedRunnablePipelineContainers.size()+" docker container(s).");
                executorService.shutdownNow();
                this.setStatusForUnexecutedJobs();
                endProcessSerializer.setStatus("timeout");
            }
        } catch (InterruptedException e) {
            LOGGER.error("Error while await termination. Force stopped "+ submittedRunnablePipelineContainers.size()+" docker container(s).", e);
            executorService.shutdownNow();
            this.setStatusForUnexecutedJobs();
            endProcessSerializer.setStatus("interrupted");
        }

        this.getDockerClient().close();
        endProcessSerializer.serialize();
        if (this.endProcessNotifier != null) {
            this.endProcessNotifier.notifyEnd();
        }
    }

    private void setStatusForUnexecutedJobs() {
        for (RunnablePipelineContainer runnablePipelineContainer : submittedRunnablePipelineContainers) {
            runnablePipelineContainer.serialize("ABORTED");
        }
    }

    private Launcher(String[] args) throws JSAPException {
        this.defineArgs();
        this.arguments = jsap.parse(args);
        LauncherUtils.checkArguments(this.jsap, this.arguments, LauncherType.DOCKERPOOL);
        LauncherUtils.checkEnvironmentVariables(this.jsap, LauncherType.DOCKERPOOL);
        this.launcherMode = LauncherUtils.getArgLauncherMode(this.arguments);

        this.initConfig();
        this.initSerializerEngines();
        this.initNotifiers();
    }

    private void initNotifiers() {
        if (LauncherUtils.getArgNotifyEndProcess(this.arguments)) {
            List<NotifierEngine> notifierEngines = LauncherUtils.initNotifierEngines(this.arguments, LOGGER);
            this.endProcessNotifier = new EndProcessNotifier(notifierEngines, LauncherType.DOCKERPOOL.name().toLowerCase()+" (runid: "+LauncherUtils.getArgRunId(this.arguments)+")");
        }
    }

    private void initConfig() {
        this.config = RepairnatorConfig.getInstance();
        this.config.setLauncherMode(LauncherUtils.getArgLauncherMode(this.arguments));

        if (LauncherUtils.getArgPushUrl(this.arguments) != null) {
            this.config.setPush(true);
            this.config.setPushRemoteRepo(LauncherUtils.getArgPushUrl(this.arguments));
            this.config.setFork(true);
        }
        this.config.setRunId(LauncherUtils.getArgRunId(this.arguments));
        this.config.setSpreadsheetId(LauncherUtils.getArgSpreadsheetId(this.arguments));
        this.config.setMongodbHost(LauncherUtils.getArgMongoDBHost(this.arguments));
        this.config.setMongodbName(LauncherUtils.getArgMongoDBName(this.arguments));
        this.config.setSmtpServer(LauncherUtils.getArgSmtpServer(this.arguments));
        this.config.setNotifyTo(LauncherUtils.getArgNotifyto(this.arguments));
    }

    private void initSerializerEngines() {
        this.engines = new ArrayList<>();

        SerializerEngine spreadsheetSerializerEngine = LauncherUtils.initSpreadsheetSerializerEngineWithFileSecret(this.arguments, LOGGER);
        if (spreadsheetSerializerEngine != null) {
            this.engines.add(spreadsheetSerializerEngine);

            try {
                ManageGoogleAccessToken manageGoogleAccessToken = ManageGoogleAccessToken.getInstance();
                Credential credential = manageGoogleAccessToken.getCredential();

                if (credential != null) {
                    this.accessToken = credential.getAccessToken();
                    this.config.setGoogleAccessToken(this.accessToken);
                }
            } catch (IOException | GeneralSecurityException e) {
                LOGGER.error("Error while initializing Google Spreadsheet, no information will be serialized in spreadsheets from the pipeline.", e);
            }
        }

        List<SerializerEngine> fileSerializerEngines = LauncherUtils.initFileSerializerEngines(this.arguments, LOGGER);
        this.engines.addAll(fileSerializerEngines);

        SerializerEngine mongoDBSerializerEngine = LauncherUtils.initMongoDBSerializerEngine(this.arguments, LOGGER);
        if (mongoDBSerializerEngine != null) {
            this.engines.add(mongoDBSerializerEngine);
        }
    }

    public static void main(String[] args) throws Exception {
        Launcher launcher = new Launcher(args);
        launcher.runPool();
    }
}
