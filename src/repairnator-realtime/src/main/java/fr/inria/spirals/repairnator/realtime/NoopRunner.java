package fr.inria.spirals.repairnator.realtime;

import fr.inria.spirals.repairnator.InputBuild;

/** the only runner with no bug: it does nothing! */
public class NoopRunner implements PipelineRunner {
    @Override
    public void submitBuild(InputBuild c) {
        System.out.println(c.toString());
        // no operation
    }

    @Override
    public void initRunner() {

    }
}
