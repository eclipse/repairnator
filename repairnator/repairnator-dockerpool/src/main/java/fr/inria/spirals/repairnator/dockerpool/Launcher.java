package fr.inria.spirals.repairnator.dockerpool;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.SearchItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by urli on 13/03/2017.
 */
public class Launcher {

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

    private void managePoolOfThreads() {
        DockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerCmdExecFactory dockerCmdExecFactory = new JerseyDockerCmdExecFactory()
                .withReadTimeout(1000)
                .withConnectTimeout(1000)
                .withMaxTotalConnections(100)
                .withMaxPerRouteConnections(10);

        DockerClient dockerClient = DockerClientBuilder.getInstance(dockerClientConfig)
                .withDockerCmdExecFactory(dockerCmdExecFactory)
                .build();

        List<Image> images = dockerClient.listImagesCmd().withImageNameFilter(this.arguments.getString("imageName")).exec();

        if (images.size() != 1) {
            throw new RuntimeException("The name should be precise enough to filter one and only one image, here "+images.size()+" image(s) has been retrieved for the following name: "+this.arguments.getString("imageName"));
        }

        Image image = images.get(0);

    }

    public Launcher(String[] args) throws JSAPException {
        this.defineArgs();
        this.arguments = jsap.parse(args);
        this.checkArguments();
    }

    public static void main(String[] args) throws JSAPException {
        Launcher launcher = new Launcher(args);
        launcher.managePoolOfThreads();
    }
}
