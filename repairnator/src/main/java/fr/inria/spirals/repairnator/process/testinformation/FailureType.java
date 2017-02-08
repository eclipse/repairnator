package fr.inria.spirals.repairnator.process.testinformation;

import java.util.Arrays;

/**
 * Created by urli on 08/02/2017.
 */
public class FailureType {
    private static final String[] ERRORS = {
            "java.lang.ClassNotFoundException",
            "java.lang.NullPointerException",
            "java.lang.NoClassDefFoundError",
            "java.lang.RuntimeException"
    };

    private String failureName;
    private boolean isError;

    public FailureType(String name) {
        this.failureName = name;
        this.detectIfIsError();
    }

    private void detectIfIsError() {
        this.isError = (Arrays.asList(ERRORS).contains(this.failureName));
    }

    public String getFailureName() {
        return failureName;
    }

    public boolean isError() {
        return isError;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FailureType that = (FailureType) o;

        if (isError != that.isError) return false;
        return failureName != null ? failureName.equals(that.failureName) : that.failureName == null;
    }

    @Override
    public int hashCode() {
        int result = failureName != null ? failureName.hashCode() : 0;
        result = 31 * result + (isError ? 1 : 0);
        return result;
    }
}
