package fr.inria.spirals.repairnator.process.step.repair;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.testinformation.ErrorType;
import fr.inria.spirals.repairnator.process.testinformation.FailureLocation;
import fr.inria.spirals.repairnator.process.testinformation.FailureType;
import fr.inria.spirals.repairnator.utils.Pair;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by andre15silva on 17/01/2021
 */
public abstract class AbstractNPERepairStep extends AbstractRepairStep {

    protected String selection = "exploration";
    private int nbIteration = 100;
    private String scope = "class";
    private String repairStrategy = "default";

    public AbstractNPERepairStep() {
        this.selection = this.getConfig().getNPESelection() != null ? this.getConfig().getNPESelection() : selection;
        this.nbIteration = this.getConfig().getNPENbIteration() != null ? this.getConfig().getNPENbIteration() : nbIteration;
        this.scope = this.getConfig().getNPEScope() != null ? this.getConfig().getNPEScope() : scope;
        this.repairStrategy = this.getConfig().getNPERepairStrategy() != null ? this.getConfig().getNPERepairStrategy() : repairStrategy;
    }

    @Override
    protected StepStatus businessExecute() {
        this.getLogger().debug("Entrance in NPERepair step...");

        // Get the failing NPE tests
        List<Pair<String, List<ErrorType>>> npeTests = getNPETests();

        if (!npeTests.isEmpty()) {
            this.getLogger().info("NPE found, start NPEFix");

            try {
                File rootDir = new File(this.getInspector().getRepoLocalPath()).getCanonicalFile();
                Config.CONFIG.setRootProject(rootDir.toPath().toAbsolutePath());
            } catch (IOException e) {
                this.getLogger().error("Error while setting the root project path, the created patches might have absolute paths.");
            }

            final List<URL> dependencies = this.getInspector().getJobStatus().getRepairClassPath();
            File binFolder = new File(this.getInspector().getWorkspace() + "/npefix-bin");
            File npeFixJar = new File(this.getConfig().getLocalMavenRepository() + "/fr/inria/gforge/spirals/npefix/0.7/npefix-0.7.jar");

            List<File> sourceFolders = new ArrayList<>();
            if (scope.equals("project")) {
                sourceFolders = Arrays.asList(this.getInspector().getJobStatus().getRepairSourceDir());
            } else if (scope.equals("class")) {
                sourceFolders = npeTests.stream().map(Pair::getValue).flatMap(List::stream)
                        .map(ErrorType::getClassFiles).flatMap(Set::stream).collect(Collectors.toList());
            } else if (scope.equals("package")) {
                sourceFolders = npeTests.stream().map(Pair::getValue).flatMap(List::stream)
                        .map(ErrorType::getPackageFiles).flatMap(Set::stream).collect(Collectors.toList());
            } else if (scope.equals("stack")) {
                sourceFolders = npeTests.stream().map(Pair::getValue).flatMap(List::stream)
                        .map(ErrorType::getStackFiles).flatMap(Set::stream).collect(Collectors.toList());
            }
            this.getLogger().info("Using scope " + scope + ", " + sourceFolders.size() + " files/dirs will be used");

            // setting up dependencies
            try {
                dependencies.add(binFolder.toURI().toURL());
                dependencies.add(npeFixJar.toURI().toURL());
            } catch (MalformedURLException e) {
                this.getLogger().error(e.getMessage());
            }
            if (!binFolder.exists()) {
                binFolder.mkdirs();
            }
            int complianceLevel = 7;

            String[] sources = sourceFolders.stream()
                    .map(File::getAbsolutePath).peek(System.out::println).toArray(String[]::new);
            DefaultRepairStrategy strategy = new DefaultRepairStrategy(sources);
            if (repairStrategy.equalsIgnoreCase("TryCatch")) {
                strategy = new TryCatchRepairStrategy(sources);
            }

            this.getLogger().debug(String.format("Classpath %s", classpath(dependencies)));

            Launcher npefix = new Launcher(sources, this.getConfig().getOutputPath() + "/npefix-output", binFolder.getAbsolutePath(), classpath(dependencies), complianceLevel, strategy);

            npefix.instrument();

            NPEOutput result = run(npefix, npeTests.stream().map((Pair::getKey)).collect(Collectors.toList()));

            spoon.Launcher spoon = new spoon.Launcher();
            for (File s : sourceFolders) {
                spoon.addInputResource(s.getAbsolutePath());
            }

            spoon.getModelBuilder().setSourceClasspath(classpath(dependencies).split(File.pathSeparatorChar + ""));
            spoon.buildModel();

            // write a JSON file with the result and the patch
            JSONObject jsonObject = result.toJSON(spoon);
            for (Object ob : jsonObject.getJSONArray("executions")) {
                // the patch in the json file
                this.getLogger().info(((JSONObject) ob).getString("diff"));
            }

            try {
                for (Decision decision : CallChecker.strategySelector.getSearchSpace()) {
                    jsonObject.append("searchSpace", decision.toJSON());
                }
                FileWriter writer = new FileWriter(this.getConfig().getOutputPath() + "/patches_" + new Date().getTime() + ".json");
                jsonObject.write(writer);
                writer.close();
            } catch (IOException e) {
                this.getLogger().error(e.getMessage());
            }

            List<RepairPatch> repairPatches = new ArrayList<>();
            boolean effectivelyPatched = false;
            File patchDir = new File(this.getInspector().getRepoLocalPath() + "/repairnatorPatches");
            patchDir.mkdir();

            Gson gson = new Gson();
            JsonElement root = gson.fromJson(jsonObject.toString(), JsonElement.class);
            this.recordToolDiagnostic(root);

            JsonArray executions = root.getAsJsonObject().getAsJsonArray("executions");
            if (executions != null) {
                for (JsonElement execution : executions) {
                    JsonObject jsonResult = execution.getAsJsonObject().getAsJsonObject("result");
                    boolean success = jsonResult.get("success").getAsBoolean() && execution.getAsJsonObject().has("decisions");

                    if (success) {
                        effectivelyPatched = true;

                        JsonElement diff = execution.getAsJsonObject().get("diff");
                        if (diff != null) {
                            String path = execution.getAsJsonObject().getAsJsonArray("decisions")
                                    .get(0).getAsJsonObject().getAsJsonObject("location").get("class")
                                    .getAsString().replace(".","/") + ".java";
                            for (File dir : this.getInspector().getJobStatus().getRepairSourceDir()) {
                                String tmpPath = dir.getAbsolutePath() + "/" + path;
                                if (new File(tmpPath).exists()) {
                                    path = tmpPath;
                                    break;
                                }
                            }

                            String content = diff.getAsString();

                            RepairPatch repairPatch = new RepairPatch(this.getRepairToolName(), path, content);
                            repairPatches.add(repairPatch);
                        } else {
                            this.addStepError("Error while parsing JSON path file: diff content is null.");
                        }
                    }
                }

                repairPatches = this.performPatchAnalysis(repairPatches);
                if (repairPatches.isEmpty()) {
                    return StepStatus.buildPatchNotFound(this);
                }
                this.recordPatches(repairPatches, MAX_PATCH_PER_TOOL);
            }

            if (effectivelyPatched) {
                return StepStatus.buildSuccess(this);
            } else {
                return StepStatus.buildPatchNotFound(this);
            }
        } else {
            this.getLogger().info("No NPE found, this step will be skipped.");
            return StepStatus.buildSkipped(this, "No NPE found.");
        }
    }

    private List<Pair<String, List<ErrorType>>> getNPETests() {
        this.getLogger().debug("Parsing test information for NPE...");
        List<Pair<String, List<ErrorType>>> npeTests = new ArrayList<>();

        // Iterate over failure locations
        for (FailureLocation failureLocation : this.getInspector().getJobStatus().getFailureLocations()) {
            // NPEEs are errors
            Map<String, List<ErrorType>> erroringMethodsAndErrors = failureLocation.getErroringMethodsAndErrors();

            // Check if test method has an NPE
            for (String method : erroringMethodsAndErrors.keySet()) {
                if (erroringMethodsAndErrors.get(method).stream()
                        .anyMatch((ErrorType et) -> et.getName().startsWith("java.lang.NullPointerException"))) {
                    String test = failureLocation.getClassName() + '#' + method;
                    this.getLogger().debug(String.format("Found NPE in %s", test));
                    List<ErrorType> errorTypes = erroringMethodsAndErrors.get(method);
                    npeTests.add(new Pair<>(test, errorTypes));
                }
            }
        }

        return npeTests;
    }

    private NPEOutput run(Launcher npefix, List<String> npeTests) {
        switch (selection.toLowerCase()) {
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
                if (repairStrategy.equalsIgnoreCase("TryCatch")) {
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
            case "safe-mono":
                return multipleRuns(npefix, npeTests, new SafeMonoSelector());
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
                this.getLogger().error(e.getMessage());
                countError++;
                continue;
            } catch (Exception e) {
                if (e.getCause() instanceof OutOfMemoryError) {
                    countError++;
                    continue;
                }
                this.getLogger().error(e.getMessage());
                countError++;
                continue;
            }
            this.getLogger().info("Multirun " + output.size() + "/" + nbIteration + " " + ((int) (output.size() / (double) nbIteration * 100)) + "%");
        }
        output.setEnd(new Date());
        return output;
    }

    private String classpath(List<URL> dependencies) {
        StringBuilder sb = new StringBuilder();
        for (URL s : dependencies) {
            sb.append(s.getPath()).append(File.pathSeparatorChar);
        }
        return sb.toString();
    }

}