package fr.inria.spirals.repairnator;

/**
 * This class listen act as a listener server for incoming build for instance
 */
public interface Listener {
    void runListenerServer();
    void submitBuild(String buildStr);
}
