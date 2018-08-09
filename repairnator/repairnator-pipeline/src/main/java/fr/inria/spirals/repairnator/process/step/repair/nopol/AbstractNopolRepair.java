package fr.inria.spirals.repairnator.process.step.repair.nopol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import fr.inria.lille.commons.synthesis.smt.solver.SolverFactory;
import fr.inria.lille.repair.common.config.NopolContext;
import fr.inria.lille.repair.common.patch.Patch;
import fr.inria.lille.repair.common.synth.RepairType;
import fr.inria.lille.repair.nopol.NoPol;
import fr.inria.lille.repair.nopol.NopolResult;
import fr.inria.spirals.repairnator.GsonPathTypeAdapter;
import fr.inria.spirals.repairnator.process.files.FileHelper;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.nopol.IgnoreStatus;
import fr.inria.spirals.repairnator.process.nopol.NopolInformation;
import fr.inria.spirals.repairnator.process.nopol.NopolStatus;
import fr.inria.spirals.repairnator.process.step.repair.AbstractRepairStep;
import fr.inria.spirals.repairnator.process.testinformation.FailureLocation;
import spoon.SpoonException;
import spoon.reflect.factory.Factory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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


/**
 * This step is used to launch Nopol using a repair strategy by trying first all test
 * and then only test in failure and finally only test in errors
 */
public abstract class AbstractNopolRepair extends AbstractRepairStep {
    public static int TOTAL_MAX_TIME = 60 * 4; // We expect it to run 4
                                                      // hours top.
    private static final int MIN_TIMEOUT = 2;
    private int passingTime;
    private Gson gson;
    private List<URL> classPath;
    private File[] sources;
    private File patchDir;
    private List<RepairPatch> repairPatches;
    private boolean patchCreated;
    private JsonArray toolDiag;

    public AbstractNopolRepair() {
        this.repairPatches = new ArrayList<>();
        this.toolDiag = new JsonArray();
        this.gson = new GsonBuilder().registerTypeAdapter(Path.class, new GsonPathTypeAdapter()).create();
        this.passingTime = 0;
    }

    public void setClassPath(List<URL> classPath) {
        this.classPath = classPath;
    }

    public void setSources(File[] sources) {
        this.sources = sources;
    }

    public String getToolResultsFolderName() {
        return "repairnator." + this.getRepairToolName().toLowerCase() + ".results";
    }

    public void initPatchDir() {
        this.patchDir = new File(this.getInspector().getRepoLocalPath()+"/"+this.getToolResultsFolderName());
        this.patchDir.mkdirs();
    }

    public void initWithJobStatus() {
        JobStatus jobStatus = this.getInspector().getJobStatus();
        this.setClassPath(jobStatus.getRepairClassPath());
        this.setSources(jobStatus.getRepairSourceDir());
    }

    public File[] getSources() {
        return sources;
    }

    public List<URL> getClassPath() {
        return classPath;
    }

    protected void runNopol(Set<FailureLocation> failureLocation, List<String> testsToIgnore, boolean ignoreError) {
        NopolInformation nopolInformation;
        if (testsToIgnore.isEmpty()) {
            nopolInformation = new NopolInformation(failureLocation, IgnoreStatus.NOTHING_TO_IGNORE);
        } else {
            if (ignoreError) {
                nopolInformation = new NopolInformation(failureLocation, IgnoreStatus.IGNORE_ERRORING);
                ignoreError = false;
            } else {
                nopolInformation = new NopolInformation(failureLocation, IgnoreStatus.IGNORE_FAILING);
            }
        }

        nopolInformation.setStatus(NopolStatus.RUNNING);

        List<String> testClass = new ArrayList<>();

        for (FailureLocation location : failureLocation) {
            testClass.add(location.getClassName());
        }

        int timeout = (TOTAL_MAX_TIME - this.passingTime) / 2;
        if (timeout < MIN_TIMEOUT) {
            timeout = MIN_TIMEOUT;
        }

        nopolInformation.setAllocatedTime(timeout);

        this.getLogger().debug("Launching repair with Nopol for following test class: " + testClass
                + " (should timeout in " + timeout + " minutes)");

        NopolContext nopolContext = new NopolContext(sources, classPath.toArray(new URL[classPath.size()]), testClass.toArray(new String[0]), testsToIgnore);
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

        String repoLocalPath = this.getInspector().getRepoLocalPath();
        if (repoLocalPath.startsWith("./")) {
            repoLocalPath = repoLocalPath.substring(2);
        }

        nopolContext.setRootProject(new File(repoLocalPath).toPath());

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

                StringBuilder failureLocationWithoutDots = new StringBuilder();
                for (FailureLocation location : failureLocation) {
                    failureLocationWithoutDots.append(location.getClassName().replace('.', '/'));
                    failureLocationWithoutDots.append(':');
                }

                List<Patch> patches = result.getPatches();
                if (patches != null && !patches.isEmpty()) {
                    for (Patch patch : patches) {
                        String diff = patch.toDiff(spoonFactory, nopolContext);
                        int i = 0;
                        File patchFile;
                        do {
                            File sourceFolder = getSources()[i];
                            patchFile = patch.getFile(sourceFolder);
                        } while (i < getSources().length && (patchFile == null || !patchFile.exists()));

                        RepairPatch repairPatch = new RepairPatch(this.getRepairToolName(), patchFile.getPath(), diff);
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

        this.toolDiag.add(gson.toJsonTree(nopolInformation));

        passingTime += localPassingTime;
    }

    protected StepStatus recordResults() {
        File nopolLog = new File(System.getProperty("user.dir"), "debug.log");
        if (nopolLog.exists()) {
            String nopolDestName = "repairnator.nopol.log";
            File nopolDest = new File(this.getInspector().getRepoLocalPath(), nopolDestName);
            try {
                Files.move(nopolLog.toPath(), nopolDest.toPath());
                this.getInspector().getJobStatus().addFileToPush(nopolDestName);
            } catch (IOException e) {
                getLogger().error("Error while renaming nopol log", e);
            }
        }

        this.recordPatches(repairPatches);
        this.recordToolDiagnostic(toolDiag);

        try {
            FileHelper.deleteFile(patchDir);
        } catch (IOException e) {
            getLogger().error("Error while removing the temp folder containing Nopol output", e);
        }

        if (!patchCreated) {
            this.addStepError("No patch has been generated by Nopol. Look at the trace to get more information.");
            return StepStatus.buildSkipped(this,"No patch has been found.");
        }

        return StepStatus.buildSuccess(this);
    }

}
