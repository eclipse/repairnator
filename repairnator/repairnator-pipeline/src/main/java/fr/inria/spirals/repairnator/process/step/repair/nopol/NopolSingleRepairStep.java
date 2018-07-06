package fr.inria.spirals.repairnator.process.step.repair.nopol;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import fr.inria.lille.commons.synthesis.smt.solver.SolverFactory;
import fr.inria.lille.repair.common.config.NopolContext;
import fr.inria.lille.repair.common.patch.Patch;
import fr.inria.lille.repair.common.synth.RepairType;
import fr.inria.lille.repair.nopol.NoPol;
import fr.inria.lille.repair.nopol.NopolResult;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.Metrics;
import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;
import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
import fr.inria.spirals.repairnator.process.nopol.IgnoreStatus;
import fr.inria.spirals.repairnator.process.nopol.NopolInformation;
import fr.inria.spirals.repairnator.process.nopol.NopolStatus;
import fr.inria.spirals.repairnator.process.step.repair.AbstractRepairStep;
import fr.inria.spirals.repairnator.process.testinformation.ComparatorFailureLocation;
import fr.inria.spirals.repairnator.process.testinformation.FailureLocation;
import spoon.SpoonException;
import spoon.reflect.factory.Factory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static fr.inria.spirals.repairnator.process.git.GitHelper.deleteFile;

/**
 * This launch Nopol against the whole test suite
 */
public class NopolSingleRepairStep extends AbstractRepairStep {
    protected static final String TOOL_NAME = "Nopol";
    protected static final String TOOL_RESULTS_FOLDER_NAME = "repairnator.nopol.results";
    public static int TOTAL_MAX_TIME = 60 * 4; // We expect it to run 4
                                                      // hours top.
    private static final int MIN_TIMEOUT = 2;

    @Override
    public String getRepairToolName() {
        return TOOL_NAME;
    }

    @Override
    protected StepStatus businessExecute() {
        this.getLogger().debug("Start to use nopol to repair...");

        JobStatus jobStatus = this.getInspector().getJobStatus();

        File patchDir = new File(this.getInspector().getRepoLocalPath()+"/"+TOOL_RESULTS_FOLDER_NAME);
        patchDir.mkdir();

        JsonArray toolDiag = new JsonArray();
        List<RepairPatch> repairPatches = new ArrayList<>();
        Gson gson = new Gson();
        Metrics metric = jobStatus.getMetrics();
        List<URL> classPath = jobStatus.getRepairClassPath();
        File[] sources = jobStatus.getRepairSourceDir();

        if (classPath != null && sources != null) {
            String[] sourcesStr = new String[sources.length];

            int i = 0;
            for (File f : sources) {
                sourcesStr[i++] = f.getAbsolutePath();
            }

            List<FailureLocation> failureLocationList = new ArrayList<>(jobStatus.getFailureLocations());
            Collections.sort(failureLocationList, new ComparatorFailureLocation());

            boolean patchCreated = false;
            int passingTime = 0;

            for (FailureLocation failureLocation : failureLocationList) {
                NopolInformation nopolInformation = new NopolInformation(failureLocation, IgnoreStatus.NOTHING_TO_IGNORE);;
                nopolInformation.setStatus(NopolStatus.RUNNING);

                String testClass = failureLocation.getClassName();
                int timeout = (TOTAL_MAX_TIME - passingTime) / 2;
                if (timeout < MIN_TIMEOUT) {
                    timeout = MIN_TIMEOUT;
                }

                nopolInformation.setAllocatedTime(timeout);

                this.getLogger().debug("Launching repair with Nopol for following test class: " + testClass
                        + " (should timeout in " + timeout + " minutes)");

                NopolContext nopolContext = new NopolContext(sources, classPath.toArray(new URL[classPath.size()]), new String[] { testClass });
                nopolContext.setComplianceLevel(8);
                nopolContext.setTimeoutTestExecution(300);
                nopolContext.setMaxTimeEachTypeOfFixInMinutes(15);
                nopolContext.setMaxTimeInMinutes(timeout);
                nopolContext.setLocalizer(NopolContext.NopolLocalizer.COCOSPOON);
                nopolContext.setSolverPath(this.getConfig().getZ3solverPath());
                nopolContext.setSynthesis(NopolContext.NopolSynthesis.DYNAMOTH);
                nopolContext.setType(RepairType.COND_THEN_PRE);
                nopolContext.setOnlyOneSynthesisResult(false);
                nopolContext.setOutputFolder(patchDir.getAbsolutePath());

                nopolInformation.setNopolContext(nopolContext);

                SolverFactory.setSolver(nopolContext.getSolver(), nopolContext.getSolverPath());

                long beforeNopol = new Date().getTime();

                try {
                    final NoPol nopol = new NoPol(nopolContext);
                    Factory spoonFactory = nopol.getSpooner().spoonFactory();

                    final ExecutorService executor = Executors.newSingleThreadExecutor();
                    final Future<NopolResult> nopolExecution = executor.submit(new Callable<NopolResult>() {
                        @Override
                        public NopolResult call() throws Exception {
                            NopolResult result = null;
                            try {
                                result = nopol.build();
                            } catch (RuntimeException e) {
                                addStepError("Got runtime exception while running Nopol", e);
                            }
                            return result;
                        }
                    });

                    try {
                        executor.shutdown();
                        NopolResult result = nopolExecution.get(nopolContext.getMaxTimeInMinutes(), TimeUnit.MINUTES);

                        if (result == null) {
                            result = nopol.getNopolResult();
                        }
                        nopolInformation.setNbStatements(result.getNbStatements());
                        nopolInformation.setNbAngelicValues(result.getNbAngelicValues());

                        String failureLocationWithoutDots = failureLocation.getClassName().replace('.','/');

                        metric.addAngelicValueByTest(failureLocationWithoutDots, result.getNbAngelicValues());

                        List<Patch> patches = result.getPatches();
                        if (patches != null && !patches.isEmpty()) {
                            for (Patch patch : patches) {
                                String diff = patch.toDiff(spoonFactory, nopolContext);

                                RepairPatch repairPatch = new RepairPatch(this.getRepairToolName(), "", diff);
                                repairPatches.add(repairPatch);
                            }
                            nopolInformation.setStatus(NopolStatus.PATCH);
                            patchCreated = true;
                        } else {
                            nopolInformation.setStatus(NopolStatus.NOPATCH);
                        }
                    } catch (TimeoutException exception) {
                        this.addStepError("Timeout: execution time > " + nopolContext.getMaxTimeInMinutes() + " " + TimeUnit.MINUTES);
                        nopolExecution.cancel(true);
                        executor.shutdownNow();
                        nopolInformation.setStatus(NopolStatus.TIMEOUT);

                        NopolResult result = nopol.getNopolResult();
                        nopolInformation.setNbStatements(result.getNbStatements());
                        nopolInformation.setNbAngelicValues(result.getNbAngelicValues());

                    } catch (InterruptedException | ExecutionException e) {
                        this.addStepError(e.getMessage());
                        nopolExecution.cancel(true);
                        executor.shutdownNow();
                        nopolInformation.setStatus(NopolStatus.EXCEPTION);
                        nopolInformation.setExceptionDetail(e.getMessage());

                        NopolResult result = nopol.getNopolResult();
                        nopolInformation.setNbStatements(result.getNbStatements());
                        nopolInformation.setNbAngelicValues(result.getNbAngelicValues());
                    }
                } catch (SpoonException e) {
                    this.addStepError(e.getMessage());
                    nopolInformation.setStatus(NopolStatus.EXCEPTION);
                    nopolInformation.setExceptionDetail(e.getMessage());
                }


                long afterNopol = new Date().getTime();

                int localPassingTime = Math.round((afterNopol - beforeNopol) / 60000);
                nopolInformation.setPassingTime(localPassingTime);

                toolDiag.add(gson.toJsonTree(nopolInformation));

                passingTime += localPassingTime;
            }

            File nopolLog = new File(System.getProperty("user.dir"), "debug.log");
            if (nopolLog.exists()) {
                String nopolDestName = "repairnator.nopol.log";
                File nopolDest = new File(this.getInspector().getRepoLocalPath(), nopolDestName);
                try {
                    Files.move(nopolLog.toPath(), nopolDest.toPath());
                    jobStatus.addFileToPush(nopolDestName);
                } catch (IOException e) {
                    getLogger().error("Error while renaming nopol log", e);
                }
            }

            this.recordPatches(repairPatches);
            this.recordToolDiagnostic(toolDiag);

            try {
                deleteFile(patchDir);
            } catch (IOException e) {
                getLogger().error("Error while removing the temp folder containing Nopol output", e);
            }

            if (!patchCreated) {
                this.addStepError("No patch has been generated by Nopol. Look at the trace to get more information.");
                return StepStatus.buildSkipped(this,"No patch has been found.");
            }

            return StepStatus.buildSuccess(this);
        } else {
            this.addStepError("No classpath or sources directory has been given. Nopol can't be launched.");
            return StepStatus.buildSkipped(this,"No classpath or source directory given.");
        }
    }

}
