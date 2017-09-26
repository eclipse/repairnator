package fr.inria.spirals.jtravis.entities;

public class Pagination {
    private int limit;
    private int offset;
    private int count;
    private String href;
    private boolean is_first;
    private boolean is_last;
    private Pagination next;
    private Pagination prev;
    private Pagination first;
    private Pagination last;


    public boolean isIs_last() { return is_last; }

    public boolean isIs_first() { return is_first; }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public String getHref() { return href; }

    public void setHref(String href) { this.href = href; }

    public Pagination getNext() {
        return next;
    }

    public Pagination getFirst() {
        return first;
    }

    public Pagination getLast() {
        return last;
    }

    public Pagination getPrev() {
        return prev;
    }
}
