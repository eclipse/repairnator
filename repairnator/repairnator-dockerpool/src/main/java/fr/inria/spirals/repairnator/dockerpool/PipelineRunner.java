package fr.inria.spirals.repairnator.dockerpool;

public interface PipelineRunner <T,C>{
    T submitBuild(C c);
}
