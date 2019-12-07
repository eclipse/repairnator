package fr.inria.jtravis.entities.v2;

import java.util.Objects;

import com.google.gson.annotations.Expose;

import fr.inria.jtravis.entities.Commit;
import fr.inria.jtravis.entities.Entity;

public final class BuildV2 extends Entity {

    @Expose
    private Commit commit;

    @Expose
    private String id;

    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }

    public Commit getCommit() {
        return commit;
    }

    public void setCommit(Commit commit) {
        this.commit = commit;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final BuildV2 buildV2 = (BuildV2) o;
        return commit.equals(buildV2.commit)&& id == buildV2.id;
    }

    @Override
    public int hashCode() {

        return Objects.hash(commit, id);
    }

    @Override
    public String toString() {
        return "JobV2{" +
                "id=" + id +
                ", commit=" + commit +
                '}';
    }

    @Override
    protected void dispatchJTravisToChildren() {
        //FIX protected commit.setJtravis() not in same package
        return;
    }
}
