package fr.inria.spirals.repairnator.process.testinformation;

import java.util.Comparator;

/**
 * Comparator to order FailureLocation based on the number of failing tests
 */
public class ComparatorFailureLocation implements Comparator<FailureLocation> {
    @Override
    public int compare(FailureLocation o1, FailureLocation o2) {
        if (o1.equals(o2)) {
            return 0;
        } else {
            int diffFail = o1.getNbFailures()-o2.getNbFailures();
            int diffErr = o1.getNbErrors()-o2.getNbErrors();

            return (diffFail != 0) ? diffFail : diffErr;
        }
    }
}
