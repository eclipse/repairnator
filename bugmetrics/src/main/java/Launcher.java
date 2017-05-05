import com.martiansoftware.jsap.*;
import com.martiansoftware.jsap.stringparsers.FileStringParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by fermadeiral on 02/05/17.
 */
public class Launcher {

    private static Logger LOGGER = LoggerFactory.getLogger(Launcher.class);
    private JSAP jsap;
    private JSAPResult arguments;
    private String jsonFileFolderPath;
    private String outputPath;

    public Launcher(String[] args) throws JSAPException {
        this.defineArgs();
        this.arguments = jsap.parse(args);
        this.checkArgs();
        this.initArgs();
    }

    private void defineArgs() throws JSAPException {
        this.jsap = new JSAP();

        Switch sw1 = new Switch("help");
        sw1.setShortFlag('h');
        sw1.setLongFlag("help");
        sw1.setDefault("false");
        this.jsap.registerParameter(sw1);

        sw1 = new Switch("debug");
        sw1.setShortFlag('d');
        sw1.setLongFlag("debug");
        sw1.setDefault("false");
        this.jsap.registerParameter(sw1);

        FlaggedOption opt2 = new FlaggedOption("jsonFileFolderPath");
        opt2.setShortFlag('i');
        opt2.setLongFlag("jsonFileFolderPath");
        opt2.setDefault("./alljson");
        opt2.setStringParser(FileStringParser.getParser().setMustBeDirectory(true).setMustExist(true));
        opt2.setHelp("Specify the path to the folder that contains the json files");
        this.jsap.registerParameter(opt2);

        opt2 = new FlaggedOption("outputPath");
        opt2.setShortFlag('o');
        opt2.setLongFlag("outputPath");
        opt2.setDefault("./output");
        opt2.setStringParser(FileStringParser.getParser().setMustBeDirectory(true).setMustExist(true));
        opt2.setHelp("Specify the path to output the metrics");
        this.jsap.registerParameter(opt2);
    }

    private void checkArgs() {
        if (!this.arguments.success()) {
            for (java.util.Iterator<?> errs = arguments.getErrorMessageIterator(); errs.hasNext();) {
                System.err.println("Error: " + errs.next());
            }
            this.printUsage();
        }

        if (this.arguments.getBoolean("help")) {
            this.printUsage();
        }
    }

    private void initArgs() {
        this.jsonFileFolderPath = this.arguments.getFile("jsonFileFolderPath").getPath();
        this.outputPath = this.arguments.getFile("outputPath").getPath();
    }

    private void printUsage() {
        System.err.println("Usage option:");
        System.err.println();
        System.err.println(jsap.getHelp());
        System.exit(-1);
    }

    private void mainProcess() throws IOException {
        LOGGER.info("Starting...");

        JsonParser jsonParser = new JsonParser(jsonFileFolderPath);
        jsonParser.run();

        LOGGER.info("Process is finished");
        System.exit(0);
    }

    public static void main(String[] args) throws IOException, JSAPException {
        Launcher launcher = new Launcher(args);
        launcher.mainProcess();
    }

}
