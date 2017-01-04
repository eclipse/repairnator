package fr.inria.spirals.repairnator;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.step.AbstractStep;
import fr.inria.spirals.repairnator.step.BuildProject;
import fr.inria.spirals.repairnator.step.CloneRepository;
import fr.inria.spirals.repairnator.step.TestProject;

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


    public ProjectInspector(Build failingBuild, String workspace) {
        this.build = failingBuild;
        this.state = ProjectState.NONE;
        this.workspace = workspace;
        this.repoLocalPath = workspace+File.separator+getRepoSlug();
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

        cloneRepo.setNextStep(buildRepo).setNextStep(testProject);
        this.state = cloneRepo.execute();

        this.stepsDurations.put("clone",cloneRepo.getDuration());
        this.stepsDurations.put("build",buildRepo.getDuration());
        this.stepsDurations.put("test",testProject.getDuration());
    }

    public Map<String, Integer> getStepsDurations() {
        return this.stepsDurations;
    }

    public void cleanInspector() {
        this.build.clearJobs();
    }

}
