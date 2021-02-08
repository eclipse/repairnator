package fr.inria.spirals.repairnator.process.testinformation;

import java.util.Objects;

public abstract class BugType {

    private String name;
    private String detail;

    public BugType(String name, String detail) {
        this.name = name;
        this.detail = detail;
    }

    public String getName() {
        return name;
    }

    public String getDetail() {
        return detail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BugType bugType = (BugType) o;
        return name.equals(bugType.name) && detail.equals(bugType.detail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, detail);
    }

    @Override
    public String toString() {
        return "BugType{" +
                "name='" + name + '\'' +
                ", detail='" + detail + '\'' +
                '}';
    }

}
