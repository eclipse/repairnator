package fr.inria.spirals.repairnator.process.step;

import fr.inria.lille.repair.ProjectReference;
import fr.inria.lille.repair.common.config.Config;
import fr.inria.lille.repair.common.patch.Patch;
import fr.inria.lille.repair.nopol.NoPol;
import fr.inria.spirals.repairnator.Launcher;
import fr.inria.spirals.repairnator.process.ProjectInspector;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by urli on 05/01/2017.
 */
public class NopolRepair extends AbstractStep {
    private static final String CLASSPATH_FILENAME = "classpath.info";
    private static final String DEFAULT_SRC_DIR = "/src/main/java";
    private static final String DEFAULT_CLASSES_DIR = "/target/classes";
    private static final String DEFAULT_TEST_CLASSES_DIR = "/target/test-classes";

    private List<Patch> patches;

    public NopolRepair(ProjectInspector inspector) {
        super(inspector);
    }

    public List<Patch> getPatches() {
        return patches;
    }

    @Override
    protected void businessExecute() {
        Launcher.LOGGER.debug("Start operation to repair failing build.");

        GatherTestInformation infoStep = inspector.getTestInformations();
        String incriminatedModule = infoStep.getFailingModulePath();
        Map<String, Map<String, String>> infoTests = infoStep.getTypeOfFailures();

        Set<String> failingTests = new HashSet<String>();

        for (Map<String,String> failures : infoTests.values()) {
            for (String testNames : failures.keySet()) {
                String[] splitName = testNames.split(":");
                if (splitName.length != 2) {
                    Launcher.LOGGER.error("Error while splitting test name: "+testNames+". It won't be considered for Nopol.");
                    this.addStepError("Error while splitting test name: "+testNames+". It won't be considered for Nopol.");
                } else {
                    failingTests.add(splitName[0]);
                }
            }
        }

        Launcher.LOGGER.debug("Try to install multimodule to avoid missing dependencies...");

        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile( new File( this.getPom() ) );
        request.setGoals( Arrays.asList( "install" ) );

        Invoker invoker = new DefaultInvoker();

        System.setProperty("maven.test.skip","true");

        InvocationResult result = null;

        try {
            result = invoker.execute(request);
        } catch (MavenInvocationException e) {
            Launcher.LOGGER.debug("Error while installing multimodule maven: "+e);
            this.addStepError(e.getMessage());
        }

        System.clearProperty("maven.test.skip");

        if (result != null && result.getExitCode() != 0) {
            Launcher.LOGGER.debug("Error while installing multimodule maven: "+result.getExecutionException());
            this.addStepError("Error while installing multimodule maven: "+result.getExecutionException());
        }

        Launcher.LOGGER.debug("Compute classpath from incriminated module...");
        System.setProperty("mdep.outputFile",CLASSPATH_FILENAME);
        String goal = "dependency:build-classpath";
        String pomModule = incriminatedModule+File.separator+"pom.xml";

        request = new DefaultInvocationRequest();
        request.setPomFile( new File( pomModule ) );
        request.setGoals( Arrays.asList( goal ) );

        invoker = new DefaultInvoker();

        try {
            result = invoker.execute(request);
        } catch (MavenInvocationException e) {
            Launcher.LOGGER.debug("Error while computing classpath maven: "+e);
            this.addStepError(e.getMessage());
            return;
        }

        if (result != null && result.getExitCode() != 0) {
            Launcher.LOGGER.debug("Error while computing classpath maven: "+result.getExecutionException());
            this.addStepError("Error while computing classpath maven: "+result.getExecutionException());
            return;
        }

        System.clearProperty("mdep.outputfile");


        String classpathPath = incriminatedModule+File.separator+CLASSPATH_FILENAME;
        String classPath = incriminatedModule+File.separator+DEFAULT_CLASSES_DIR+":"+incriminatedModule+File.separator+DEFAULT_TEST_CLASSES_DIR+":";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(classpathPath)));
            classPath += reader.readLine();
        } catch (IOException e) {
            Launcher.LOGGER.error("Problem while getting classpath: "+e);
            this.addStepError(e.getMessage());
            return;
        }

        File sourceDir = new File(incriminatedModule+DEFAULT_SRC_DIR);

        if (!sourceDir.isDirectory() || !sourceDir.canRead()) {
            Launcher.LOGGER.error("Source dir "+sourceDir.getAbsolutePath()+" is not a directory or cannot be read.");
            this.addStepError("Source dir "+sourceDir.getAbsolutePath()+" is not a directory or cannot be read.");
            return;
        }

        ProjectReference projectReference = new ProjectReference(sourceDir.getAbsolutePath(), classPath, failingTests.toArray(new String[failingTests.size()]));
        Config config = new Config();
        config.setTimeoutTestExecution(10);
        config.setLocalizer(Config.NopolLocalizer.GZOLTAR);
        NoPol nopol = new NoPol(projectReference, config);
        this.patches = nopol.build();

        this.setState(ProjectState.PATCHED);

    }


}
