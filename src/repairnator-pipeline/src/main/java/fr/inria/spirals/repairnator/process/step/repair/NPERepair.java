package fr.inria.spirals.repairnator.process.step.repair;

import fr.inria.spirals.npefix.config.Config;
import fr.inria.spirals.npefix.main.DecisionServer;
import fr.inria.spirals.npefix.main.all.DefaultRepairStrategy;
import fr.inria.spirals.npefix.main.all.Launcher;
import fr.inria.spirals.npefix.resi.CallChecker;
import fr.inria.spirals.npefix.resi.context.Decision;
import fr.inria.spirals.npefix.resi.context.Lapse;
import fr.inria.spirals.npefix.resi.context.NPEOutput;
import fr.inria.spirals.npefix.resi.exception.NoMoreDecision;
import fr.inria.spirals.npefix.resi.selector.*;
import fr.inria.spirals.npefix.resi.strategies.*;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;
import fr.inria.spirals.repairnator.process.testinformation.ErrorType;
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
 * Created by urli on 10/07/2017.
 */
public class NPERepair extends AbstractRepairStep {
    public static final String TOOL_NAME = "NPEFix";
    private static final String NPEFIX_GOAL = "fr.inria.gforge.spirals:repair-maven-plugin:1.5:npefix";

    public NPERepair() {}

    @Override
    public String getRepairToolName() {
        return TOOL_NAME;
    }

    private boolean isThereNPE() {
        for (FailureLocation failureLocation : this.getInspector().getJobStatus().getFailureLocations()) {
            Map<String, List<ErrorType>> errorsMap = failureLocation.getErroringMethodsAndErrors();
            for (String method : errorsMap.keySet()) {
                if (errorsMap.get(method).stream()
                        .anyMatch((ErrorType et) -> et.getName().startsWith("java.lang.NullPointerException"))) {
                    return true;
                }
            }
        }
        return false;
    }
	private List<String> getNPETests() {
		this.getLogger().debug("Parsing test information for NPE...");
		List<String> npeTests = new ArrayList<>();

		// Iterate over failure locations
		for (FailureLocation failureLocation : this.getInspector().getJobStatus().getFailureLocations()) {
			// NPEEs are errors
			Map<String, List<FailureType>> failingMethodsAndFailures = failureLocation.getFailingMethodsAndFailures();

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
			File binFolder = new File(this.getConfig().getOutputPath() + "/npefix-bin");
			System.out.println(binFolder);

			// setting up dependencies
			try {
				dependencies.add(binFolder.toURI().toURL());
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
			/*
			if (repairStrategy.toLowerCase().equals("TryCatch".toLowerCase())) {
				strategy = new TryCatchRepairStrategy(sources);
			}
			 */

			System.out.println(classpath(dependencies));

			Launcher npefix = new Launcher(sources, this.getConfig().getOutputPath() + "/npefix-output", binFolder.getAbsolutePath(), classpath(dependencies), complianceLevel, strategy);

			npefix.instrument();

			NPEOutput result = run(npefix, npeTests);

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
			for (Object ob : jsonObject.getJSONArray("executions")) {
				// the patch in the json file
				System.out.println(((JSONObject) ob).getString("diff"));
			}

			try {
				for (Decision decision : CallChecker.strategySelector.getSearchSpace()) {
					jsonObject.append("searchSpace", decision.toJSON());
				}
				FileWriter writer = new FileWriter(this.getConfig().getOutputPath() + "/patches_" + new Date().getTime() + ".json");
				jsonObject.write(writer);
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			return StepStatus.buildSkipped(this);

            /*
            List<RepairPatch> repairPatches = new ArrayList<>();

            MavenHelper mavenHelper = new MavenHelper(this.getPom(), NPEFIX_GOAL, null, this.getName(), this.getInspector(), true );
            int status = MavenHelper.MAVEN_ERROR;
            try {
                status = mavenHelper.run();
            } catch (InterruptedException e) {
                this.addStepError("Error while executing Maven goal", e);
            }

            if (status == MavenHelper.MAVEN_ERROR) {
                this.addStepError("Error while running NPE fix: maybe the project does not contain a NPE?");
                return StepStatus.buildSkipped(this,"Error while running maven goal for NPEFix.");
            } else {
                System.out.println(this.getInspector().getJobStatus().getPomDirPath()+"/target/npefix");
                Collection<File> files = FileUtils.listFiles(new File(this.getInspector().getJobStatus().getPomDirPath()+"/target/npefix"), new String[] { "json"}, false);
                if (!files.isEmpty()) {

                    File patchesFiles = files.iterator().next();
                    try {
                        FileUtils.copyFile(patchesFiles, new File(this.getInspector().getRepoLocalPath()+"/repairnator.npefix.results"));
                    } catch (IOException e) {
                        this.addStepError("Error while moving NPE fix results", e);
                    }

                    this.getInspector().getJobStatus().addFileToPush("repairnator.npefix.results");

                    boolean effectivelyPatched = false;
                    File patchDir = new File(this.getInspector().getRepoLocalPath()+"/repairnatorPatches");

                    patchDir.mkdir();
                    try {
                        JsonParser jsonParser = new JsonParser();
                        JsonElement root = jsonParser.parse(new FileReader(patchesFiles));
                        this.recordToolDiagnostic(root);

                        JsonArray executions = root.getAsJsonObject().getAsJsonArray("executions");
                        if (executions != null) {
                            for (JsonElement execution : executions) {
                                JsonObject result = execution.getAsJsonObject().getAsJsonObject("result");
                                boolean success = result.get("success").getAsBoolean() && execution.getAsJsonObject().has("decisions");

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

                            this.recordPatches(repairPatches,MAX_PATCH_PER_TOOL);
                        }
                    } catch (IOException e) {
                        this.addStepError("Error while parsing JSON patch files");
                    }

                    if (effectivelyPatched) {
                    	return StepStatus.buildSuccess(this);
                    } else {
                    	return StepStatus.buildPatchNotFound(this);
                    }

                } else {
                    this.addStepError("Error while getting the patch json file: no file found.");
                    return StepStatus.buildSkipped(this,"Error while reading patch file.");
                }


            }

             */
		} else {
			this.getLogger().info("No NPE found, this step will be skipped.");
			return StepStatus.buildSkipped(this, "No NPE found.");
		}
	}

	private NPEOutput run(Launcher npefix, List<String> npeTests) {
		switch ("exploration".toLowerCase()) { // TODO: make this a parameter
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
				if ("default".toLowerCase().equals("TryCatch".toLowerCase())) { // TODO: make this a parameter
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
		int nbIteration = 100; // TODO: Remove this
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

	private String classpath(List<URL> dependencies) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < dependencies.size(); i++) {
			URL s = dependencies.get(i);
			sb.append(s.getPath()).append(File.pathSeparatorChar);
		}

		sb.append(this.getConfig().getMavenHome() + "/fr/inria/gforge/spirals/npefix/" + 0.7 + "/npefix-" + 0.7 + ".jar");
		System.out.println(sb);
		return sb.toString();
	}
}
