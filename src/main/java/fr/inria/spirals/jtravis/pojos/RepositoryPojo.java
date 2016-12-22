package fr.inria.spirals.jtravis.pojos;

/**
 * Created by urli on 22/12/2016.
 */
public class RepositoryPojo {
    protected int id;
    protected String slug;
    protected boolean active;
    protected String description;
    protected int last_build_id;

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

    public int getLast_build_id() {
        return last_build_id;
    }

    public void setLast_build_id(int last_build_id) {
        this.last_build_id = last_build_id;
    }
}
