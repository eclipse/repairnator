package fr.inria.spirals.repairnator.process.inspectors.properties.patchDiff;

public class Lines {

    private int numberAdded;
    private int numberDeleted;

    public Lines() {}

    public int getNumberAdded() {
        return numberAdded;
    }

    public void setNumberAdded(int numberAdded) {
        this.numberAdded = numberAdded;
    }

    public int getNumberDeleted() {
        return numberDeleted;
    }

    public void setNumberDeleted(int numberDeleted) {
        this.numberDeleted = numberDeleted;
    }
}
