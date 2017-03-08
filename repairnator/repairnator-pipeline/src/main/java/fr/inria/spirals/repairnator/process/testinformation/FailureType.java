package fr.inria.spirals.repairnator.process.testinformation;

/**
 * Created by urli on 08/02/2017.
 */
public class FailureType {

    private String failureName;
    private String failureDetail;
    private boolean isError;

    public FailureType(String name, String detail, boolean isError) {
        this.failureName = name;
        this.failureDetail = detail;
        this.isError = isError;
    }

    public String getFailureName() {
        return failureName;
    }

    public boolean isError() {
        return isError;
    }

    public String getFailureDetail() {
        return failureDetail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FailureType that = (FailureType) o;

        if (isError != that.isError) {
            return false;
        }
        if (failureName != null ? !failureName.equals(that.failureName) : that.failureName != null) {
            return false;
        }
        return failureDetail != null ? failureDetail.equals(that.failureDetail) : that.failureDetail == null;
    }

    @Override
    public int hashCode() {
        int result = failureName != null ? failureName.hashCode() : 0;
        result = 31 * result + (failureDetail != null ? failureDetail.hashCode() : 0);
        result = 31 * result + (isError ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FailureType{" +
                "failureName='" + failureName + '\'' +
                ", failureDetail='" + failureDetail + '\'' +
                ", isError=" + isError +
                '}';
    }
}
