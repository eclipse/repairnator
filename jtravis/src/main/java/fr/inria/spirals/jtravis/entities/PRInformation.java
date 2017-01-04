package fr.inria.spirals.jtravis.entities;

/**
 * Created by urli on 04/01/2017.
 */
public class PRInformation {

    private Commit head;
    private Commit base;
    private Repository otherRepo;

    public PRInformation() {}

    public Commit getHead() {
        return head;
    }

    public void setHead(Commit head) {
        this.head = head;
    }

    public Commit getBase() {
        return base;
    }

    public void setBase(Commit base) {
        this.base = base;
    }

    public Repository getOtherRepo() {
        return otherRepo;
    }

    public void setOtherRepo(Repository otherRepo) {
        this.otherRepo = otherRepo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PRInformation that = (PRInformation) o;

        if (head != null ? !head.equals(that.head) : that.head != null) return false;
        if (base != null ? !base.equals(that.base) : that.base != null) return false;
        return otherRepo != null ? otherRepo.equals(that.otherRepo) : that.otherRepo == null;
    }

    @Override
    public int hashCode() {
        int result = head != null ? head.hashCode() : 0;
        result = 31 * result + (base != null ? base.hashCode() : 0);
        result = 31 * result + (otherRepo != null ? otherRepo.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PRInformation{" +
                "head=" + head +
                ", base=" + base +
                ", otherRepo=" + otherRepo +
                '}';
    }
}
