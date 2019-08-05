package fr.inria.jtravis.entities.v2;

import com.google.gson.annotations.Expose;
import fr.inria.jtravis.entities.Config;
import fr.inria.jtravis.entities.StateType;

import java.util.Objects;

public final class JobV2 {

    @Expose
    private int id;

    @Expose
    private int repositoryId;

    @Expose
    private int buildId;

    @Expose
    private String number;

    @Expose
    private StateType state;

    @Expose
    private Config config;

    public int getId() {
        return id;
    }

    public int getRepositoryId() {
        return repositoryId;
    }

    public String getNumber() {
        return number;
    }

    public StateType getState() {
        return state;
    }

    public Config getConfig() {
        return config;
    }

    public int getBuildId() {
        return buildId;
    }

    protected void setId(int id) {
        this.id = id;
    }

    protected void setRepositoryId(int repositoryId) {
        this.repositoryId = repositoryId;
    }

    protected void setNumber(String number) {
        this.number = number;
    }

    protected void setState(StateType state) {
        this.state = state;
    }

    protected void setConfig(Config config) {
        this.config = config;
    }

    protected void setBuildId(int buildId) {
        this.buildId = buildId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final JobV2 jobV2 = (JobV2) o;
        return id == jobV2.id &&
                repositoryId == jobV2.repositoryId &&
                Objects.equals(number, jobV2.number) &&
                state == jobV2.state &&
                Objects.equals(config, jobV2.config);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, repositoryId, number, state, config);
    }

    @Override
    public String toString() {
        return "JobV2{" +
                "id=" + id +
                ", repositoryId=" + repositoryId +
                ", number='" + number + '\'' +
                ", state=" + state +
                ", config=" + config +
                '}';
    }

}
