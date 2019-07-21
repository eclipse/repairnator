package fr.inria.spirals.repairnator.realtime;

/**
 * This interface describes the core behaviour
 * PipelineRunner Classes without using Docker.
 */
public interface PipelineRunner <T,C>{
    T submitBuild(C c);
}
