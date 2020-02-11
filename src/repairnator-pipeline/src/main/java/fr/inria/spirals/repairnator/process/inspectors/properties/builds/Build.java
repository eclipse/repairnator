package fr.inria.spirals.repairnator.process.inspectors.properties.builds;

import java.util.Date;

public class Build {

    private long id;
    private String url;
    private Date date;

    public Build(long id, String url, Date date) {
        this.id = id;
        this.url = url;
        this.date = date;
    }

    public long getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public Date getDate() {
        return date;
    }

}
