package fr.inria.spirals.repairnator.process;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.process.step.BuildProject;
import fr.inria.spirals.repairnator.process.step.CloneRepository;
import fr.inria.spirals.repairnator.process.step.GatherTestInformation;
import fr.inria.spirals.repairnator.process.step.ProjectState;
import fr.inria.spirals.repairnator.process.step.PushIncriminatedBuild;
import fr.inria.spirals.repairnator.process.step.TestProject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by urli on 26/12/2016.
 */
public class ProjectInspector {
    private Build build;
    private String repoLocalPath;
    private ProjectState state;
    private String workspace;
    Map<String, Integer> stepsDurations;
    private GatherTestInformation testInformations;
    private PushIncriminatedBuild pushBuild;
    private boolean push;


    public ProjectInspector(Build failingBuild, String workspace, boolean push) {
        this.build = failingBuild;
        this.state = ProjectState.NONE;
        this.workspace = workspace;
        this.repoLocalPath = workspace+File.separator+getRepoSlug()+File.separator+build.getId();
        this.stepsDurations = new HashMap<String, Integer>();
    }

    public ProjectState getState() {
        return state;
    }

    public Build getBuild() {
        return build;
    }

    public String getRepoSlug() {
        return this.build.getRepository().getSlug();
    }

    public String getRepoLocalPath() {
        return repoLocalPath;
    }

    public void processRepair() {

        AbstractStep cloneRepo = new CloneRepository(this);
        AbstractStep buildRepo = new BuildProject(this);
        AbstractStep testProject = new TestProject(this);
        this.testInformations = new GatherTestInformation(this);
        this.pushBuild = new PushIncriminatedBuild(this);

        cloneRepo
                .setNextStep(buildRepo)
                .setNextStep(testProject)
                .setNextStep(this.testInformations);

        if (push) {
            this.testInformations.setNextStep(this.pushBuild);
        }

        cloneRepo.setState(ProjectState.INIT);
        this.state = cloneRepo.execute();

        this.stepsDurations.put("clone",cloneRepo.getDuration());
        this.stepsDurations.put("build",buildRepo.getDuration());
        this.stepsDurations.put("test",testProject.getDuration());
        this.stepsDurations.put("gatherInfo",this.testInformations.getDuration());
        this.stepsDurations.put("pushFail",this.pushBuild.getDuration());
    }

    public Map<String, Integer> getStepsDurations() {
        return this.stepsDurations;
    }

    public GatherTestInformation getTestInformations() {
        return testInformations;
    }

    public PushIncriminatedBuild getPushBuild() {
        return pushBuild;
    }

    public String toString() {
        return this.getRepoLocalPath()+" : "+this.getState();
    }
}
