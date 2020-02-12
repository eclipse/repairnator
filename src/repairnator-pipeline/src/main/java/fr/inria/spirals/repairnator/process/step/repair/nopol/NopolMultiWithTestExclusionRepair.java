package fr.inria.spirals.repairnator.process.step.repair.nopol;

import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.testinformation.ComparatorFailureLocation;
import fr.inria.spirals.repairnator.process.testinformation.FailureLocation;
import java.util.*;

/**
 * This step is used to launch Nopol using a repair strategy by trying first all test
 * and then only test in failure and finally only test in errors
 */
public class NopolMultiWithTestExclusionRepair extends AbstractNopolRepair {
    protected static final String TOOL_NAME = "NopolTestExclusionStrategy";

    @Override
    protected StepStatus businessExecute() {
        this.getLogger().debug("Start to use nopol with test exclusion strategy to repair...");

        this.initPatchDir();
        this.initWithJobStatus();

        if (this.getClassPath() != null && this.getSources() != null) {

            List<FailureLocation> failureLocationList = new ArrayList<>(this.getInspector().getJobStatus().getFailureLocations());
            Collections.sort(failureLocationList, new ComparatorFailureLocation());

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
                    this.runNopol(Collections.singleton(failureLocation), testsToIgnore, ignoreError);
                }
            }

            return this.recordResults();
        } else {
            this.addStepError("No classpath or sources directory has been given. Nopol can't be launched.");
            return StepStatus.buildSkipped(this,"No classpath or source directory given.");
        }
    }

    @Override
    public String getRepairToolName() {
        return TOOL_NAME;
    }
}
