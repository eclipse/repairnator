package fr.inria.spirals.repairnator.realtime;

import fr.inria.jtravis.entities.Build;

/** the only runner with no bug: it does nothing! */
public class NoopRunner implements PipelineRunner,BuildSubmitter {
    @Override
    public void submitBuild(Build c) {
        System.out.println(c.toString());
        // no operation
    }

    @Override
    public void initRunner() {

    }
}
