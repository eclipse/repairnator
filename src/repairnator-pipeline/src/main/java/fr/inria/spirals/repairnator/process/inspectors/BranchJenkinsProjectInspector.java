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
import fr.inria.spirals.repairnator.process.step.BranchJenkinsCloneRepository;
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

import fr.inria.spirals.repairnator.process.step.push.GitRepositoryCommitPatch;
import fr.inria.spirals.repairnator.process.step.push.GitRepositoryCommitProcessEnd;
import fr.inria.spirals.repairnator.process.step.push.GitRepositoryInitRepoToPush;
import fr.inria.spirals.repairnator.process.step.push.GitRepositoryPushProcessEnd;


/**
 * This class initialize the pipelines by creating the steps:
 * it's the backbone of the pipeline.
 */
public class BranchJenkinsProjectInspector extends GitRepositoryProjectInspector {
    private final Logger logger = LoggerFactory.getLogger(ProjectInspector.class);

    private String gitRepositoryUrl;
    private String gitRepositoryBranch;
    private String gitRepositoryIdCommit;
    private boolean gitRepositoryFirstCommit;

    public BranchJenkinsProjectInspector(String gitRepoUrl, String gitRepoBranch, String gitRepoIdCommit, boolean isGitRepositoryFirstCommit,
            String workspace, List<AbstractDataSerializer> serializers, List<AbstractNotifier> notifiers) {
        
        super(gitRepoUrl,gitRepoBranch,gitRepoIdCommit,isGitRepositoryFirstCommit,workspace,serializers,notifiers);
        this.gitRepositoryUrl = gitRepoUrl;
        this.gitRepositoryBranch = gitRepoBranch;
        this.gitRepositoryIdCommit = gitRepoIdCommit;
        this.gitRepositoryFirstCommit = isGitRepositoryFirstCommit;

        this.gitSlug = this.gitRepositoryUrl.split("https://github.com/",2)[1].replace("/", "-");
        this.workspace = workspace;
        this.repoLocalPath = workspace + File.separator + getProjectIdToBeInspected();

        this.repoToPushLocalPath = repoLocalPath+"_topush";
        this.m2LocalPath = new File(this.repoLocalPath + File.separator + ".m2").getAbsolutePath();
        this.serializers = serializers;
        this.gitHelper = new GitHelper();
        this.jobStatus = new JobStatus(repoLocalPath);
        this.notifiers = notifiers;
        this.checkoutType = CheckoutType.NO_CHECKOUT;
        this.steps = new ArrayList<>();
    }

    /*@Override
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

    public String getCheckoutBranchName() {
        return this.gitBranch;
    }

    public String getGitCommit() {
        return this.gitCommit;
    }*/


    /*@Override
    protected void initProperties() {
        try {
            Properties properties = this.jobStatus.getProperties();

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
    }*/

   /* public void run() {
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
    }*/


    @Override
    public void run() {
        if (getGitRepositoryUrl() != null) {
            AbstractStep cloneRepo = new BranchJenkinsCloneRepository(this);
            
            // If we have experimental plugins, we need to add them here.
            String[] repos = RepairnatorConfig.getInstance().getExperimentalPluginRepoList();
            if(repos != null) {
                for(int i = 0; i < repos.length-1; i =+ 2) {
                    cloneRepo.addNextStep(new AddExperimentalPluginRepo(this, repos[i], repos[i+1], repos[i+2]));
                }
            }
            // Add the next steps
            cloneRepo
                    .addNextStep(new BuildProject(this))
                    .addNextStep(new TestProject(this))
                    .addNextStep(new GatherTestInformation(this, true, new BuildShouldFail(), false))
                    .addNextStep(new GitRepositoryInitRepoToPush(this))
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

            cloneRepo.addNextStep(new GitRepositoryCommitPatch(this, CommitType.COMMIT_REPAIR_INFO))
                    .addNextStep(new CheckoutPatchedBuild(this, true))
                    .addNextStep(new BuildProject(this))
                    .addNextStep(new TestProject(this))
                    .addNextStep(new GatherTestInformation(this, true, new BuildShouldPass(), true))
                    .addNextStep(new GitRepositoryCommitPatch(this, CommitType.COMMIT_HUMAN_PATCH));

            this.finalStep = new ComputeSourceDir(this, false, true); // this step is used to compute code metrics on the project
            
            this.finalStep.
                    addNextStep(new ComputeModules(this, false)).
                    addNextStep(new WritePropertyFile(this)).
                    addNextStep(new GitRepositoryCommitProcessEnd(this)).
                    addNextStep(new GitRepositoryPushProcessEnd(this));

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
        } else {
            this.logger.debug("Build " + this.getBuggyBuild().getId() + " is not a failing build.");
        }
    }

}
