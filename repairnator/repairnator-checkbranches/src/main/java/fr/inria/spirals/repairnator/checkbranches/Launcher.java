package fr.inria.spirals.repairnator.checkbranches;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.stringparsers.FileStringParser;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Image;
import fr.inria.spirals.repairnator.notifier.EndProcessNotifier;
import fr.inria.spirals.repairnator.notifier.engines.EmailNotifierEngine;
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

        // help
        Switch sw1 = new Switch("help");
        sw1.setShortFlag('h');
        sw1.setLongFlag("help");
        sw1.setDefault("false");
        this.jsap.registerParameter(sw1);

        // verbosity
        sw1 = new Switch("debug");
        sw1.setShortFlag('d');
        sw1.setLongFlag("debug");
        sw1.setDefault("false");
        this.jsap.registerParameter(sw1);

        sw1 = new Switch("skipDelete");
        sw1.setLongFlag("skipDelete");
        sw1.setDefault("false");
        sw1.setHelp("Skip the deletion of docker container.");
        this.jsap.registerParameter(sw1);

        sw1 = new Switch("notifyEndProcess");
        sw1.setLongFlag("notifyEndProcess");
        sw1.setDefault("false");
        sw1.setHelp("Activate the notification when the process ends.");
        this.jsap.registerParameter(sw1);

        FlaggedOption opt2 = new FlaggedOption("imageName");
        opt2.setShortFlag('n');
        opt2.setLongFlag("name");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setRequired(true);
        opt2.setHelp("Specify the docker image name to use.");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("input");
        opt2.setShortFlag('i');
        opt2.setLongFlag("input");
        opt2.setStringParser(FileStringParser.getParser().setMustBeFile(true).setMustExist(true));
        opt2.setRequired(true);
        opt2.setHelp("Specify the input file containing the list of branches to reproduce");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("output");
        opt2.setShortFlag('o');
        opt2.setLongFlag("output");
        opt2.setStringParser(FileStringParser.getParser().setMustBeFile(true));
        opt2.setRequired(true);
        opt2.setHelp("Specify where to put output data");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("logDirectory");
        opt2.setShortFlag('l');
        opt2.setLongFlag("logDirectory");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setRequired(true);
        opt2.setHelp("Specify where to put logs and serialized files created by docker machines.");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("threads");
        opt2.setShortFlag('t');
        opt2.setLongFlag("threads");
        opt2.setStringParser(JSAP.INTEGER_PARSER);
        opt2.setDefault("2");
        opt2.setHelp("Specify the number of threads to run in parallel");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("globalTimeout");
        opt2.setShortFlag('g');
        opt2.setLongFlag("globalTimeout");
        opt2.setStringParser(JSAP.INTEGER_PARSER);
        opt2.setDefault("1");
        opt2.setHelp("Specify the number of day before killing the whole pool.");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("runId");
        opt2.setLongFlag("runId");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("Specify the run id for this launch.");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("smtpServer");
        opt2.setLongFlag("smtpServer");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("Specify SMTP server to use for Email notification");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("notifyto");
        opt2.setLongFlag("notifyto");
        opt2.setList(true);
        opt2.setListSeparator(',');
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("Specify email adresses to notify");
        this.jsap.registerParameter(opt2);
    }

    private void checkArguments() {
        if (!this.arguments.success()) {
            // print out specific error messages describing the problems
            for (java.util.Iterator<?> errs = arguments.getErrorMessageIterator(); errs.hasNext();) {
                System.err.println("Error: " + errs.next());
            }
            this.printUsage();
        }

        if (this.arguments.getBoolean("help")) {
            this.printUsage();
        }
    }

    private void printUsage() {
        System.err.println("Usage: java <repairnator-dockerpool name> [option(s)]");
        System.err.println();
        System.err.println("Options : ");
        System.err.println();
        System.err.println(jsap.getHelp());
        System.exit(-1);
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
            RunnablePipelineContainer runnablePipelineContainer = new RunnablePipelineContainer(imageId, branchName, this.arguments.getBoolean("skipDelete"));
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
                this.setStatusForUnexecutedJobs();
            }
        } catch (InterruptedException e) {
            LOGGER.error("Error while await termination. Force stopped "+ submittedRunnablePipelineContainers.size()+" docker container(s).", e);
            executorService.shutdownNow();
            this.setStatusForUnexecutedJobs();
        }

        docker.close();
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
        this.checkArguments();

        this.initConfig();
        this.initNotifiers();
    }

    private void initNotifiers() {
        if (this.arguments.getBoolean("notifyEndProcess")) {
            List<NotifierEngine> notifierEngines = new ArrayList<>();
            if (this.arguments.getString("smtpServer") != null && this.arguments.getStringArray("notifyto") != null) {
                LOGGER.info("The email notifier engine will be used.");

                notifierEngines.add(new EmailNotifierEngine(this.arguments.getStringArray("notifyto"), this.arguments.getString("smtpServer")));
            } else {
                LOGGER.info("The email notifier engine won't be used.");
            }

            this.endProcessNotifier = new EndProcessNotifier(notifierEngines, "dockerpool - (runid: "+this.config.getRunId()+")");
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
