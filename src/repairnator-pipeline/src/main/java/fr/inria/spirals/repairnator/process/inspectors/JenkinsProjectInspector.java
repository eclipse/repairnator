package fr.inria.spirals.repairnator.process.inspectors;

import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.AbstractNotifier;
import fr.inria.spirals.repairnator.notifier.ErrorNotifier;
import fr.inria.spirals.repairnator.notifier.PatchNotifier;
import fr.inria.spirals.repairnator.pipeline.RepairToolsManager;
import fr.inria.spirals.repairnator.process.git.GitHelper;
import fr.inria.spirals.repairnator.process.inspectors.properties.Properties;
import fr.inria.spirals.repairnator.process.inspectors.properties.machineInfo.MachineInfo;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.process.step.AddExperimentalPluginRepo;
import fr.inria.spirals.repairnator.process.step.BuildProject;
import fr.inria.spirals.repairnator.process.step.JenkinsCloneRepository;
import fr.inria.spirals.repairnator.process.step.TestProject;
import fr.inria.spirals.repairnator.process.step.WritePropertyFile;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuild;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutPatchedBuild;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutType;
import fr.inria.spirals.repairnator.process.step.gatherinfo.BuildShouldFail;
import fr.inria.spirals.repairnator.process.step.gatherinfo.BuildShouldPass;
import fr.inria.spirals.repairnator.process.step.gatherinfo.GatherTestInformation;
import fr.inria.spirals.repairnator.process.step.paths.ComputeClasspath;
import fr.inria.spirals.repairnator.process.step.paths.ComputeModules;
import fr.inria.spirals.repairnator.process.step.paths.ComputeSourceDir;
import fr.inria.spirals.repairnator.process.step.paths.ComputeTestDir;
import fr.inria.spirals.repairnator.process.step.push.CommitPatch;
import fr.inria.spirals.repairnator.process.step.push.CommitProcessEnd;
import fr.inria.spirals.repairnator.process.step.push.CommitType;
import fr.inria.spirals.repairnator.process.step.push.InitRepoToPush;
import fr.inria.spirals.repairnator.process.step.push.PushProcessEnd;
import fr.inria.spirals.repairnator.process.step.repair.AbstractRepairStep;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import fr.inria.spirals.repairnator.utils.Utils;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import java.util.Map;

/**
 * This class initialize the pipelines by creating the steps:
 * it's the backbone of the pipeline.
 */
public class JenkinsProjectInspector extends ProjectInspector{
    private final Logger logger = LoggerFactory.getLogger(ProjectInspector.class);

    private GitHelper gitHelper;
    private BuildToBeInspected buildToBeInspected;
    private String gitUrl;
    private String gitSlug;
    private String gitBranch;
    private String gitCommit;
    private String repoLocalPath;
    private String repoToPushLocalPath;

    private String workspace;
    private String m2LocalPath;
    private List<AbstractDataSerializer> serializers;
    private JobStatus jobStatus;
    private List<AbstractNotifier> notifiers;
    private PatchNotifier patchNotifier;

    private CheckoutType checkoutType;

    private List<AbstractStep> steps;
    private AbstractStep finalStep;
    private boolean pipelineEnding;

    public JenkinsProjectInspector(BuildToBeInspected buildToBeInspected, String workspace, List<AbstractDataSerializer> serializers, List<AbstractNotifier> notifiers) {
        super(buildToBeInspected,workspace,serializers,notifiers);
    }

    public JenkinsProjectInspector(String workspace,String gitUrl,String gitBranch,String gitCommit,List<AbstractDataSerializer> serializers, List<AbstractNotifier> notifiers) {
        super(workspace,gitUrl,gitBranch,gitCommit,serializers,notifiers);
        this.gitUrl = gitUrl;
        this.gitBranch = gitBranch;
        this.gitCommit = gitCommit;
        this.gitSlug = this.gitUrl.split("https://github.com/",2)[1].replace(".git","");
        this.workspace = workspace;
        this.repoLocalPath = workspace + File.separator + this.getRepoSlug();
        this.repoToPushLocalPath = repoLocalPath+"_topush";
        this.m2LocalPath = new File(this.repoLocalPath + File.separator + ".m2").getAbsolutePath();
        this.serializers = null;
        this.gitHelper = new GitHelper();
        this.jobStatus = new JobStatus(repoLocalPath);
        this.notifiers = notifiers;
        this.checkoutType = CheckoutType.NO_CHECKOUT;
        this.steps = new ArrayList<>();
        this.initProperties();
    }

    /* This is the new branch for end process */
    @Override
    public String getRemoteBranchName() {
        return this.getRepoSlug().replace('/', '-');
    }

    @Override
    public String getRepoSlug() {
        return this.gitSlug;
    }

    public String getGitUrl() {
        return this.gitUrl;
    }

    /* This is the branch , which repairnator will repair*/
    public String getCheckoutBranchName() {
        return this.gitBranch;
    }

    public String getGitCommit() {
        return this.gitCommit;
    }

    @Override
    protected void initProperties() {
        try {
            Properties properties = this.jobStatus.getProperties();

            /* ProcessDurations use checkoutBuggyBuild*/

            fr.inria.spirals.repairnator.process.inspectors.properties.repository.Repository repository = properties.getRepository();
            repository.setName(this.getRepoSlug());
            repository.setUrl(this.getGitUrl());
            GitHub gitHub;
            try {
                gitHub = RepairnatorConfig.getInstance().getGithub();
                GHRepository repo = gitHub.getRepository(this.getRepoSlug());
                repository.setGithubId(repo.getId());
                if (repo.isFork()) {
                    repository.setIsFork(true);
                    repository.getOriginal().setName(repo.getParent().getFullName());
                    repository.getOriginal().setGithubId(repo.getParent().getId());
                    repository.getOriginal().setUrl(Utils.getSimpleGithubRepoUrl(repo.getParent().getFullName()));
                }
            } catch (IOException e) {
                this.logger.warn("It was not possible to retrieve information to check if " + this.getRepoSlug() + " is a fork.");
                this.logger.debug(e.toString());
            }
        } catch (Exception e) {
            this.logger.error("Error while initializing metrics.", e);
        }
    }

    public String getRepoLocalPath() {
        return this.repoLocalPath;
    }

    public void run() {
        AbstractStep cloneRepo = new JenkinsCloneRepository(this);
        // If we have experimental plugins, we need to add them here.
        String[] repos = RepairnatorConfig.getInstance().getExperimentalPluginRepoList();
        if(repos != null) {
            for(int i = 0; i < repos.length-1; i =+ 2) {
                cloneRepo.addNextStep(new AddExperimentalPluginRepo(this, repos[i], repos[i+1], repos[i+2]));
            }
        }
        
        cloneRepo
                .addNextStep(new BuildProject(this))
                .addNextStep(new TestProject(this))
                .addNextStep(new GatherTestInformation(this, true, new BuildShouldFail(), false))
                .addNextStep(new InitRepoToPush(this))
                .addNextStep(new ComputeClasspath(this, false))
                .addNextStep(new ComputeSourceDir(this, false, false))
                .addNextStep(new ComputeTestDir(this, false));

        for (String repairToolName : RepairnatorConfig.getInstance().getRepairTools()) {
            AbstractRepairStep repairStep = RepairToolsManager.getStepFromName(repairToolName);
            if (repairStep != null) { 
                repairStep.setProjectInspector(this);
                cloneRepo.addNextStep(repairStep);
            } else {
                logger.error("Error while getting repair step class for following name: " + repairToolName);
            }
        }

        cloneRepo.addNextStep(new CommitPatch(this, CommitType.COMMIT_REPAIR_INFO))
                .addNextStep(new CheckoutPatchedBuild(this, true))
                .addNextStep(new BuildProject(this))
                .addNextStep(new TestProject(this))
                .addNextStep(new GatherTestInformation(this, true, new BuildShouldPass(), true))
                .addNextStep(new CommitPatch(this, CommitType.COMMIT_HUMAN_PATCH));


        this.finalStep = new ComputeSourceDir(this, false, true); // this step is used to compute code metrics on the project


        this.finalStep.
                addNextStep(new ComputeModules(this, false)).
                addNextStep(new WritePropertyFile(this)).
                addNextStep(new CommitProcessEnd(this)).
                addNextStep(new PushProcessEnd(this));

        cloneRepo.setDataSerializer(this.serializers);
        cloneRepo.setNotifiers(this.notifiers);

        this.printPipeline();

        try {
            cloneRepo.execute();
        } catch (Exception e) {
            this.jobStatus.addStepError("Unknown", e.getMessage());
            this.logger.error("Exception catch while executing steps: ", e);
            this.jobStatus.setFatalError(e);

            ErrorNotifier errorNotifier = ErrorNotifier.getInstance();
            if (errorNotifier != null) {
                errorNotifier.observe(this);
            }

            for (AbstractDataSerializer serializer : this.serializers) {
                serializer.serialize();
            }
        }
    }


}
