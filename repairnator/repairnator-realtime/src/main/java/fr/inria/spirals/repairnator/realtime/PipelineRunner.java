package fr.inria.spirals.repairnator.realtime;

import fr.inria.spirals.repairnator.buildrainer.BuildSubmitter;

/**
 * This interface describes the core behaviour
 * PipelineRunner Classes without using Docker.
 */
public interface PipelineRunner extends BuildSubmitter{
    /** do everything needed to initialize the runner */
    void initRunner();
}
