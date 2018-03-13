package fr.inria.spirals.repairnator.checkbranches;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Image;
import fr.inria.spirals.repairnator.LauncherType;
import fr.inria.spirals.repairnator.LauncherUtils;
import fr.inria.spirals.repairnator.notifier.EndProcessNotifier;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
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
    private JSAP jsap;
    private JSAPResult arguments;
    private RepairnatorConfig config;
    private EndProcessNotifier endProcessNotifier;

    public static List<RunnablePipelineContainer> submittedRunnablePipelineContainers = new CopyOnWriteArrayList<>();
    public static DockerClient docker;

    private void defineArgs() throws JSAPException {
        // Verbose output
        this.jsap = new JSAP();

        // -h or --help
        this.jsap.registerParameter(LauncherUtils.defineArgHelp());
        // -d or --debug
        this.jsap.registerParameter(LauncherUtils.defineArgDebug());
        // --runId
        this.jsap.registerParameter(LauncherUtils.defineArgRunId());
        // -i or --input
        this.jsap.registerParameter(LauncherUtils.defineArgInput("Specify the input file containing the list of branches to reproduce"));
        // -o or --output
        this.jsap.registerParameter(LauncherUtils.defineArgOutput(true, false, true, false, "Specify where to put output data"));
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
        // -t or --threads
        this.jsap.registerParameter(LauncherUtils.defineArgThreads());
        // -g or --globalTimeout
        this.jsap.registerParameter(LauncherUtils.defineArgGlobalTimeout());

        Switch sw1 = new Switch("humanPatch");
        sw1.setShortFlag('p');
        sw1.setLongFlag("humanPatch");
        sw1.setDefault("false");
        this.jsap.registerParameter(sw1);

        FlaggedOption opt2 = new FlaggedOption("repository");
        opt2.setShortFlag('r');
        opt2.setLongFlag("repository");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setRequired(true);
        opt2.setHelp("Specify where to collect branches");
        this.jsap.registerParameter(opt2);
    }

    private List<String> readListOfBranches() {
        List<String> result = new ArrayList<>();
        File inputFile = this.arguments.getFile("input");

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

    private String findDockerImage() {
        try {
            docker = DefaultDockerClient.fromEnv().build();

            List<Image> allImages = docker.listImages(DockerClient.ListImagesParam.allImages());

            String imageId = null;
            for (Image image : allImages) {
                if (image.repoTags() != null && image.repoTags().contains(this.arguments.getString("imageName"))) {
                    imageId = image.id();
                    break;
                }
            }

            if (imageId == null) {
                throw new RuntimeException("There was a problem when looking for the docker image with argument \""+this.arguments.getString("imageName")+"\": no image has been found.");
            }
            return imageId;
        } catch (DockerCertificateException|InterruptedException|DockerException e) {
            throw new RuntimeException("Error while looking for the docker image",e);
        }
    }

    private void runPool() throws IOException {
        String runId = this.arguments.getString("runId");

        List<String> branchNames = this.readListOfBranches();
        LOGGER.info("Find "+branchNames.size()+" branches to run.");

        String imageId = this.findDockerImage();
        LOGGER.info("Found the following docker image id: "+imageId);

        ExecutorService executorService = Executors.newFixedThreadPool(this.arguments.getInt("threads"));

        for (String branchName : branchNames) {
            RunnablePipelineContainer runnablePipelineContainer = new RunnablePipelineContainer(imageId, this.arguments.getString("repository"), branchName, this.arguments.getFile("output").getAbsolutePath(), this.arguments.getBoolean("skipDelete"), this.arguments.getBoolean("humanPatch"));
            submittedRunnablePipelineContainers.add(runnablePipelineContainer);
            executorService.submit(runnablePipelineContainer);
        }

        executorService.shutdown();
        try {
            if (executorService.awaitTermination(this.arguments.getInt("globalTimeout"), TimeUnit.DAYS)) {
                LOGGER.info("Job finished within time.");
            } else {
                LOGGER.warn("Timeout launched: the job is running for one day. Force stopped "+ submittedRunnablePipelineContainers.size()+" docker container(s).");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOGGER.error("Error while await termination. Force stopped "+ submittedRunnablePipelineContainers.size()+" docker container(s).", e);
            executorService.shutdownNow();
        }

        docker.close();
        if (this.endProcessNotifier != null) {
            this.endProcessNotifier.notifyEnd();
        }
    }

    private Launcher(String[] args) throws JSAPException {
        this.defineArgs();
        this.arguments = jsap.parse(args);
        LauncherUtils.checkArguments(this.jsap, this.arguments, LauncherType.CHECKBRANCHES);

        this.initConfig();
        this.initNotifiers();
    }

    private void initNotifiers() {
        if (this.arguments.getBoolean("notifyEndProcess")) {
            List<NotifierEngine> notifierEngines = LauncherUtils.initNotifierEngines(this.arguments, LOGGER);
            this.endProcessNotifier = new EndProcessNotifier(notifierEngines, LauncherType.CHECKBRANCHES.name().toLowerCase()+" (runid: "+this.arguments.getString("runId")+")");
        }
    }

    private void initConfig() {
        this.config = RepairnatorConfig.getInstance();

        this.config.setRunId(this.arguments.getString("runId"));
        this.config.setSmtpServer(this.arguments.getString("smtpServer"));
        this.config.setNotifyTo(this.arguments.getStringArray("notifyto"));
    }

    public static void main(String[] args) throws Exception {
        Launcher launcher = new Launcher(args);
        launcher.runPool();
    }
}
