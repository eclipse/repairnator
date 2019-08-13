package fr.inria.spirals.repairnator.scanner;

import fr.inria.spirals.repairnator.Listener;

/**
 * A listener which does nothing.
 */
public class NoopListener implements Listener{
    private static Launcher launcher;

    public NoopListener (){}

    public void submitBuild(String buildStr){}
    public void runListenerServer() {}
}
