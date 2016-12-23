package fr.inria.spirals.jtravis.entities;

import fr.inria.spirals.jtravis.helpers.LogHelper;
import fr.inria.spirals.jtravis.pojos.JobPojo;

/**
 * Created by urli on 21/12/2016.
 */
public class Job extends JobPojo {
    private JobConfig config;
    private Log log;

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
        if (log == null && this.getLogId() > 0) {
            this.log = LogHelper.getLogFromId(this.getLogId());
        }
        return this.log;
    }
}
