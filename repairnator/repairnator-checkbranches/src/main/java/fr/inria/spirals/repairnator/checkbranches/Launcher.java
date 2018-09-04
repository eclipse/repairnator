package fr.inria.spirals.repairnator.checkbranches;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import com.spotify.docker.client.DockerClient;
import fr.inria.spirals.repairnator.LauncherType;
import fr.inria.spirals.repairnator.LauncherUtils;
import fr.inria.spirals.repairnator.docker.DockerHelper;
import fr.inria.spirals.repairnator.notifier.EndProcessNotifier;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.states.LauncherMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by urli on 13/03/2017.
 */
public class Launcher {
    private static Logger LOGGER = LoggerFactory.getLogger(Launcher.class);
    private RepairnatorConfig config;
    private EndProcessNotifier endProcessNotifier;

    public static List<RunnablePipelineContainer> submittedRunnablePipelineContainers = new CopyOnWriteArrayList<>();
    public static DockerClient docker;

    private Launcher(String[] args) throws JSAPException {
        JSAP jsap = this.defineArgs();
        JSAPResult arguments = jsap.parse(args);
        LauncherUtils.checkArguments(jsap, arguments, LauncherType.CHECKBRANCHES);

        this.initConfig(arguments);
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
        // -i or --input
        jsap.registerParameter(LauncherUtils.defineArgInput("Specify the input file containing the list of branches to reproduce"));
        // -o or --output
        jsap.registerParameter(LauncherUtils.defineArgOutput(LauncherType.CHECKBRANCHES, "Specify where to put output data"));
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
        // -t or --threads
        jsap.registerParameter(LauncherUtils.defineArgNbThreads());
        // -g or --globalTimeout
        jsap.registerParameter(LauncherUtils.defineArgGlobalTimeout());

        Switch sw1 = new Switch("humanPatch");
        sw1.setShortFlag('p');
        sw1.setLongFlag("humanPatch");
        sw1.setDefault("false");
        jsap.registerParameter(sw1);

        FlaggedOption opt2 = new FlaggedOption("repository");
        opt2.setShortFlag('r');
        opt2.setLongFlag("repository");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setRequired(true);
        opt2.setHelp("Specify where to collect branches");
        jsap.registerParameter(opt2);

        return jsap;
    }

    private void initConfig(JSAPResult arguments) {
        this.config = RepairnatorConfig.getInstance();

        if (LauncherUtils.getArgDebug(arguments)) {
            this.config.setDebug(true);
        }
        this.config.setRunId(LauncherUtils.getArgRunId(arguments));
        if (LauncherUtils.gerArgBearsMode(arguments)) {
            this.config.setLauncherMode(LauncherMode.BEARS);
        } else {
            this.config.setLauncherMode(LauncherMode.REPAIR);
        }
        this.config.setInputPath(LauncherUtils.getArgInput(arguments).getPath());
        this.config.setSerializeJson(true);
        this.config.setOutputPath(LauncherUtils.getArgOutput(arguments).getAbsolutePath());
        this.config.setNotifyEndProcess(LauncherUtils.getArgNotifyEndProcess(arguments));
        this.config.setSmtpServer(LauncherUtils.getArgSmtpServer(arguments));
        this.config.setNotifyTo(LauncherUtils.getArgNotifyto(arguments));
        this.config.setDockerImageName(LauncherUtils.getArgDockerImageName(arguments));
        this.config.setSkipDelete(LauncherUtils.getArgSkipDelete(arguments));
        this.config.setNbThreads(LauncherUtils.getArgNbThreads(arguments));
        this.config.setGlobalTimeout(LauncherUtils.getArgGlobalTimeout(arguments));
        this.config.setHumanPatch(arguments.getBoolean("humanPatch"));
        this.config.setRepository(arguments.getString("repository"));
    }

    private void initNotifiers() {
        if (this.config.isNotifyEndProcess()) {
            List<NotifierEngine> notifierEngines = LauncherUtils.initNotifierEngines(LOGGER);
            this.endProcessNotifier = new EndProcessNotifier(notifierEngines, LauncherType.CHECKBRANCHES.name().toLowerCase()+" (runid: "+this.config.getRunId()+")");
        }
    }

    private List<String> readListOfBranches() {
        List<String> result = new ArrayList<>();
        File inputFile = new File(this.config.getInputPath());

        try {
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            while (reader.ready()) {
                String line = reader.readLine().trim();
                result.add(line);
            }

            reader.close();
        } catch (IOException e) {
            throw new RuntimeException("Error while reading branches from file: "+inputFile.getPath(),e);
        }

        return result;
    }

    private void runPool() throws IOException {
        String runId = this.config.getRunId();

        List<String> branchNames = this.readListOfBranches();
        LOGGER.info("Find "+branchNames.size()+" branches to run.");

        this.docker = DockerHelper.initDockerClient();
        String imageId = DockerHelper.findDockerImage(this.config.getDockerImageName(), this.docker);
        LOGGER.info("Found the following docker image id: "+imageId);

        ExecutorService executorService = Executors.newFixedThreadPool(this.config.getNbThreads());

        for (String branchName : branchNames) {
            RunnablePipelineContainer runnablePipelineContainer = new RunnablePipelineContainer(imageId, branchName);
            submittedRunnablePipelineContainers.add(runnablePipelineContainer);
            executorService.submit(runnablePipelineContainer);
        }

        executorService.shutdown();
        try {
            if (executorService.awaitTermination(this.config.getGlobalTimeout(), TimeUnit.DAYS)) {
                LOGGER.info("Job finished within time.");
            } else {
                LOGGER.warn("Timeout launched: the job is running for one day. Force stopped "+ submittedRunnablePipelineContainers.size()+" docker container(s).");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOGGER.error("Error while await termination. Force stopped "+ submittedRunnablePipelineContainers.size()+" docker container(s).", e);
            executorService.shutdownNow();
        }

        this.docker.close();
        if (this.endProcessNotifier != null) {
            this.endProcessNotifier.notifyEnd();
        }
    }

    public static void main(String[] args) throws Exception {
        Launcher launcher = new Launcher(args);
        launcher.runPool();
    }

}
