package fr.inria.spirals.jtravis.pojos;

import java.util.List;

/**
 * Created by urli on 22/12/2016.
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
