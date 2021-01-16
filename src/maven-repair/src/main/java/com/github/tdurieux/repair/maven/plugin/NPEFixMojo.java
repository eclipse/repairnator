package com.github.tdurieux.repair.maven.plugin;

import exceptionparser.StackTrace;
import exceptionparser.StackTraceElement;
import exceptionparser.StackTraceParser;
import fr.inria.spirals.npefix.config.Config;
import fr.inria.spirals.npefix.main.DecisionServer;
import fr.inria.spirals.npefix.main.all.DefaultRepairStrategy;
import fr.inria.spirals.npefix.main.all.Launcher;
import fr.inria.spirals.npefix.main.all.TryCatchRepairStrategy;
import fr.inria.spirals.npefix.resi.CallChecker;
import fr.inria.spirals.npefix.resi.context.Decision;
import fr.inria.spirals.npefix.resi.context.Lapse;
import fr.inria.spirals.npefix.resi.context.NPEOutput;
import fr.inria.spirals.npefix.resi.exception.NoMoreDecision;
import fr.inria.spirals.npefix.resi.selector.*;
import fr.inria.spirals.npefix.resi.strategies.*;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.inspectors.InspectorFactory;
import fr.inria.spirals.repairnator.process.step.repair.NPERepair;
import fr.inria.spirals.repairnator.utils.Pair;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.surefire.log.api.NullConsoleLogger;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.surefire.report.ReportTestCase;
import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.plugins.surefire.report.SurefireReportParser;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReportException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@Mojo(name = "npefix", aggregator = true,
        defaultPhase = LifecyclePhase.TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class NPEFixMojo extends AbstractRepairMojo {

    /**
     * Location of the file.
     */
    @Parameter(defaultValue = "${project.build.directory}/npefix", property = "outputDir", required = true)
    private File outputDirectory;

    @Parameter(defaultValue = "${project.build.directory}/npefix", property = "resultDir", required = true)
    private File resultDirectory;

    @Parameter(defaultValue = "exploration", property = "selector", required = true)
    private String selector;

    @Parameter(defaultValue = "100", property = "laps", required = true)
    private int nbIteration;

    @Parameter(defaultValue = "class", property = "scope", required = true)
    private String scope;

    @Parameter(defaultValue = "default", property = "strategy", required = true)
    private String repairStrategy;

    @Parameter(readonly = true, defaultValue = "${project}")
    private MavenProject project;


    private File patchesJson;

    public void execute() throws MojoExecutionException {
        File tmpRepairnatorDir = com.google.common.io.Files.createTempDir();
        RepairnatorConfig.getInstance().setRepairTools(Collections.singleton("NPEFix"));
        RepairnatorConfig.getInstance().setOutputPath(tmpRepairnatorDir.getAbsolutePath());
        RepairnatorConfig.getInstance().setWorkspacePath(tmpRepairnatorDir.getAbsolutePath());
        RepairnatorConfig.getInstance().setLocalMavenRepository(localRepository.getBasedir());
        InspectorFactory.getMavenInspector(project.getBasedir().getAbsolutePath(), Collections.singletonList(new NPERepair()), null).run();

        List<File> patches = Arrays.asList(tmpRepairnatorDir.listFiles(((dir, name) -> name.startsWith("patches") && name.endsWith(".json"))));

        if (patches.size() == 0) {
            this.getLog().error("No patches have been found by NPEFix");
        } else {
            try {
                resultDirectory.mkdirs();
                patchesJson = new File(resultDirectory.getAbsolutePath() + "/" + patches.get(0).getName());
                patchesJson.createNewFile();
                com.google.common.io.Files.copy(patches.get(0), patchesJson);
            } catch (IOException e) {
                this.getLog().error(e);
            }
        }

        /*
        // get the failing NPE tests
        List<Pair<String, Set<File>>> npeTests = getNPETest();

        try {
            File currentDir = new File(".").getCanonicalFile();
            Config.CONFIG.setRootProject(currentDir.toPath().toAbsolutePath());
        } catch (IOException e) {
            getLog().error("Error while setting the root project path, the created patches might have absolute paths.");
        }
        final List<URL> dependencies = getClasspath();
        Set<File> sourceFolders = new HashSet<>();
        if ("project".equals(scope)) {
            sourceFolders = new HashSet<>(getSourceFolders());
        } else {
            for (Pair<String, Set<File>> test : npeTests) {
                sourceFolders.addAll(test.getValue());
            }
        }
        List<File> testFolders = getTestFolders();

        classpath(dependencies);

        // stop if we don't have at least one NPE
        if (npeTests.isEmpty()) {
            throw new RuntimeException("No failing test with NullPointerException or the NPE occurred outside the source.");
        }

        final String[] sources = new String[sourceFolders.size() /* + testFolders.size()*//*];
        int indexSource = 0;
        */

        /*for (int i = 0; i < testFolders.size(); i++, indexSource++) {
            String s = testFolders.get(i);
            sources[indexSource] = s;
            System.out.println("Test: " + s);
        }*/
        /*
            
        // collecting the source and bin folders
        for (File sourceFolder : sourceFolders) {
            sources[indexSource] = sourceFolder.getAbsolutePath();
            System.out.println("Source: " + sourceFolder);
            indexSource++;
        }
        File binFolder = new File(outputDirectory.getAbsolutePath() + "/npefix-bin");

        // setting up dependencies
        try {
            dependencies.add(binFolder.toURI().toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        if (!binFolder.exists()) {
            binFolder.mkdirs();
        }
        int complianceLevel = getComplianceLevel();
        complianceLevel = Math.max(complianceLevel, 5);
        System.out.println("ComplianceLevel: " + complianceLevel);

        Date initDate = new Date();

        // configuring NPEFix
        DefaultRepairStrategy strategy = new DefaultRepairStrategy(sources);
        if (repairStrategy.toLowerCase().equals("TryCatch".toLowerCase())) {
            strategy = new TryCatchRepairStrategy(sources);
        }

        //npefix.getSpoon().getEnvironment().setAutoImports(false);

        // NPEfix is based on metaprogramming, this is where it happens
        try {

            Launcher  npefix = new Launcher(sources, outputDirectory.getAbsolutePath() + "/npefix-output", binFolder.getAbsolutePath(), classpath(dependencies), complianceLevel, strategy);

            npefix.instrument();

            List<String> tests = new ArrayList<>();
            for (Pair<String, Set<File>> npeTest : npeTests) {
                if (!tests.contains(npeTest.getKey())) {
                    tests.add(npeTest.getKey());
                }
            }

            // getting the patch if any (an object of type NPEOutput), see method run below
            this.result = run(npefix, tests);

            System.out.println("========================RESULT=======================\n" + result);

            spoon.Launcher spoon = new spoon.Launcher();
            for (File s : sourceFolders) {
                spoon.addInputResource(s.getAbsolutePath());
            }

            spoon.getModelBuilder().setSourceClasspath(classpath(dependencies).split(File.pathSeparatorChar + ""));
            spoon.buildModel();

            // write a JSON file with the result and the patch
            JSONObject jsonObject = result.toJSON(spoon);
            System.out.println("JSON OBJECT: \n" + jsonObject);
            jsonObject.put("endInit", initDate.getTime());
            System.out.println(resultDirectory.getAbsolutePath());
            System.out.println(jsonObject.getJSONArray("executions"));
            for(Object ob : jsonObject.getJSONArray("executions"))
            {
                // the patch in the json file
                System.out.println(((JSONObject)ob).getString("diff"));
            }

            for (Decision decision : CallChecker.strategySelector.getSearchSpace()) {
                jsonObject.append("searchSpace", decision.toJSON());
            }
            FileWriter writer = new FileWriter(resultDirectory.getAbsolutePath() + "/patches_" + new Date().getTime() + ".json");
            jsonObject.write(writer);
            writer.close();

        }
        catch (Exception e) {
            System.out.println(e.toString() +
                    "Since we have surefire plugin configure to <testFailureIgnore>true</testFailureIgnore>, " +
                    "to automatically trigger NPEFix" +
                    "We don't break the build for errors during the build, continue.");
        }
        if (npeTests.contains(getFailingTests())) {
            System.out.println("Since we have surefire plugin configure to <testFailureIgnore>true</testFailureIgnore>, " +
                    "to automatically trigger NPEFix" +
                    "We want to fail the build if there is one test failure.");
            throw new RuntimeException("There was one test failure");
        }
        */

    }

    private NPEOutput run(Launcher npefix, List<String> npeTests) {
        switch (selector.toLowerCase()) {
            case "dom":
                return npefix.runStrategy(npeTests,
                        new NoStrat(),
                        new Strat1A(),
                        new Strat1B(),
                        new Strat2A(),
                        new Strat2B(),
                        new Strat3(),
                        new Strat4(ReturnType.NULL),
                        new Strat4(ReturnType.VAR),
                        new Strat4(ReturnType.NEW),
                        new Strat4(ReturnType.VOID));
            case "exploration":
                ExplorerSelector selector = new ExplorerSelector();
                if (repairStrategy.toLowerCase().equals("TryCatch".toLowerCase())) {
                    selector = new ExplorerSelector(new Strat4(ReturnType.NULL), new Strat4(ReturnType.VAR), new Strat4(ReturnType.NEW), new Strat4(ReturnType.VOID));
                }
                return multipleRuns(npefix, npeTests, selector);
            case "mono":
                Config.CONFIG.setMultiPoints(false);
                return multipleRuns(npefix, npeTests, new MonoExplorerSelector());
            case "greedy":
                return multipleRuns(npefix, npeTests, new GreedySelector());
            case "random":
                return multipleRuns(npefix, npeTests, new RandomSelector());
        }
        return null;
    }

    private NPEOutput multipleRuns(Launcher npefix, List<String> npeTests, Selector selector) {
        DecisionServer decisionServer = new DecisionServer(selector);
        decisionServer.startServer();

        NPEOutput output = new NPEOutput();

        int countError = 0;
        while (output.size() < nbIteration) {
            if (countError > 5) {
                break;
            }
            try {
                List<Lapse> result = npefix.run(selector, npeTests);
                if (result.isEmpty()) {
                    countError++;
                    continue;
                }
                boolean isEnd = true;
                for (int i = 0; i < result.size() && isEnd; i++) {
                    Lapse lapse = result.get(i);
                    if (lapse.getOracle().getError() != null) {
                        isEnd = isEnd && lapse.getOracle().getError().contains(NoMoreDecision.class.getSimpleName()) || lapse.getDecisions().isEmpty();
                    } else {
                        isEnd = false;
                    }
                }
                if (isEnd) {
                    // no more decision
                    countError++;
                    continue;
                }
                countError = 0;
                if (output.size() + result.size() > nbIteration) {
                    output.addAll(result.subList(0, (nbIteration - output.size())));
                } else {
                    output.addAll(result);
                }
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                countError++;
                continue;
            } catch (Exception e) {
                if (e.getCause() instanceof OutOfMemoryError) {
                    countError++;
                    continue;
                }
                e.printStackTrace();
                countError++;
                continue;
            }
            System.out.println("Multirun " + output.size() + "/" + nbIteration + " " + ((int) (output.size() / (double) nbIteration * 100)) + "%");
        }
        output.setEnd(new Date());
        return output;
    }

    public static String getNpeFixVersion() {
        try {
            final java.util.Properties properties = new java.util.Properties();
            properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("versions.properties"));
            return (properties.getProperty("npefix.version"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String classpath(List<URL> dependencies) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dependencies.size(); i++) {
            URL s = dependencies.get(i);
            sb.append(s.getPath()).append(File.pathSeparatorChar);
        }
        final Artifact artifact = artifactFactory.createArtifact("fr.inria.gforge.spirals", "npefix", getNpeFixVersion(), null, "jar");
        File file = new File(localRepository.getBasedir() + "/" + localRepository.pathOf(artifact));

        sb.append(file.getAbsoluteFile());
        System.out.println(sb);
        return sb.toString();
    }

    private File getSurefireReportsDirectory(MavenProject subProject) {
        String buildDir = subProject.getBuild().getDirectory();
        return new File(buildDir + "/surefire-reports");
    }

    /**
     * gets the failing tests due to a NullPointerException by parsing the Surefire XML files
     */
    private List<Pair<String, Set<File>>> getNPETest() {
        List<Pair<String, Set<File>>> output = new ArrayList<>();

        for (MavenProject mavenProject : reactorProjects) {
            File surefireReportsDirectory = getSurefireReportsDirectory(mavenProject);
            SurefireReportParser parser = new SurefireReportParser(Collections.singletonList(surefireReportsDirectory), Locale.ENGLISH, new NullConsoleLogger());
            try {
                List<ReportTestSuite> testSuites = parser.parseXMLReportFiles();
                for (int i = 0; i < testSuites.size(); i++) {
                    ReportTestSuite reportTestSuite = testSuites.get(i);
                    List<ReportTestCase> testCases = reportTestSuite.getTestCases();
                    for (int j = 0; j < testCases.size(); j++) {
                        ReportTestCase reportTestCase = testCases.get(j);
                        if (reportTestCase.hasFailure() && reportTestCase.getFailureDetail() != null) {
                            try {
                                StackTrace stackTrace = StackTraceParser.parse(reportTestCase.getFailureDetail());
                                StackTrace causedBy = stackTrace.getCausedBy();
                                while (causedBy != null) {
                                    stackTrace = causedBy;
                                    causedBy = stackTrace.getCausedBy();
                                }
                                if (stackTrace.getExceptionType().contains("NullPointerException") || repairStrategy.toLowerCase().equals("TryCatch".toLowerCase())) {
                                    Set<File> files = new HashSet<>();
                                    for (StackTraceElement stackTraceElement : stackTrace.getElements()) {
                                        System.out.println("METHOD: " + stackTraceElement.getMethod());
                                        System.out.println("METHOD substring: " + stackTraceElement.getMethod().substring(0, stackTraceElement.getMethod().lastIndexOf(".")));
                                        String path = stackTraceElement.getMethod().substring(0, stackTraceElement.getMethod().lastIndexOf(".")).replace(".", "/") + ".java";
                                        System.out.println("PATH : " + path);
                                        if (path.contains("$")) {
                                            path = path.substring(0, path.indexOf("$")) + ".java";
                                        }
                                        if ("package".equals(scope)) {
                                            path = path.substring(0, path.lastIndexOf("/"));
                                        }
                                        for (MavenProject project : reactorProjects) {
                                            System.out.println("SOURCE DIRECTORY: " + project.getBuild().getSourceDirectory());
                                            File file = new File(project.getBuild().getSourceDirectory() + "/" + path);
                                            if (file.exists()) {
                                                files.add(file);
                                                break;
                                            }
                                        }
                                        if (!"stack".equals(scope)) {
                                            break;
                                        }
                                    }
                                    System.out.println("KEY: " + reportTestCase.getFullClassName() + "#" + reportTestCase.getName());
                                    System.out.println("VALUE: " + files);
                                    output.add(new Pair<>(reportTestCase.getFullClassName() + "#" + reportTestCase.getName(), files));
                                }
                            } catch (StackTraceParser.ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } catch (MavenReportException e) {
                e.printStackTrace();
            }
        }

        return output;
    }

    public File getResultDirectory() {
        return resultDirectory;
    }
}
