package fr.inria.spirals.repairnator.process.step;

import fr.inria.lille.repair.ProjectReference;
import fr.inria.lille.repair.common.config.Config;
import fr.inria.lille.repair.common.patch.Patch;
import fr.inria.lille.repair.nopol.NoPol;
import fr.inria.spirals.repairnator.process.ProjectInspector;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by urli on 05/01/2017.
 */
public class cNopolRepair extends AbstractStep {
    private static final String CLASSPATH_FILENAME = "classpath.info";
    private static final String DEFAULT_SRC_DIR = "/src/main/java";
    private static final String DEFAULT_CLASSES_DIR = "/target/classes";
    private static final String DEFAULT_TEST_CLASSES_DIR = "/target/test-classes";
    private static final String CMD_KILL_GZOLTAR_AGENT = "ps -ef | grep gzoltar | grep -v grep | awk '{print $2}' |xargs kill";

    private Map<String,List<Patch>> patches;

    private ProjectReference projectReference;

    public NopolRepair(ProjectInspector inspector) {
        super(inspector);
    }

    public Map<String,List<Patch>> getPatches() {
        return patches;
    }

    public ProjectReference getProjectReference() {
        return projectReference;
    }

    private String initClasspath(String incriminatedModule) {
        String result = incriminatedModule+File.separator+DEFAULT_CLASSES_DIR+":";
        result += incriminatedModule+File.separator+DEFAULT_TEST_CLASSES_DIR+":";
        result += System.getenv("JAVA_HOME")+"/../lib/tools.jar:";
        return result;
    }

    @Override
    protected void businessExecute() {
        this.getLogger().debug("Start operation to repair failing build.");

        GatherTestInformation infoStep = inspector.getTestInformations();
        String incriminatedModule = infoStep.getFailingModulePath();
        Map<String, Map<String, String>> infoTests = infoStep.getTypeOfFailures();

        Set<String> failingTests = new HashSet<String>();

        for (Map<String,String> failures : infoTests.values()) {
            for (String testNames : failures.keySet()) {
                String[] splitName = testNames.split(":");
                if (splitName.length != 2) {
                    this.getLogger().error("Error while splitting test name: "+testNames+". It won't be considered for Nopol.");
                    this.addStepError("Error while splitting test name: "+testNames+". It won't be considered for Nopol.");
                } else {
                    failingTests.add(splitName[0]);
                }
            }
        }

        this.getLogger().debug("Try to install multimodule to avoid missing dependencies...");

        Properties properties = new Properties();
        properties.setProperty("maven.test.skip","true");

        MavenHelper helper = new MavenHelper(this.getPom(), "install", properties, this.getClass().getName(), this.inspector, true);

        int result = helper.run();

        if (result != MavenHelper.MAVEN_SUCCESS) {
            this.getLogger().debug("Error while installing multimodule maven");
        }

        this.getLogger().debug("Compute classpath from incriminated module...");
        properties = new Properties();
        properties.setProperty("mdep.outputFile",CLASSPATH_FILENAME);

        String goal = "dependency:build-classpath";
        String pomModule = incriminatedModule+File.separator+"pom.xml";

        helper = new MavenHelper(pomModule, goal, properties, this.getClass().getName(), this.inspector, true);

        result = helper.run();

        if (result != MavenHelper.MAVEN_SUCCESS) {
            this.getLogger().debug("Error while computing classpath maven");
            return;
        }


        String classpathPath = incriminatedModule+File.separator+CLASSPATH_FILENAME;
        String classPath = initClasspath(incriminatedModule);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(classpathPath)));
            classPath += reader.readLine();
        } catch (IOException e) {
            this.getLogger().error("Problem while getting classpath: "+e);
            this.addStepError(e.getMessage());
            return;
        }

        File sourceDir = new File(incriminatedModule+DEFAULT_SRC_DIR);

        if (!sourceDir.isDirectory() || !sourceDir.canRead()) {
            this.getLogger().error("Source dir "+sourceDir.getAbsolutePath()+" is not a directory or cannot be read.");
            this.addStepError("Source dir "+sourceDir.getAbsolutePath()+" is not a directory or cannot be read.");
            return;
        }

        this.patches = new HashMap<String,List<Patch>>();
        boolean patchCreated = false;

        for (String failingTest : failingTests) {
            this.getLogger().debug("Launching repair with Nopol for following test: "+failingTest+"...");

            this.projectReference = new ProjectReference(sourceDir.getAbsolutePath(), classPath, new String[] {failingTest});
            Config config = new Config();
            config.setTimeoutTestExecution(5);
            config.setMaxTimeInMinutes(1);
            config.setLocalizer(Config.NopolLocalizer.GZOLTAR);
            config.setSolverPath(this.inspector.getNopolSolverPath());
            config.setSynthesis(Config.NopolSynthesis.DYNAMOTH);
            final NoPol nopol = new NoPol(projectReference, config);
            List<Patch> patch = null;

            final ExecutorService executor = Executors.newSingleThreadExecutor();
            final Future nopolExecution = executor.submit(
                    new Callable() {
                        @Override
                        public Object call() throws Exception {
                            return nopol.build();
                        }
                    });
            try {
                patch = (List<Patch>) nopolExecution.get(config.getMaxTimeInMinutes(), TimeUnit.MINUTES);
            } catch (TimeoutException exception) {
                this.getLogger().error("Timeout: execution time > " + config.getMaxTimeInMinutes() + " " + TimeUnit.MINUTES);
                this.addStepError("Timeout: execution time > " + config.getMaxTimeInMinutes() + " " + TimeUnit.MINUTES);

                // Really ugly but did not find any other way to kill agents...
                try {
                    Runtime.getRuntime().exec(CMD_KILL_GZOLTAR_AGENT);
                } catch (IOException e) {
                    this.getLogger().error("Error while killing gzoltar agent using following command: "+CMD_KILL_GZOLTAR_AGENT);
                    this.getLogger().error(e.getMessage());
                }
                continue;
            } catch (InterruptedException e) {
                this.getLogger().error(e.getMessage());
                this.addStepError(e.getMessage());
                continue;
            } catch (ExecutionException e) {
                this.getLogger().error(e.getMessage());
                this.addStepError(e.getMessage());
                continue;
            }

            if (patch != null && !patch.isEmpty()) {
                this.patches.put(failingTest, patch);
                patchCreated = true;
            }
        }
        this.projectReference = new ProjectReference(sourceDir.getAbsolutePath(), classPath, failingTests.toArray(new String[failingTests.size()]));

        if (!patchCreated) {
            this.addStepError("No patch has been generated by Nopol. Look at the trace to get more information.");
            return;
        }
        this.setState(ProjectState.PATCHED);

    }


}
