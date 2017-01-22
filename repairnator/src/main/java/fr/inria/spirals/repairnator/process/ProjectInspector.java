package fr.inria.spirals.repairnator.process;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.process.step.BuildProject;
import fr.inria.spirals.repairnator.process.step.CloneRepository;
import fr.inria.spirals.repairnator.process.step.GatherTestInformation;
import fr.inria.spirals.repairnator.process.step.NopolRepair;
import fr.inria.spirals.repairnator.process.step.ProjectState;
import fr.inria.spirals.repairnator.process.step.PushIncriminatedBuild;
import fr.inria.spirals.repairnator.process.step.TestProject;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by urli on 26/12/2016.
 */
public class ProjectInspector {
    private final Logger logger = LoggerFactory.getLogger(ProjectInspector.class);
    private Build build;
    private String repoLocalPath;
    private ProjectState state;
    private String workspace;
    private String nopolSolverPath;
    Map<String, Integer> stepsDurationsInSeconds;
    private GatherTestInformation testInformations;
    private PushIncriminatedBuild pushBuild;
    private NopolRepair nopolRepair;
    private boolean push;
    private int steps;
    private Map<String, List<String>> stepErrors;
    private boolean autoclean;
    private String m2LocalPath;
    private List<AbstractDataSerializer> serializers;


    public ProjectInspector(Build failingBuild, String workspace, List<AbstractDataSerializer> serializers, String nopolSolverPath, boolean push, int steps) {
        this.build = failingBuild;
        this.state = ProjectState.NONE;
        this.workspace = workspace;
        this.nopolSolverPath = nopolSolverPath;
        this.repoLocalPath = workspace+File.separator+getRepoSlug()+File.separator+build.getId();
        this.m2LocalPath = new File(workspace+File.separator+".m2").getAbsolutePath();
        this.stepsDurationsInSeconds = new HashMap<String, Integer>();
        this.push = push;
        this.steps = steps;
        this.stepErrors = new HashMap<String, List<String>>();
        this.autoclean = false;
    }

    public String getM2LocalPath() {
        return m2LocalPath;
    }

    public boolean isAutoclean() {
        return autoclean;
    }

    public void setAutoclean(boolean autoclean) {
        this.autoclean = autoclean;
    }

    public void setRepoLocalPath(String repoLocalPath) {
        this.repoLocalPath = repoLocalPath;
    }

    public String getNopolSolverPath() {
        return nopolSolverPath;
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

    public void addStepError(String step, String error) {
        if (!stepErrors.containsKey(step)) {
            stepErrors.put(step, new ArrayList<String>());
        }

        List<String> errors = stepErrors.get(step);
        errors.add(error);
    }

    public Map<String, List<String>> getStepErrors() {
        return stepErrors;
    }

    public void processRepair() {

        AbstractStep cloneRepo = new CloneRepository(this);
        AbstractStep buildRepo = new BuildProject(this);
        AbstractStep testProject = new TestProject(this);
        this.testInformations = new GatherTestInformation(this);
        this.pushBuild = new PushIncriminatedBuild(this);
        this.nopolRepair = new NopolRepair(this);


        cloneRepo.setLimitStepNumber(this.steps);
        cloneRepo.setDataSerializer(this.serializers);
        cloneRepo
                .setNextStep(buildRepo)
                .setNextStep(testProject)
                .setNextStep(this.testInformations);

        if (push) {
            this.testInformations.setNextStep(this.pushBuild)
                                .setNextStep(this.nopolRepair);
        } else {
            this.logger.debug("Push boolean is set to false the failing builds won't be pushed.");
            this.testInformations.setNextStep(this.nopolRepair);
        }

        cloneRepo.setState(ProjectState.INIT);
        this.state = cloneRepo.execute();

        this.stepsDurationsInSeconds.put("clone",cloneRepo.getDuration());
        this.stepsDurationsInSeconds.put("build",buildRepo.getDuration());
        this.stepsDurationsInSeconds.put("test",testProject.getDuration());
        this.stepsDurationsInSeconds.put("gatherInfo",this.testInformations.getDuration());
        this.stepsDurationsInSeconds.put("pushFail",this.pushBuild.getDuration());
        this.stepsDurationsInSeconds.put("nopolRepair",this.nopolRepair.getDuration());
    }

    public Map<String, Integer> getStepsDurationsInSeconds() {
        return this.stepsDurationsInSeconds;
    }

    public GatherTestInformation getTestInformations() {
        return testInformations;
    }

    public PushIncriminatedBuild getPushBuild() {
        return pushBuild;
    }

    public NopolRepair getNopolRepair() {
        return nopolRepair;
    }

    public String toString() {
        return this.getRepoLocalPath()+" : "+this.getState();
    }
}
