package fr.inria.spirals.jtravis.entities;

import fr.inria.spirals.jtravis.helpers.LogHelper;
import fr.inria.spirals.jtravis.pojos.JobPojo;

/**
 * Business object to deal with job in Travis CI API
 * A job can get lazily its log information
 *
 * @author Simon Urli
 */
public class Job extends JobPojo {
    private Config config;
    private Log log;
    private BuildTool buildTool;

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
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
        if (log == null) {
            this.log = LogHelper.getLogFromJob(this);
        }
        return this.log;
    }

    public BuildTool getBuildTool() {
        if (buildTool == null) {
            if (this.getLog() != null) {
                buildTool = this.getLog().getBuildTool();
            } else {
                buildTool = BuildTool.UNKNOWN;
            }
        }

        return buildTool;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Job job = (Job) o;

        if (config != null ? !config.equals(job.config) : job.config != null) return false;
        return log != null ? log.equals(job.log) : job.log == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (config != null ? config.hashCode() : 0);
        result = 31 * result + (log != null ? log.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Job{" +
                super.toString() +
                "config=" + config +
                ", log=" + log +
                '}';
    }
}
