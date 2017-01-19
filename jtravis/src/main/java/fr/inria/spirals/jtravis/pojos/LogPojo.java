package fr.inria.spirals.jtravis.pojos;

/**
 * Represent a log object in Travis CI API (see {@link https://docs.travis-ci.com/api#logs})
 *
 * @author Simon Urli
 */
public class LogPojo {
    private int id;
    private String type;
    private String body;
    private int jobId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LogPojo logPojo = (LogPojo) o;

        if (id != logPojo.id) return false;
        if (jobId != logPojo.jobId) return false;
        if (type != null ? !type.equals(logPojo.type) : logPojo.type != null) return false;
        return body != null ? body.equals(logPojo.body) : logPojo.body == null;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (body != null ? body.hashCode() : 0);
        result = 31 * result + jobId;
        return result;
    }

    @Override
    public String toString() {
        return "LogPojo{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", body='" + body + '\'' +
                ", jobId=" + jobId +
                '}';
    }
}
