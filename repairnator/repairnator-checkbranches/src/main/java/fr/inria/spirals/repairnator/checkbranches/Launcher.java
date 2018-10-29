package fr.inria.spirals.repairnator.checkbranches;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.spotify.docker.client.DockerClient;
import fr.inria.spirals.repairnator.LauncherType;
import fr.inria.spirals.repairnator.LauncherUtils;
import fr.inria.spirals.repairnator.docker.DockerHelper;
import fr.inria.spirals.repairnator.notifier.EndProcessNotifier;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
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
        JSAP jsap = new JSAP();

        // -h or --help
        jsap.registerParameter(LauncherUtils.defineArgHelp());
        // -c or --configPath
        jsap.registerParameter(LauncherUtils.defineArgConfigPath());

        return jsap;
    }

    private void initConfig(JSAPResult arguments) {
        this.config = RepairnatorConfig.loadConfig(LauncherUtils.getArgConfigPath(arguments).getAbsolutePath());
        LauncherUtils.checkConfig(LauncherType.CHECKBRANCHES);
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

        File file = new File(this.config.getOutputPath());
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write("Analysis started at " + new Date()); writer.newLine(); writer.flush();
        writer.write("Considered repository: " + this.config.getRepository()); writer.newLine(); writer.flush();

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
        writer.write("Analysis finished at " + new Date()); writer.newLine(); writer.flush(); writer.close();

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
