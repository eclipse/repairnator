[33mcommit 57ba9a070f7b31ff630b2f6ee9b06b3427559a1c[m[33m ([m[1;36mHEAD -> [m[1;32mfile-helper[m[33m)[m
Merge: 2e3db0e e0b976f
Author: btellstrom <benjamin.tellstrom@gmail.com>
Date:   Thu Jul 12 11:05:15 2018 +0200

    merged master into file-helper for merge in github

[1mdiff --cc repairnator/repairnator-pipeline/src/main/java/fr/inria/spirals/repairnator/process/step/repair/NopolRepair.java.orig[m
[1mindex 0000000,0000000..3a8a065[m
[1mnew file mode 100644[m
[1m--- /dev/null[m
[1m+++ b/repairnator/repairnator-pipeline/src/main/java/fr/inria/spirals/repairnator/process/step/repair/NopolRepair.java.orig[m
[36m@@@ -1,0 -1,0 +1,256 @@@[m
[32m++package fr.inria.spirals.repairnator.process.step.repair;[m
[32m++[m
[32m++import com.google.gson.Gson;[m
[32m++import com.google.gson.JsonArray;[m
[32m++import fr.inria.lille.commons.synthesis.smt.solver.SolverFactory;[m
[32m++import fr.inria.lille.repair.common.config.NopolContext;[m
[32m++import fr.inria.lille.repair.common.patch.Patch;[m
[32m++import fr.inria.lille.repair.common.synth.RepairType;[m
[32m++import fr.inria.lille.repair.nopol.NoPol;[m
[32m++import fr.inria.lille.repair.nopol.NopolResult;[m
[32m++import fr.inria.spirals.repairnator.process.files.FileHelper;[m
[32m++import fr.inria.spirals.repairnator.process.inspectors.JobStatus;[m
[32m++import fr.inria.spirals.repairnator.process.inspectors.Metrics;[m
[32m++import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;[m
[32m++import fr.inria.spirals.repairnator.process.inspectors.StepStatus;[m
[32m++import fr.inria.spirals.repairnator.process.nopol.IgnoreStatus;[m
[32m++import fr.inria.spirals.repairnator.process.nopol.NopolInformation;[m
[32m++import fr.inria.spirals.repairnator.process.nopol.NopolStatus;[m
[32m++import fr.inria.spirals.repairnator.process.testinformation.ComparatorFailureLocation;[m
[32m++import fr.inria.spirals.repairnator.process.testinformation.FailureLocation;[m
[32m++import spoon.SpoonException;[m
[32m++import spoon.reflect.factory.Factory;[m
[32m++[m
[32m++import java.io.File;[m
[32m++import java.io.IOException;[m
[32m++import java.net.URL;[m
[32m++import java.nio.file.Files;[m
[32m++import java.util.*;[m
[32m++import java.util.concurrent.*;[m
[32m++[m
[32m++/**[m
[32m++ * Created by urli on 05/01/2017.[m
[32m++ */[m
[32m++public class NopolRepair extends AbstractRepairStep {[m
[32m++    protected static final String TOOL_NAME = "Nopol";[m
[32m++    protected static final String TOOL_RESULTS_FOLDER_NAME = "repairnator.nopol.results";[m
[32m++    public static int TOTAL_MAX_TIME = 60 * 4; // We expect it to run 4[m
[32m++                                                      // hours top.[m
[32m++    private static final int MIN_TIMEOUT = 2;[m
[32m++[m
[32m++    @Override[m
[32m++    public String getRepairToolName() {[m
[32m++        return TOOL_NAME;[m
[32m++    }[m
[32m++[m
[32m++    @Override[m
[32m++    protected StepStatus businessExecute() {[m
[32m++        this.getLogger().debug("Start to use nopol to repair...");[m
[32m++[m
[32m++        JobStatus jobStatus = this.getInspector().getJobStatus();[m
[32m++[m
[32m++        File patchDir = new File(this.getInspector().getRepoLocalPath()+"/"+TOOL_RESULTS_FOLDER_NAME);[m
[32m++        patchDir.mkdir();[m
[32m++[m
[32m++        JsonArray toolDiag = new JsonArray();[m
[32m++        List<RepairPatch> repairPatches = new ArrayList<>();[m
[32m++        Gson gson = new Gson();[m
[32m++        Metrics metric = jobStatus.getMetrics();[m
[32m++        List<URL> classPath = jobStatus.getRepairClassPath();[m
[32m++        File[] sources = jobStatus.getRepairSourceDir();[m
[32m++[m
[32m++        if (classPath != null && sources != null) {[m
[32m++            String[] sourcesStr = new String[sources.length];[m
[32m++[m
[32m++            int i = 0;[m
[32m++            for (File f : sources) {[m
[32m++                sourcesStr[i++] = f.getAbsolutePath();[m
[32m++            }[m
[32m++[m
[32m++            List<FailureLocation> failureLocationList = new ArrayList<>(jobStatus.getFailureLocations());[m
[32m++            Collections.sort(failureLocationList, new ComparatorFailureLocation());[m
[32m++[m
[32m++            boolean patchCreated = false;[m
[32m++            int passingTime = 0;[m
[32m++[m
[32m++            for (FailureLocation failureLocation : failureLocationList) {[m
[32m++                Set<String> erroringTests = failureLocation.getErroringMethods();[m
[32m++                Set<String> failingTests = failureLocation.getFailingMethods();[m
[32m++[m
[32m++                // this one is used to loop on Nopol over tests to ignore. It can be a list containing an empty list.[m
[32m++                List<List<String>> listOfTestToIgnore = new ArrayList<>();[m
[32m++[m
[32m++                boolean ignoreError = false;[m
[32m++                // in that case: no tests to ignore[m
[32m++                if (erroringTests.isEmpty() || failingTests.isEmpty()) {[m
[32m++                    listOfTestToIgnore.add(new ArrayList<>());[m
[32m++                // then we will first try to ignore erroring tests, then to ignore failing tests[m
[32m++                } else {[m
[32m++                    listOfTestToIgnore.add(new ArrayList<>(erroringTests));[m
[32m++                    listOfTestToIgnore.add(new ArrayList<>(failingTests));[m
[32m++[m
[32m++                    ignoreError = true;[m
[32m++                }[m
[32m++[m
[32m++                for (List<String> testsToIgnore : listOfTestToIgnore) {[m
[32m++                    NopolInformation nopolInformation;[m
[32m++                    if (testsToIgnore.isEmpty()) {[m
[32m++                        nopolInformation = new NopolInformation(failureLocation, IgnoreStatus.NOTHING_TO_IGNORE);[m
[32m++                    } else {[m
[32m++                        if (ignoreError) {[m
[32m++                            nopolInformation = new NopolInformation(failureLocation, IgnoreStatus.IGNORE_ERRORING);[m
[32m++                            ignoreError = false;[m
[32m++                        } else {[m
[32m++                            nopolInformation = new NopolInformation(failureLocation, IgnoreStatus.IGNORE_FAILING);[m
[32m++                        }[m
[32m++                    }[m
[32m++[m
[32m++                    nopolInformation.setStatus(NopolStatus.RUNNING);[m
[32m++[m
[32m++                    String testClass = failureLocation.getClassName();[m
[32m++                    int timeout = (TOTAL_MAX_TIME - passingTime) / 2;[m
[32m++                    if (timeout < MIN_TIMEOUT) {[m
[32m++                        timeout = MIN_TIMEOUT;[m
[32m++                    }[m
[32m++[m
[32m++                    nopolInformation.setAllocatedTime(timeout);[m
[32m++[m
[32m++                    this.getLogger().debug("Launching repair with Nopol for following test class: " + testClass[m
[32m++                            + " (should timeout in " + timeout + " minutes)");[m
[32m++[m
[32m++                    NopolContext nopolContext = new NopolContext(sources, classPath.toArray(new URL[classPath.size()]), new String[] { testClass }, testsToIgnore);[m
[32m++                    nopolContext.setComplianceLevel(8);[m
[32m++                    nopolContext.setTimeoutTestExecution(300);[m
[32m++                    nopolContext.setMaxTimeEachTypeOfFixInMinutes(15);[m
[32m++                    nopolContext.setMaxTimeInMinutes(timeout);[m
[32m++                    nopolContext.setLocalizer(NopolContext.NopolLocalizer.COCOSPOON);[m
[32m++                    nopolContext.setSolverPath(this.getConfig().getZ3solverPath());[m
[32m++                    nopolContext.setSynthesis(NopolContext.NopolSynthesis.DYNAMOTH);[m
[32m++                    nopolContext.setType(RepairType.COND_THEN_PRE);[m
[32m++                    nopolContext.setOnlyOneSynthesisResult(false);[m
[32m++                    nopolContext.setOutputFolder(patchDir.getAbsolutePath());[m
[32m++[m
[32m++                    nopolInformation.setNopolContext(nopolContext);[m
[32m++[m
[32m++                    SolverFactory.setSolver(nopolContext.getSolver(), nopolContext.getSolverPath());[m
[32m++[m
[32m++                    long beforeNopol = new Date().getTime();[m
[32m++[m
[32m++                    try {[m
[32m++                        final NoPol nopol = new NoPol(nopolContext);[m
[32m++                        Factory spoonFactory = nopol.getSpooner().spoonFactory();[m
[32m++[m
[32m++                        final ExecutorService executor = Executors.newSingleThreadExecutor();[m
[32m++                        final Future<NopolResult> nopolExecution = executor.submit(new Callable<NopolResult>() {[m
[32m++                            @Override[m
[32m++                            public NopolResult call() throws Exception {[m
[32m++                                NopolResult result = null;[m
[32m++                                try {[m
[32m++                                    result = nopol.build();[m
[32m++                                } catch (RuntimeException e) {[m
[32m++                                    addStepError("Got runtime exception while running Nopol", e);[m
[32m++                                }[m
[32m++                                return result;[m
[32m++                            }[m
[32m++                        });[m
[32m++[m
[32m++                        try {[m
[32m++                            executor.shutdown();[m
[32m++                            NopolResult result = nopolExecution.get(nopolContext.getMaxTimeInMinutes(), TimeUnit.MINUTES);[m
[32m++[m
[32m++                            if (result == null) {[m
[32m++                                result = nopol.getNopolResult();[m
[32m++                            }[m
[32m++                            nopolInformation.setNbStatements(result.getNbStatements());[m
[32m++                            nopolInformation.setNbAngelicValues(result.getNbAngelicValues());[m
[32m++[m
[32m++                            String failureLocationWithoutDots = failureLocation.getClassName().replace('.','/');[m
[32m++[m
[32m++                            metric.addAngelicValueByTest(failureLocationWithoutDots, result.getNbAngelicValues());[m
[32m++[m
[32m++                            List<Patch> patches = result.getPatches();[m
[32m++                            if (patches != null && !patches.isEmpty()) {[m
[32m++                                for (Patch patch : patches) {[m
[32m++                                    String diff = patch.toDiff(spoonFactory, nopolContext);[m
[32m++[m
[32m++                                    RepairPatch repairPatch = new RepairPatch(this.getRepairToolName(), "", diff);[m
[32m++                                    repairPatches.add(repairPatch);[m
[32m++                                }[m
[32m++                                nopolInformation.setStatus(NopolStatus.PATCH);[m
[32m++                                patchCreated = true;[m
[32m++                            } else {[m
[32m++                                nopolInformation.setStatus(NopolStatus.NOPATCH);[m
[32m++                            }[m
[32m++                        } catch (TimeoutException exception) {[m
[32m++                            this.addStepError("Timeout: execution time > " + nopolContext.getMaxTimeInMinutes() + " " + TimeUnit.MINUTES);[m
[32m++                            nopolExecution.cancel(true);[m
[32m++                            executor.shutdownNow();[m
[32m++                            nopolInformation.setStatus(NopolStatus.TIMEOUT);[m
[32m++[m
[32m++                            NopolResult result = nopol.getNopolResult();[m
[32m++                            nopolInformation.setNbStatements(result.getNbStatements());[m
[32m++                            nopolInformation.setNbAngelicValues(result.getNbAngelicValues());[m
[32m++[m
[32m++                        } catch (InterruptedException | ExecutionException e) {[m
[32m++                            this.addStepError(e.getMessage());[m
[32m++                            nopolExecution.cancel(true);[m
[32m++                            executor.shutdownNow();[m
[32m++                            nopolInformation.setStatus(NopolStatus.EXCEPTION);[m
[32m++                            nopolInformation.setExceptionDetail(e.getMessage());[m
[32m++[m
[32m++                            NopolResult result = nopol.getNopolResult();[m
[32m++                            nopolInformation.setNbStatements(result.getNbStatements());[m
[32m++                            nopolInformation.setNbAngelicValues(result.getNbAngelicValues());[m
[32m++                        }[m
[32m++                    } catch (SpoonException e) {[m
[32m++                        this.addStepError(e.getMessage());[m
[32m++                        nopolInformation.setStatus(NopolStatus.EXCEPTION);[m
[32m++                        nopolInformation.setExceptionDetail(e.getMessage());[m
[32m++                    }[m
[32m++[m
[32m++[m
[32m++                    long afterNopol = new Date().getTime();[m
[32m++[m
[32m++                    int localPassingTime = Math.round((afterNopol - beforeNopol) / 60000);[m
[32m++                    nopolInformation.setPassingTime(localPassingTime);[m
[32m++[m
[32m++                    toolDiag.add(gson.toJsonTree(nopolInformation));[m
[32m++[m
[32m++                    passingTime += localPassingTime;[m
[32m++                }[m
[32m++            }[m
[32m++[m
[32m++            File nopolLog = new File(System.getProperty("user.dir"), "debug.log");[m
[32m++            if (nopolLog.exists()) {[m
[32m++                String nopolDestName = "repairnator.nopol.log";[m
[32m++                File nopolDest = new File(this.getInspector().getRepoLocalPath(), nopolDestName);[m
[32m++                try {[m
[32m++                    Files.move(nopolLog.toPath(), nopolDest.toPath());[m
[32m++                    jobStatus.addFileToPush(nopolDestName);[m
[32m++                } catch (IOException e) {[m
[32m++                    getLogger().error("Error while renaming nopol log", e);[m
[32m++                }[m
[32m++            }[m
[32m++[m
[32m++            this.recordPatches(repairPatches);[m
[32m++            this.recordToolDiagnostic(toolDiag);[m
[32m++[m
[32m++            try {[m
[32m++                FileHelper.deleteFile(patchDir);[m
[32m++            } catch (IOException e) {[m
[32m++                getLogger().error("Error while removing the temp folder containing Nopol output", e);[m
[32m++            }[m
[32m++[m
[32m++            if (!patchCreated) {[m
[32m++                this.addStepError("No patch has been generated by Nopol. Look at the trace to get more information.");[m
[32m++                return StepStatus.buildSkipped(this,"No patch has been found.");[m
[32m++            }[m
[32m++[m
[32m++            return StepStatus.buildSuccess(this);[m
[32m++        } else {[m
[32m++            this.addStepError("No classpath or sources directory has been given. Nopol can't be launched.");[m
[32m++            return StepStatus.buildSkipped(this,"No classpath or source directory given.");[m
[32m++        }[m
[32m++    }[m
[32m++[m
[32m++}[m
