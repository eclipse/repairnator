package fr.inria.spirals.repairnator.scanner;

import fr.inria.spirals.repairnator.Listener;

/**
 * This class fetch build ids from ActiveMQ queue and run the pipeline with it.
 */
public class NoopListener implements Listener{
    private static Launcher launcher;

    public NoopListener (){}

    public void runListenerServer() {}
}
