package fr.inria.spirals.repairnator.realtime;

import fr.inria.jtravis.entities.Build;

/**
 * This interface describes the core behaviour
 * PipelineRunner Classes without using Docker.
 */
public interface PipelineRunner {

    /** submit a build for analysis */
    void submitBuild(Build c);

    /** do everything needed to initialize the runner */
    void initRunner();
}
