package fr.inria.spirals.jtravis.entities;

/**
 * Created by urli on 21/12/2016.
 */
public class Repository {
    private int id;
    private String slug;
    private String description;
    private Build lastBuild;
    private boolean isActive;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Build getLastBuild() {
        return lastBuild;
    }

    public void setLastBuild(Build lastBuild) {
        this.lastBuild = lastBuild;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Repository that = (Repository) o;

        if (id != that.id) return false;
        if (isActive != that.isActive) return false;
        if (slug != null ? !slug.equals(that.slug) : that.slug != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        return lastBuild != null ? lastBuild.equals(that.lastBuild) : that.lastBuild == null;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
