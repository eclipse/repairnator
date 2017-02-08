package fr.inria.spirals.repairnator.process;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.RepairMode;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.process.step.BuildProject;
import fr.inria.spirals.repairnator.process.step.CloneRepository;
import fr.inria.spirals.repairnator.process.step.GatherTestInformation;
import fr.inria.spirals.repairnator.process.step.NopolRepair;
import fr.inria.spirals.repairnator.process.step.PushIncriminatedBuild;
import fr.inria.spirals.repairnator.process.step.TestProject;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
    private RepairMode mode;
    private List<URL> repairClassPath;
    private File[] repairSourceDir;


    public ProjectInspector(Build failingBuild, String workspace, List<AbstractDataSerializer> serializers, String nopolSolverPath, boolean push, int steps, RepairMode mode) {
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
        this.serializers = serializers;
        this.mode = mode;
    }

    public List<URL> getRepairClassPath() {
        return repairClassPath;
    }

    public void setRepairClassPath(List<URL> repairClassPath) {
        this.repairClassPath = repairClassPath;
    }

    public File[] getRepairSourceDir() {
        return repairSourceDir;
    }

    public void setRepairSourceDir(File[] repairSourceDir) {
        this.repairSourceDir = repairSourceDir;
    }

    public String getWorkspace() {
        return workspace;
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

    public void setState(ProjectState state) {
        this.state = state;
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

    public void addStepDuration(String step, int duration) {
        this.stepsDurationsInSeconds.put(step, duration);
    }

    public void processRepair() {

        AbstractStep firstStep = null;

        this.testInformations = new GatherTestInformation(this);
        this.pushBuild = new PushIncriminatedBuild(this);
        this.nopolRepair = new NopolRepair(this);

        if (mode != RepairMode.NOPOLONLY) {
            AbstractStep cloneRepo = new CloneRepository(this);
            AbstractStep buildRepo = new BuildProject(this);
            AbstractStep testProject = new TestProject(this);
            cloneRepo.setNextStep(buildRepo).setNextStep(testProject).setNextStep(this.testInformations);
            firstStep = cloneRepo;
        }



        if (mode == RepairMode.NOPOLONLY) {
            firstStep = this.testInformations;
            try {
                Properties properties = ProjectScanner.getPropertiesFromFile(this.getRepoLocalPath()+File.separator+AbstractStep.PROPERTY_FILENAME);
                firstStep.setProperties(properties);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        firstStep.setLimitStepNumber(this.steps);
        firstStep.setDataSerializer(this.serializers);

        if (push) {
            this.testInformations.setNextStep(this.pushBuild)
                                .setNextStep(this.nopolRepair);
        } else {
            this.logger.debug("Push boolean is set to false the failing builds won't be pushed.");
            this.testInformations.setNextStep(this.nopolRepair);
        }

        firstStep.setState(ProjectState.INIT);

        try {
            firstStep.execute();
        } catch (Exception e) {
            this.addStepError("Unknown", e.getMessage());
            this.logger.debug("Exception catch while executing steps: ",e);
        }
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
