package fr.inria.spirals.jtravis.pojos;

/**
 * Represents the config attribute object in a job object (see {@link fr.inria.spirals.jtravis.entities.Job})
 * This object is different from a {@link fr.inria.spirals.jtravis.pojos.BuildConfigPojo} for the jdk attribute which is a simple String here.
 *
 * @author Simon Urli
 */
public class JobConfigPojo extends ConfigPojo {
    private String jdk;

    public String getJdk() {
        return jdk;
    }

    public void setJdk(String jdk) {
        this.jdk = jdk;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        JobConfigPojo that = (JobConfigPojo) o;

        return jdk != null ? jdk.equals(that.jdk) : that.jdk == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (jdk != null ? jdk.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "JobConfigPojo{" +
                super.toString() +
                "jdk='" + jdk + '\'' +
                '}';
    }
}
