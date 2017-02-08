package fr.inria.spirals.repairnator.process;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.RepairMode;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.process.step.BuildProject;
import fr.inria.spirals.repairnator.process.step.CloneRepository;
import fr.inria.spirals.repairnator.process.step.GatherTestInformation;
import fr.inria.spirals.repairnator.process.step.ProjectState;
import fr.inria.spirals.repairnator.process.step.TestProject;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;

public class ProjectInspector4Bears extends ProjectInspector {
	private final Logger logger = LoggerFactory.getLogger(ProjectInspector4Bears.class);
	
	public ProjectInspector4Bears(Build build, String workspace, List<AbstractDataSerializer> serializers, String nopolSolverPath, boolean push, int steps, RepairMode mode) {
		super(build, workspace, serializers, null, push, steps, mode);
	}
	
	public void run() {
        AbstractStep firstStep = null;

        this.testInformations = new GatherTestInformation(this);
        
        AbstractStep cloneRepo = new CloneRepository(this);
        AbstractStep buildRepo = new BuildProject(this);
        AbstractStep testProject = new TestProject(this);
        cloneRepo.setNextStep(buildRepo).setNextStep(testProject).setNextStep(this.testInformations);
        firstStep = cloneRepo;
    
        firstStep.setLimitStepNumber(this.steps);
        firstStep.setDataSerializer(this.serializers);

        firstStep.setState(ProjectState.INIT);

        try {
            firstStep.execute();
        } catch (Exception e) {
            this.addStepError("Unknown", e.getMessage());
            this.logger.debug("Exception catch while executing steps: ",e);
        }
    }
}