package fr.inria.spirals.jtravis.pojos;

/**
 * Created by urli on 22/12/2016.
 */
public class RepositoryPojo {
    protected int id;
    protected String slug;
    protected boolean active;
    protected String description;
    protected int lastBuildId;

    public RepositoryPojo() {}

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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getLastBuildId() {
        return lastBuildId;
    }

    public void setLastBuildId(int lastBuildId) {
        this.lastBuildId = lastBuildId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RepositoryPojo that = (RepositoryPojo) o;

        if (id != that.id) return false;
        if (active != that.active) return false;
        if (lastBuildId != that.lastBuildId) return false;
        if (slug != null ? !slug.equals(that.slug) : that.slug != null) return false;
        return description != null ? description.equals(that.description) : that.description == null;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (slug != null ? slug.hashCode() : 0);
        result = 31 * result + (active ? 1 : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + lastBuildId;
        return result;
    }
}
