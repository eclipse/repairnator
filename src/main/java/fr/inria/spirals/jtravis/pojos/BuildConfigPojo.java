package fr.inria.spirals.jtravis.pojos;

import java.util.List;

/**
 *
 * Represents the config attribute object in a build object (see {@link fr.inria.spirals.jtravis.entities.Build})
 * This object is different from a {@link fr.inria.spirals.jtravis.pojos.JobConfigPojo} for the jdk attribute which is a list of String here.
 *
 * @author Simon Urli
 */
public class BuildConfigPojo extends ConfigPojo {
    private List<String> jdk;

    public List<String> getJdk() {
        return jdk;
    }

    public void setJdk(List<String> jdk) {
        this.jdk = jdk;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BuildConfigPojo that = (BuildConfigPojo) o;

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
        return "BuildConfigPojo{" +
                super.toString()+
                "jdk=" + jdk +
                '}';
    }
}
