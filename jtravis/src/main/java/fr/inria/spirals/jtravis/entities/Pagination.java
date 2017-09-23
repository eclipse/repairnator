package fr.inria.spirals.jtravis.entities;

public class Pagination {
    private int limit;
    private int offset;
    private int count;
    private boolean is_first;
    private boolean is_last;

    public boolean isIs_last() { return is_last; }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }
}
