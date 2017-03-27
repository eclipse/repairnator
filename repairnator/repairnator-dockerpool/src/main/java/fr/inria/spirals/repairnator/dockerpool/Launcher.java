package fr.inria.spirals.repairnator.dockerpool;
import com.google.api.client.auth.oauth2.Credential;
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
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.serializer.gspreadsheet.GoogleSpreadSheetFactory;
import fr.inria.spirals.repairnator.serializer.gspreadsheet.ManageGoogleAccessToken;
import fr.inria.spirals.repairnator.dockerpool.serializer.TreatedBuildTracking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
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
    private String accessToken;

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
        opt2.setHelp("Specify the input file containing the list of build ids.");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("logDirectory");
        opt2.setShortFlag('l');
        opt2.setLongFlag("logDirectory");
        opt2.setStringParser(FileStringParser.getParser().setMustBeDirectory(true).setMustExist(true));
        opt2.setDefault("/var/log/repairnator");
        opt2.setHelp("Specify where to put logs created by docker machines.");
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

        opt2 = new FlaggedOption("googleSecretPath");
        opt2.setShortFlag('s');
        opt2.setLongFlag("googleSecretPath");
        opt2.setStringParser(FileStringParser.getParser().setMustBeFile(true).setMustExist(true));
        opt2.setDefault("./client_secret.json");
        opt2.setHelp("Specify the path to the JSON google secret for serializing.");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("runId");
        opt2.setLongFlag("runId");
        opt2.setStringParser(JSAP.STRING_PARSER);
        opt2.setHelp("Specify the run id for this launch.");
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

    private void checkEnvironmentVariables() {
        for (String envVar : Utils.ENVIRONMENT_VARIABLES) {
            if (System.getenv(envVar) == null || System.getenv(envVar).equals("")) {
                System.err.println("You must set the following environment variable: "+envVar);
                this.printUsage();
            }
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

    private List<Integer> readListOfBuildIds() {
        List<Integer> result = new ArrayList<>();
        File inputFile = this.arguments.getFile("input");

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

    private String findDockerImage() {
        try {
            this.docker = DefaultDockerClient.fromEnv().build();

            List<Image> allImages = docker.listImages(DockerClient.ListImagesParam.allImages());

            String imageId = null;
            for (Image image : allImages) {
                if (image.repoTags().contains(this.arguments.getString("imageName"))) {
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
        List<Integer> buildIds = this.readListOfBuildIds();
        LOGGER.info("Find "+buildIds.size()+" builds to run.");

        String imageId = this.findDockerImage();
        LOGGER.info("Found the following docker image id: "+imageId);

        String logFile = this.arguments.getFile("logDirectory").getAbsolutePath();

        ExecutorService executorService = Executors.newFixedThreadPool(this.arguments.getInt("threads"));

        for (Integer builId : buildIds) {
            TreatedBuildTracking googleSpreadSheetTreatedBuildTracking = new TreatedBuildTracking(this.arguments.getString("runId"), builId);
            RunnablePipelineContainer runnablePipelineContainer = new RunnablePipelineContainer(imageId, builId, logFile, this.arguments.getString("runId"), googleSpreadSheetTreatedBuildTracking, this.accessToken);
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
        this.checkEnvironmentVariables();

        try {
            GoogleSpreadSheetFactory.initWithFileSecret(this.arguments.getFile("googleSecretPath").getPath());

            ManageGoogleAccessToken manageGoogleAccessToken = ManageGoogleAccessToken.getInstance();
            Credential credential = manageGoogleAccessToken.getCredential();

            if (credential != null) {
                this.accessToken = credential.getAccessToken();
            }
        } catch (IOException | GeneralSecurityException e) {
            LOGGER.error("Error while initializing Google Spreadsheet, no information will be serialized in spreadsheets", e);
        }
    }

    public static void main(String[] args) throws Exception {
        Launcher launcher = new Launcher(args);
        launcher.runPool();
    }
}
