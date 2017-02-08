package fr.inria.spirals.repairnator.process;

/**
 * Created by urli on 03/01/2017.
 */
public enum ProjectState {
    NONE,
    INIT,
    CLONABLE,
    BUILDABLE,
    TESTABLE,
    HASTESTFAILURE,
    HASTESTERRORS,
    NOTFAILING,
    PATCHED,
    NOTTESTABLE,
    CLASSPATHCOMPUTED,
    SOURCEDIRCOMPUTED
}
