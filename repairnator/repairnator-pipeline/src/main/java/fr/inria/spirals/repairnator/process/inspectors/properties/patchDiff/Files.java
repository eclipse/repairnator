package fr.inria.spirals.repairnator.process.inspectors.properties.patchDiff;

public class Files {

    private int numberAdded;
    private int numberChanged;
    private int numberDeleted;

    public Files() {}

    public int getNumberAdded() {
        return numberAdded;
    }

    public void setNumberAdded(int numberAdded) {
        this.numberAdded = numberAdded;
    }

    public int getNumberChanged() {
        return numberChanged;
    }

    public void setNumberChanged(int numberChanged) {
        this.numberChanged = numberChanged;
    }

    public int getNumberDeleted() {
        return numberDeleted;
    }

    public void setNumberDeleted(int numberDeleted) {
        this.numberDeleted = numberDeleted;
    }
}
