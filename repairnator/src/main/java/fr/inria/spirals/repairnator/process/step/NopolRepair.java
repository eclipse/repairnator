package fr.inria.spirals.repairnator.process.step;

import fr.inria.lille.commons.synthesis.smt.solver.SolverFactory;
import fr.inria.lille.repair.ProjectReference;
import fr.inria.lille.repair.common.config.Config;
import fr.inria.lille.repair.common.patch.Patch;
import fr.inria.lille.repair.common.synth.StatementType;
import fr.inria.lille.repair.nopol.NoPol;
import fr.inria.spirals.repairnator.process.ProjectInspector;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.profile.DefaultProfileSelector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
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
public class NopolRepair extends AbstractStep {
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

    private List<URL> initClasspath(String incriminatedModule) throws MalformedURLException {
        List<URL> result = new ArrayList<URL>();
        result.add(new URL("file://"+incriminatedModule+File.separator+DEFAULT_CLASSES_DIR));
        result.add(new URL("file://"+incriminatedModule+File.separator+DEFAULT_TEST_CLASSES_DIR));
        return result;
    }

    private File[] searchForSourcesDirectory(String incriminatedModulePath, boolean rootCall) {
        List<File> result = new ArrayList<File>();
        File defaultSourceDir = new File(incriminatedModulePath+DEFAULT_SRC_DIR);

        if (defaultSourceDir.exists()) {
            result.add(defaultSourceDir);
            return result.toArray(new File[result.size()]);
        }

        this.getLogger().debug("The default source directory ("+defaultSourceDir.getPath()+") does not exists. Try to read pom.xml to get informations.");
        File pomIncriminatedModule = new File(incriminatedModulePath+"/pom.xml");

        try
        {
            Model model = MavenHelper.readPomXml(pomIncriminatedModule, this.inspector.getM2LocalPath());

            Build buildSection = model.getBuild();

            if (buildSection != null) {
                String pathSrcDirFromPom = model.getBuild().getSourceDirectory();

                File srcDirFromPom = new File(pathSrcDirFromPom);

                if (srcDirFromPom.exists()) {
                    result.add(srcDirFromPom);
                    return result.toArray(new File[result.size()]);
                }

                this.getLogger().debug("The source directory given in pom.xml ("+pathSrcDirFromPom+") does not exists. Try to get source dir from all modules if multimodule.");
            } else {
                this.getLogger().debug("Build section does not exists in this pom.xml. Try to get source dir from all modules.");
            }

            if (model.getParent() != null && rootCall) {
                String relativePath = "../pom.xml";

                if (model.getParent().getRelativePath() != null) {
                    relativePath = model.getParent().getRelativePath();
                }

                File parentPomXml = new File(incriminatedModulePath+File.separator+relativePath);

                model = MavenHelper.readPomXml(parentPomXml, this.inspector.getM2LocalPath());

                for (String module : model.getModules()) {
                    File[] srcDir = this.searchForSourcesDirectory(parentPomXml.getParent()+File.separator+module, false);
                    if (srcDir != null) {
                        result.addAll(Arrays.asList(srcDir));
                    }
                }

                if (result.size() > 0) {
                    return result.toArray(new File[result.size()]);
                } else {
                    return null;
                }
            } else {
                if (rootCall) {
                    this.addStepError("Source directory is not at default location or specified in build section and no parent can be found.");
                }
            }

        }
        catch ( ModelBuildingException e ) {
            this.addStepError("Error while building pom.xml model: "+e);
        }
        return null;
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

        this.getLogger().debug("Compute classpath from incriminated module...");
        Properties properties = new Properties();
        properties.setProperty("mdep.outputFile",CLASSPATH_FILENAME);

        String goal = "dependency:build-classpath";
        String pomModule = incriminatedModule+File.separator+"pom.xml";

        MavenHelper helper = new MavenHelper(pomModule, goal, properties, this.getClass().getName(), this.inspector, true);

        int result = helper.run();

        if (result != MavenHelper.MAVEN_SUCCESS) {
            this.getLogger().debug("Error while computing classpath maven");
            return;
        }


        String classpathPath = incriminatedModule+File.separator+CLASSPATH_FILENAME;
        List<URL> classPath;

        try {
            classPath = initClasspath(incriminatedModule);
            BufferedReader reader = new BufferedReader(new FileReader(new File(classpathPath)));
            String classpathLine = reader.readLine();

            String[] allJars = classpathLine.split(":");
            for (String jar : allJars) {
                classPath.add(new URL("file://"+jar));
            }
        } catch (IOException e) {
            this.addStepError("Problem while getting classpath: "+e);
            return;
        }

        File[] sources = this.searchForSourcesDirectory(incriminatedModule, true);

        if (sources == null) {
             this.addStepError("Fail to find the sources directory.");
            return;
        }

        this.patches = new HashMap<String,List<Patch>>();
        boolean patchCreated = false;

        for (String failingTest : failingTests) {
            this.getLogger().debug("Launching repair with Nopol for following test: "+failingTest+"...");

            this.projectReference = new ProjectReference(sources, classPath.toArray(new URL[classPath.size()]), new String[] {failingTest});
            Config config = new Config();
            config.setTimeoutTestExecution(5);
            config.setMaxTimeInMinutes(5);
            config.setLocalizer(Config.NopolLocalizer.GZOLTAR);
            config.setSolverPath(this.inspector.getNopolSolverPath());
            config.setSynthesis(Config.NopolSynthesis.DYNAMOTH);
            config.setType(StatementType.PRE_THEN_COND);

            SolverFactory.setSolver(config.getSolver(), config.getSolverPath());

            final NoPol nopol = new NoPol(projectReference, config);
            List<Patch> patch = null;

            final ExecutorService executor = Executors.newSingleThreadExecutor();
            final Future nopolExecution = executor.submit(
                    new Callable() {
                        @Override
                        public Object call() throws Exception {
                            return nopol.build(projectReference.testClasses());
                        }
                    });
            try {
                executor.shutdown();
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

        if (!patchCreated) {
            this.addStepError("No patch has been generated by Nopol. Look at the trace to get more information.");
            return;
        }
        this.setState(ProjectState.PATCHED);

    }


}
