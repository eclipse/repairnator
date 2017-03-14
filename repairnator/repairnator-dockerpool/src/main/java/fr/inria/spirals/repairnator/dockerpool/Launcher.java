package fr.inria.spirals.repairnator.dockerpool;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.stringparsers.FileStringParser;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerExit;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.Image;
import fr.inria.spirals.repairnator.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by urli on 13/03/2017.
 */
public class Launcher {

    private static final String ENTRY_CMD="java -cp $JAVA_HOME/lib/tools.jar:repairnator-pipeline.jar fr.inria.spirals.repairnator.pipeline.Launcher -m repair -d -b ";

    private static Logger LOGGER = LoggerFactory.getLogger(Launcher.class);
    private JSAP jsap;
    private JSAPResult arguments;

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

        opt2 = new FlaggedOption("buildId");
        opt2.setShortFlag('b');
        opt2.setLongFlag("buildId");
        opt2.setStringParser(JSAP.INTEGER_PARSER);
        opt2.setRequired(true);
        opt2.setHelp("Specify the buildId to run.");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("logDirectory");
        opt2.setShortFlag('l');
        opt2.setLongFlag("logDirectory");
        opt2.setStringParser(FileStringParser.getParser().setMustBeDirectory(true).setMustExist(true));
        opt2.setDefault("/var/log/repairnator");
        opt2.setHelp("Specify where to put logs created by docker machines.");
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

    private void managePoolOfThreads() throws DockerCertificateException, DockerException, InterruptedException {
        final DockerClient docker = DefaultDockerClient.fromEnv().build();

        List<Image> allImages = docker.listImages(DockerClient.ListImagesParam.byName(this.arguments.getString("imageName")));
        String imageId = null;

        for (Image image : allImages) {
            if (image.repoTags().contains(this.arguments.getString("imageName"))) {
                imageId = image.id();
                break;
            }
        }

        if (imageId != null) {

            String containerName = "repairnator-pipeline_"+Utils.formatFilenameDate(new Date())+"_"+this.arguments.getInt("buildId");
            String logFileName = containerName+".log";
            String[] envValues = new String[] { "BUILD_ID="+this.arguments.getInt("buildId"), "LOG_FILENAME="+logFileName};

            Map<String,String> labels = new HashMap<>();
            labels.put("name",containerName);
            HostConfig hostConfig = HostConfig.builder().appendBinds(this.arguments.getFile("logDirectory").getAbsolutePath()+":/var/log").build();
            ContainerConfig containerConfig = ContainerConfig.builder()
                                                        .image(imageId)
                                                        .env(envValues)
                                                        .hostname(Utils.getHostname())
                                                        .hostConfig(hostConfig)
                                                        .labels(labels)
                                                        .build();

            LOGGER.info("Create the container: "+containerName);
            ContainerCreation container = docker.createContainer(containerConfig);
            LOGGER.info("Start the container: "+containerName);
            docker.startContainer(container.id());

            ContainerExit exitStatus = docker.waitContainer(container.id());

            LOGGER.info("The container has finished with status code: "+exitStatus.statusCode());
            docker.removeContainer(container.id());
            docker.close();
        } else {
            docker.close();
        }

    }

    public Launcher(String[] args) throws JSAPException {
        this.defineArgs();
        this.arguments = jsap.parse(args);
        this.checkArguments();
    }

    public static void main(String[] args) throws Exception {
        Launcher launcher = new Launcher(args);
        launcher.managePoolOfThreads();
    }
}
