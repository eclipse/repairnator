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

    public Log getLog() {
        throw new NotImplementedException();
    }
}
