package fr.inria.spirals.repairnator.process.testinformation;

/**
 * Created by urli on 08/02/2017.
 */
public class FailureLocation {
    private String className;
    private String method;
    private String details;

    public FailureLocation(String className, String method, String details) {
        this.className = className;
        this.method = method;
        this.details = details;
    }

    public String getClassName() {
        return className;
    }

    public String getMethod() {
        return method;
    }

    public String getDetails() {
        return details;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FailureLocation that = (FailureLocation) o;

        if (className != null ? !className.equals(that.className) : that.className != null) return false;
        if (method != null ? !method.equals(that.method) : that.method != null) return false;
        return details != null ? details.equals(that.details) : that.details == null;
    }

    @Override
    public int hashCode() {
        int result = className != null ? className.hashCode() : 0;
        result = 31 * result + (method != null ? method.hashCode() : 0);
        result = 31 * result + (details != null ? details.hashCode() : 0);
        return result;
    }
}
