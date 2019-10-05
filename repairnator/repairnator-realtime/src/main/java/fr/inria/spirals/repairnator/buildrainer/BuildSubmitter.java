package fr.inria.spirals.repairnator.buildrainer;

import fr.inria.jtravis.entities.Build;

// Submit build ids received from websocket
public interface BuildSubmitter {
    void submitBuild(Build b);
}
