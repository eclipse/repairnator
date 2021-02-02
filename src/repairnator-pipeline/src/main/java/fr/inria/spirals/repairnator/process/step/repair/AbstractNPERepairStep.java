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
import fr.inria.spirals.repairnator.process.testinformation.FailureLocation;
import fr.inria.spirals.repairnator.process.testinformation.FailureType;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Created by andre15silva on 17/01/2021
 */
public abstract class AbstractNPERepairStep extends AbstractRepairStep {

    private String selection = "exploration";
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
        List<String> npeTests = getNPETests();

        if (!npeTests.isEmpty()) {
            this.getLogger().info("NPE found, start NPEFix");

            try {
                File rootDir = new File(this.getInspector().getRepoLocalPath()).getCanonicalFile();
                Config.CONFIG.setRootProject(rootDir.toPath().toAbsolutePath());
            } catch (IOException e) {
                this.getLogger().error("Error while setting the root project path, the created patches might have absolute paths.");
            }

            final List<URL> dependencies = this.getInspector().getJobStatus().getRepairClassPath();
            final File[] sourceFolders = this.getInspector().getJobStatus().getRepairSourceDir();
            File binFolder = new File(this.getInspector().getWorkspace() + "/npefix-bin");
            File npeFixJar = new File(this.getConfig().getLocalMavenRepository() + "/fr/inria/gforge/spirals/npefix/0.7/npefix-0.7.jar");

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

            String[] sources = Arrays.stream(sourceFolders)
                    .map(File::getAbsolutePath).toArray(String[]::new);
            DefaultRepairStrategy strategy = new DefaultRepairStrategy(sources);
            if (repairStrategy.equalsIgnoreCase("TryCatch")) {
                strategy = new TryCatchRepairStrategy(sources);
            }

            this.getLogger().debug(String.format("Classpath %s", classpath(dependencies)));

            Launcher npefix = new Launcher(sources, this.getConfig().getOutputPath() + "/npefix-output", binFolder.getAbsolutePath(), classpath(dependencies), complianceLevel, strategy);

            npefix.instrument();

            NPEOutput result = run(npefix, npeTests);

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
                            String content = diff.getAsString();

                            RepairPatch repairPatch = new RepairPatch(this.getRepairToolName(), "", content);
                            repairPatches.add(repairPatch);
                        } else {
                            this.addStepError("Error while parsing JSON path file: diff content is null.");
                        }
                    }
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

    private List<String> getNPETests() {
        this.getLogger().debug("Parsing test information for NPE...");
        List<String> npeTests = new ArrayList<>();

        // Iterate over failure locations
        for (FailureLocation failureLocation : this.getInspector().getJobStatus().getFailureLocations()) {
            // NPEEs are errors
            Map<String, List<FailureType>> failingMethodsAndFailures = failureLocation.getErroringMethodsAndFailures();

            // Check if test method has an NPE
            for (String method : failingMethodsAndFailures.keySet()) {
                if (failingMethodsAndFailures.get(method).stream()
                        .anyMatch((FailureType ft) -> ft.getFailureName().startsWith("java.lang.NullPointerException"))) {
                    String test = failureLocation.getClassName() + '#' + method;
                    this.getLogger().debug(String.format("Found NPE in %s", test));
                    npeTests.add(test);
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