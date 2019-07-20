package fr.inria.spirals.repairnator.realtime;

public interface PipelineRunner <T,C>{
    T submitBuild(C c);
}
