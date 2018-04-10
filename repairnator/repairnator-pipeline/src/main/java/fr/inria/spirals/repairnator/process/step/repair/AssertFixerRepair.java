package fr.inria.spirals.repairnator.process.step.repair;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.AbstractStep;

import java.io.File;
import java.net.URL;
import java.util.List;

public class AssertFixerRepair extends AbstractStep {
    private static final int TOTAL_TIME = 30*60; // 30 minutes

    public AssertFixerRepair(ProjectInspector inspector) {
        super(inspector);
    }

    public AssertFixerRepair(ProjectInspector inspector, String name) {
        super(inspector, name);
    }

    @Override
    protected void businessExecute() {
        this.getLogger().info("Start AssertFixerRepair");
        List<URL> classPath = this.inspector.getJobStatus().getRepairClassPath();
        File[] sources = this.inspector.getJobStatus().getRepairSourceDir();
        File[] tests = this.inspector.getJobStatus().getTestDir();

        
    }
}
