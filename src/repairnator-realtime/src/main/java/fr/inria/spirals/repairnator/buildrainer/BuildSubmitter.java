package fr.inria.spirals.repairnator.buildrainer;

import fr.inria.spirals.repairnator.InputBuild;

// Submit build ids received from websocket
public interface BuildSubmitter {
    void submitBuild(InputBuild b);
}
