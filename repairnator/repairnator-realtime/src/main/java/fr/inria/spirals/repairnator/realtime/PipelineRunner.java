package fr.inria.spirals.repairnator.realtime;

import fr.inria.jtravis.entities.Build;

public interface PipelineRunner {
    void submitBuild(Build build);
}
