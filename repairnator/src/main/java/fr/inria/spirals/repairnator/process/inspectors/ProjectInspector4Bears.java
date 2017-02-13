package fr.inria.spirals.repairnator.process.inspectors;

import java.util.List;

import fr.inria.spirals.repairnator.process.ProjectState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.RepairMode;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.process.step.BuildProject;
import fr.inria.spirals.repairnator.process.step.CloneRepository;
import fr.inria.spirals.repairnator.process.step.GatherTestInformation;
import fr.inria.spirals.repairnator.process.step.TestProject;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;

/**
 * Created by fermadeiral.
 */
public class ProjectInspector4Bears extends ProjectInspector {
	private final Logger logger = LoggerFactory.getLogger(ProjectInspector4Bears.class);
	
	private boolean previousBuild;
	
	public ProjectInspector4Bears(Build build, String workspace, List<AbstractDataSerializer> serializers, String nopolSolverPath, boolean push, RepairMode mode, boolean previousBuild) {
		super(build, workspace, serializers, null, push, mode);
		this.previousBuild = previousBuild;
	}
	
	public void run() {
        AbstractStep firstStep = null;

        this.testInformations = new GatherTestInformation(this);
        
        AbstractStep cloneRepo = new CloneRepository(this);
        AbstractStep buildRepo = new BuildProject(this);
        AbstractStep testProject = new TestProject(this);
        cloneRepo.setNextStep(buildRepo).setNextStep(testProject).setNextStep(this.testInformations);
        
        firstStep = cloneRepo;
        firstStep.setDataSerializer(this.serializers);

        firstStep.setState(ProjectState.INIT);

        try {
            firstStep.execute();
        } catch (Exception e) {
            this.addStepError("Unknown", e.getMessage());
            this.logger.debug("Exception catch while executing steps: ",e);
        }
    }
	
	public void proceed() {
		// Push build step will enter here, and other possible steps...
	}
	
	public boolean isAboutAPreviousBuild() {
		return previousBuild;
	}
}