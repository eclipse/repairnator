package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;
import fr.inria.spirals.repairnator.process.testinformation.FailureLocation;
import fr.inria.spirals.repairnator.process.testinformation.FailureType;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by urli on 10/07/2017.
 */
public class NPERepair extends AbstractStep {
    private static final String NPEFIX_GOAL = "fr.inria.spirals:npefix-maven:1.0-SNAPSHOT:npefix";

    public NPERepair(ProjectInspector inspector) {
        super(inspector);
    }

    public NPERepair(ProjectInspector inspector, String name) {
        super(inspector, name);
    }

    private boolean isThereNPE() {
        for (FailureLocation failureLocation : this.inspector.getJobStatus().getFailureLocations()) {
            for (FailureType failureType : failureLocation.getFailures()) {
                if (failureType.getFailureName().equals("java.lang.NullPointerException")) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void businessExecute() {
        this.getLogger().debug("Entrance in NPERepair step...");

        if (isThereNPE()) {
            this.getLogger().info("NPE found, start NPEFix");

            MavenHelper mavenHelper = new MavenHelper(this.getInspector().getJobStatus().getFailingModulePath()+"/pom.xml", NPEFIX_GOAL, null, this.getName(), this.getInspector(), true );
            int status = mavenHelper.run();

            if (status == MavenHelper.MAVEN_ERROR) {
                this.getLogger().warn("Error while running NPE fix");
            } else {
                try {
                    FileUtils.moveFile(new File(this.getInspector().getJobStatus().getFailingModulePath()+"/target/npefix/patches.json"), new File(this.getInspector().getRepoLocalPath()+"/repairnator.npefix.results"));
                } catch (IOException e) {
                    this.addStepError("Error while moving NPE fix results", e);
                }
            }
        } else {
            this.getLogger().info("No NPE found, this step will be skipped.");
        }
    }
}
