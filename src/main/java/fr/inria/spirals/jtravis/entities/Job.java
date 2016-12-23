package fr.inria.spirals.jtravis.entities;

import fr.inria.spirals.jtravis.pojos.JobPojo;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Created by urli on 21/12/2016.
 */
public class Job extends JobPojo {
    private JobConfig config;

    public JobConfig getConfig() {
        return config;
    }

    public void setConfig(JobConfig config) {
        this.config = config;
    }

    public BuildStatus getBuildStatus() {
        if (this.getState() != null) {
            return BuildStatus.valueOf(this.getState().toUpperCase());
        } else {
            return null;
        }
    }

    public Log getLog() {
        throw new NotImplementedException();
    }
}
