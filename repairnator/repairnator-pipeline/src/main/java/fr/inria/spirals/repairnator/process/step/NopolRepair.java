package fr.inria.spirals.repairnator.process.step;

import fr.inria.lille.commons.synthesis.smt.solver.SolverFactory;
import fr.inria.lille.repair.common.config.NopolContext;
import fr.inria.lille.repair.common.patch.Patch;
import fr.inria.lille.repair.common.synth.StatementType;
import fr.inria.lille.repair.nopol.NoPol;
import fr.inria.lille.repair.nopol.NopolResult;
import fr.inria.spirals.repairnator.process.inspectors.Metrics;
import fr.inria.spirals.repairnator.process.nopol.PatchAndDiff;
import fr.inria.spirals.repairnator.states.PipelineState;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.nopol.IgnoreStatus;
import fr.inria.spirals.repairnator.process.nopol.NopolInformation;
import fr.inria.spirals.repairnator.process.nopol.NopolStatus;
import fr.inria.spirals.repairnator.process.testinformation.ComparatorFailureLocation;
import fr.inria.spirals.repairnator.process.testinformation.FailureLocation;
import spoon.reflect.factory.Factory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by urli on 05/01/2017.
 */
public class NopolRepair extends AbstractStep {
    protected static int TOTAL_MAX_TIME = 60 * 4; // We expect it to run 4
                                                      // hours top.
    private static final int MIN_TIMEOUT = 2;
    private List<NopolInformation> nopolInformations;

    public NopolRepair(ProjectInspector inspector) {
        super(inspector);
        this.nopolInformations = new ArrayList<>();
        inspector.getJobStatus().setNopolInformations(this.nopolInformations);
    }

    public List<NopolInformation> getNopolInformations() {
        return nopolInformations;
    }

    @Override
    protected void businessExecute() {
        this.getLogger().debug("Start to use nopol to repair...");

        this.setPipelineState(PipelineState.NOPOL_NOTPATCHED);

        Metrics metric = this.inspector.getJobStatus().getMetrics();
        List<URL> classPath = this.inspector.getJobStatus().getRepairClassPath();
        File[] sources = this.inspector.getJobStatus().getRepairSourceDir();

        if (classPath != null && sources != null) {
            String[] sourcesStr = new String[sources.length];

            int i = 0;
            for (File f : sources) {
                sourcesStr[i++] = f.getAbsolutePath();
            }

            List<FailureLocation> failureLocationList = new ArrayList<>(this.inspector.getJobStatus().getFailureLocations());
            Collections.sort(failureLocationList, new ComparatorFailureLocation());

            boolean patchCreated = false;
            int passingTime = 0;

            for (FailureLocation failureLocation : failureLocationList) {
                Set<String> erroringTests = failureLocation.getErroringMethods();
                Set<String> failingTests = failureLocation.getFailingMethods();

                // this one is used to loop on Nopol over tests to ignore. It can be a list containing an empty list.
                List<List<String>> listOfTestToIgnore = new ArrayList<>();

                boolean ignoreError = false;
                // in that case: no tests to ignore
                if (erroringTests.isEmpty() || failingTests.isEmpty()) {
                    listOfTestToIgnore.add(new ArrayList<>());
                // then we will first try to ignore erroring tests, then to ignore failing tests
                } else {
                    listOfTestToIgnore.add(new ArrayList<>(erroringTests));
                    listOfTestToIgnore.add(new ArrayList<>(failingTests));

                    ignoreError = true;
                }

                for (List<String> testsToIgnore : listOfTestToIgnore) {
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
                    this.nopolInformations.add(nopolInformation);
                    nopolInformation.setStatus(NopolStatus.RUNNING);

                    String testClass = failureLocation.getClassName();
                    int timeout = (TOTAL_MAX_TIME - passingTime) / 2;
                    if (timeout < MIN_TIMEOUT) {
                        timeout = MIN_TIMEOUT;
                    }

                    nopolInformation.setAllocatedTime(timeout);

                    this.getLogger().debug("Launching repair with Nopol for following test class: " + testClass
                            + " (should timeout in " + timeout + " minutes)");

                    NopolContext nopolContext = new NopolContext(sources, classPath.toArray(new URL[classPath.size()]), new String[] { testClass }, testsToIgnore);
                    nopolContext.setComplianceLevel(8);
                    nopolContext.setTimeoutTestExecution(300);
                    nopolContext.setMaxTimeEachTypeOfFixInMinutes(15);
                    nopolContext.setMaxTimeInMinutes(timeout);
                    nopolContext.setLocalizer(NopolContext.NopolLocalizer.OCHIAI);
                    nopolContext.setSolverPath(this.getConfig().getZ3solverPath());
                    nopolContext.setSynthesis(NopolContext.NopolSynthesis.DYNAMOTH);
                    nopolContext.setType(StatementType.COND_THEN_PRE);
                    nopolContext.setOnlyOneSynthesisResult(false);

                    nopolInformation.setNopolContext(nopolContext);

                    SolverFactory.setSolver(nopolContext.getSolver(), nopolContext.getSolverPath());

                    long beforeNopol = new Date().getTime();

                    final NoPol nopol = new NoPol(nopolContext);
                    Factory spoonFactory = nopol.getSpooner().spoonFactory();
                    List<PatchAndDiff> patchAndDiffs = new ArrayList<>();

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
                                patchAndDiffs.add(new PatchAndDiff(patch, diff));
                            }
                            nopolInformation.setPatches(patchAndDiffs);
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
                    long afterNopol = new Date().getTime();

                    nopolInformation.setDateEnd();

                    int localPassingTime = Math.round((afterNopol - beforeNopol) / 60000);
                    nopolInformation.setPassingTime(localPassingTime);

                    passingTime += localPassingTime;
                }
            }

            File nopolLog = new File(this.getInspector().getRepoLocalPath(), "debug.log");
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
            File nopolProperties = new File(this.getInspector().getRepoLocalPath()+"/repairnator.nopol.results");

            this.getInspector().getJobStatus().addFileToPush("repairnator.nopol.results");

            File patchDir = new File(this.getInspector().getRepoLocalPath()+"/repairnatorPatches");

            patchDir.mkdir();
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(nopolProperties));

                int infoNumber = 0;
                for (NopolInformation information : this.nopolInformations) {
                    String informationStr = "nopolinfo #"+(infoNumber++)+"\n"
                                            +"location: "+information.getLocation()+"\n"
                                            +"status: "+information.getStatus().name()+"\n"
                                            +"dateEnd: "+information.getDateEnd().toString()+"\n"
                                            +"allocatedtime: "+information.getAllocatedTime()+"minutes \n"
                                            +"passingTime: "+information.getPassingTime()+"minutes \n"
                                            +"nb patches: "+information.getPatches().size()+"\n"
                                            +"nopol context: "+information.getNopolContext()+"\n"
                                            +"exception: "+information.getExceptionDetail()+"\n"
                                            +"nbStatements: "+information.getNbStatements()+"\n"
                                            +"nbAngelicValues: "+information.getNbAngelicValues()+"\n"
                                            +"ignoreStatus: "+information.getIgnoreStatus().name()+"\n"
                                            +"----------\n\n";

                    writer.write(informationStr);
                    writer.newLine();
                    writer.newLine();
                    writer.flush();

                    int patchNumber = 0;
                    for (PatchAndDiff patchAndDiff : information.getPatches()) {
                        File patchFile = new File(patchDir.getPath()+"/"+information.getLocation().getClassName()+"_patch_"+(patchNumber++));

                        Patch patch = patchAndDiff.getPatch();
                        BufferedWriter patchWriter = new BufferedWriter(new FileWriter(patchFile));
                        String patchWrite = "location: "+patch.getSourceLocation()+"\n"
                                            +"type: "+patch.getType()+"\n"
                                            +"patch: "+patchAndDiff.getDiff();

                        patchWriter.write(patchWrite);
                        patchWriter.flush();

                        patchWriter.close();
                    }
                }
                writer.close();
            } catch (IOException e) {
                this.addStepError("Error while writing nopol informations");
                this.getLogger().error("Error while writing nopol informations", e);
            }


            if (!patchCreated) {
                this.addStepError("No patch has been generated by Nopol. Look at the trace to get more information.");
                return;
            }
            this.setPipelineState(PipelineState.NOPOL_PATCHED);
            this.getInspector().getJobStatus().setHasBeenPatched(true);
            List<String> nopolPatches = new ArrayList<>();
            for (NopolInformation information : this.nopolInformations) {
                for (PatchAndDiff p : information.getPatches()) {
                    nopolPatches.add(p.getDiff());
                }
            }
            this.getInspector().getJobStatus().setNopolPatches(nopolPatches);
        } else {
            this.addStepError("No classpath or sources directory has been given. Nopol can't be launched.");
        }
    }

}
