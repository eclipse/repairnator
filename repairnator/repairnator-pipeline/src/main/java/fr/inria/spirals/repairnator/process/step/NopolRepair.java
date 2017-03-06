package fr.inria.spirals.repairnator.process.step;

import fr.inria.lille.commons.synthesis.smt.solver.SolverFactory;
import fr.inria.lille.repair.common.config.NopolContext;
import fr.inria.lille.repair.common.patch.Patch;
import fr.inria.lille.repair.common.synth.StatementType;
import fr.inria.lille.repair.nopol.NoPol;
import fr.inria.lille.repair.nopol.NopolResult;
import fr.inria.spirals.repairnator.ProjectState;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.nopol.NopolInformation;
import fr.inria.spirals.repairnator.process.nopol.NopolStatus;
import fr.inria.spirals.repairnator.process.testinformation.ComparatorFailureLocation;
import fr.inria.spirals.repairnator.process.testinformation.FailureLocation;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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
    private static final int TOTAL_MAX_TIME = 60 * 4; // We expect it to run 4
                                                      // hours top.
    private static final int MIN_TIMEOUT = 2;
    private static final String CMD_KILL_GZOLTAR_AGENT = "ps -ef | grep gzoltar | grep -v grep | awk '{print $2}' |xargs kill";

    private List<NopolInformation> nopolInformations;

    public NopolRepair(ProjectInspector inspector) {
        super(inspector);
        this.nopolInformations = new ArrayList<>();
    }

    public List<NopolInformation> getNopolInformations() {
        return nopolInformations;
    }

    @Override
    protected void businessExecute() {
        this.getLogger().debug("Start to use nopol to repair...");

        List<URL> classPath = this.inspector.getRepairClassPath();
        File[] sources = this.inspector.getRepairSourceDir();

        String[] sourcesStr = new String[sources.length];

        int i = 0;
        for (File f : sources) {
            sourcesStr[i++] = f.getAbsolutePath();
        }

        GatherTestInformation infoStep = inspector.getTestInformations();
        List<FailureLocation> failureLocationList = new ArrayList<>(infoStep.getFailureLocations());
        Collections.sort(failureLocationList, new ComparatorFailureLocation());

        boolean patchCreated = false;
        int passingTime = 0;

        for (FailureLocation failureLocation : failureLocationList) {
            NopolInformation nopolInformation = new NopolInformation(failureLocation);
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

            NopolContext nopolContext = new NopolContext(sources, classPath.toArray(new URL[classPath.size()]), new String[] { testClass });
            nopolContext.setComplianceLevel(8);
            nopolContext.setTimeoutTestExecution(300);
            nopolContext.setMaxTimeEachTypeOfFixInMinutes(15);
            nopolContext.setMaxTimeInMinutes(timeout);
            nopolContext.setLocalizer(NopolContext.NopolLocalizer.OCHIAI);
            nopolContext.setSolverPath(this.inspector.getNopolSolverPath());
            nopolContext.setSynthesis(NopolContext.NopolSynthesis.DYNAMOTH);
            nopolContext.setType(StatementType.COND_THEN_PRE);
            nopolContext.setOnlyOneSynthesisResult(false);

            nopolInformation.setNopolContext(nopolContext);

            SolverFactory.setSolver(nopolContext.getSolver(), nopolContext.getSolverPath());

            long beforeNopol = new Date().getTime();

            final NoPol nopol = new NoPol(nopolContext);
            List<Patch> patch = null;

            final ExecutorService executor = Executors.newSingleThreadExecutor();
            final Future<NopolResult> nopolExecution = executor.submit(new Callable() {
                @Override
                public Object call() throws Exception {
                    NopolResult result = null;
                    try {
                        result = nopol.build();
                    } catch (RuntimeException e) {
                        //e.printStackTrace();
                    }
                    return result;
                }
            });

            try {
                executor.shutdown();
                NopolResult result = nopolExecution.get(nopolContext.getMaxTimeInMinutes(), TimeUnit.MINUTES);

                nopolInformation.setNbStatements(result.getNbStatements());
                nopolInformation.setNbAngelicValues(result.getNbAngelicValues());
                patch = result.getPatches();
                if (patch != null && !patch.isEmpty()) {
                    nopolInformation.setPatches(patch);
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
            } catch (InterruptedException | ExecutionException e) {
                this.addStepError(e.getMessage());
                nopolExecution.cancel(true);
                executor.shutdownNow();
                nopolInformation.setStatus(NopolStatus.EXCEPTION);
                nopolInformation.setExceptionDetail(e.getMessage());
            }

            try {
                Runtime.getRuntime().exec(CMD_KILL_GZOLTAR_AGENT);
            } catch (IOException e) {
                this.getLogger()
                        .error("Error while killing gzoltar agent using following command: " + CMD_KILL_GZOLTAR_AGENT);
                this.getLogger().error(e.getMessage());
            }

            long afterNopol = new Date().getTime();

            nopolInformation.setDateEnd();

            int localPassingTime = Math.round((afterNopol - beforeNopol) / 60000);
            nopolInformation.setPassingTime(localPassingTime);

            passingTime += localPassingTime;
        }

        if (!patchCreated) {
            this.addStepError("No patch has been generated by Nopol. Look at the trace to get more information.");
            return;
        }
        this.setState(ProjectState.PATCHED);

    }

}
