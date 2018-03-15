package fr.inria.spirals.repairnator.process.step;

import fr.inria.jtravis.entities.Build;
import fr.inria.jtravis.entities.Job;
import fr.inria.jtravis.entities.Log;
import fr.inria.jtravis.entities.TestsInformation;
import fr.inria.jtravis.helpers.JobHelper;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;

public class RetrieveTravisTestInformation extends AbstractStep {

    public RetrieveTravisTestInformation(ProjectInspector inspector) {
        super(inspector);
    }

    public RetrieveTravisTestInformation(ProjectInspector inspector, String name) {
        super(inspector, name);
    }

    @Override
    protected void businessExecute() {
        Build buggyBuild = this.getInspector().getBuggyBuild();

        if (buggyBuild.getJobs() != null && !buggyBuild.getJobs().isEmpty()) {
            Job firstJob = buggyBuild.getJobs().get(0);
            Log log = firstJob.getLog();
            TestsInformation testsInformation = log.getTestsInformation();

        }


    }
}
