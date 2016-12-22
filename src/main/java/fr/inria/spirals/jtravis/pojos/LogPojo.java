package fr.inria.spirals.jtravis.pojos;

/**
 * Created by urli on 22/12/2016.
 */
public class LogPojo {
    private int id;
    private String type;
    private String body;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LogPojo logPojo = (LogPojo) o;

        if (id != logPojo.id) return false;
        if (type != null ? !type.equals(logPojo.type) : logPojo.type != null) return false;
        return body != null ? body.equals(logPojo.body) : logPojo.body == null;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (body != null ? body.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "LogPojo{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", body='" + body + '\'' +
                '}';
    }
}
