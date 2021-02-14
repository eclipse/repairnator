package fr.inria.spirals.repairnator.process.testinformation;

/**
 * Created by urli on 08/02/2017.
 */
public class FailureType extends BugType {

    /**
     * Class representing a type of failure that occurred during a test execution
     *
     * @param name   The name of the type of failure
     * @param detail The detailed message generated for this failure
     */
    public FailureType(String name, String detail) {
        super(name, detail);
    }

}
