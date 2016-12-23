package fr.inria.spirals.jtravis.pojos;

import java.util.List;

/**
 * An abstract object representing config attribute both for {@link fr.inria.spirals.jtravis.pojos.BuildPojo} and {@link fr.inria.spirals.jtravis.pojos.JobPojo}
 * It is keeping abstract as some differences still exist for each class.
 *
 * @author Simon Urli
 */
public abstract class ConfigPojo {
    private String language;
    private String sudo;
    private List<String> install;
    private String script;
    private String group;
    private String dist;
    private String os;

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getSudo() {
        return sudo;
    }

    public void setSudo(String sudo) {
        this.sudo = sudo;
    }

    public List<String> getInstall() {
        return install;
    }

    public void setInstall(List<String> install) {
        this.install = install;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getDist() {
        return dist;
    }

    public void setDist(String dist) {
        this.dist = dist;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConfigPojo that = (ConfigPojo) o;

        if (language != null ? !language.equals(that.language) : that.language != null) return false;
        if (sudo != null ? !sudo.equals(that.sudo) : that.sudo != null) return false;
        if (install != null ? !install.equals(that.install) : that.install != null) return false;
        if (script != null ? !script.equals(that.script) : that.script != null) return false;
        if (group != null ? !group.equals(that.group) : that.group != null) return false;
        if (dist != null ? !dist.equals(that.dist) : that.dist != null) return false;
        return os != null ? os.equals(that.os) : that.os == null;
    }

    @Override
    public int hashCode() {
        int result = language != null ? language.hashCode() : 0;
        result = 31 * result + (sudo != null ? sudo.hashCode() : 0);
        result = 31 * result + (install != null ? install.hashCode() : 0);
        result = 31 * result + (script != null ? script.hashCode() : 0);
        result = 31 * result + (group != null ? group.hashCode() : 0);
        result = 31 * result + (dist != null ? dist.hashCode() : 0);
        result = 31 * result + (os != null ? os.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ConfigPojo{" +
                "language='" + language + '\'' +
                ", sudo='" + sudo + '\'' +
                ", install=" + install +
                ", script='" + script + '\'' +
                ", group='" + group + '\'' +
                ", dist='" + dist + '\'' +
                ", os='" + os + '\'' +
                '}';
    }
}
