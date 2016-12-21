package fr.inria.spirals.jtravis.entities;

/**
 * Created by urli on 21/12/2016.
 */
public class Log {
    private int id;
    private Job job;
    private String body;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
