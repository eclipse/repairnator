package fr.inria.spirals.repairnator.process.step.repair;

import com.google.gson.JsonElement;
import fr.inria.coming.codefeatures.RepairnatorFeatures;
import fr.inria.coming.core.engine.git.GITRepositoryInspector;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.PatchNotifier;
import fr.inria.spirals.repairnator.process.git.GitHelper;
import fr.inria.spirals.repairnator.process.inspectors.GitRepositoryProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.JenkinsProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.utils.DateUtils;
import fr.inria.spirals.repairnator.utils.Utils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.kohsuke.github.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractRepairStep extends AbstractStep {

    public static final String DEFAULT_DIR_PATCHES = "repairnator-patches";
    public static final InputStream DEFAULT_TEXT_FILE = AbstractRepairStep.class.getClassLoader().getResourceAsStream("R-Hero-PR-text.MD");
    public static final String FEEDBACK_URL = "http://sequencer.westeurope.cloudapp.azure.com:8081";

    public static final String GITHUB_TEXT_PR = "This patch uses the program repair tools %(tools) \n\n";

    public static final int MAX_PATCH_PER_TOOL = 1;

    public static String prTitle = "Patch automatically synthesized by R-Hero! \uD83C\uDFA9";

    private String prText;

    public AbstractRepairStep() {
        super(null, false);
    }

    public void setProjectInspector(ProjectInspector inspector) {
        super.setProjectInspector(inspector);
        this.setName(this.getRepairToolName());
    }

    @Override
    public void execute() {
        if (this.getConfig().getRepairTools().contains(this.getRepairToolName())) {
            super.execute();
        } else {
            this.getLogger().warn("Skipping repair step "+this.getRepairToolName());
            this.getInspector().getJobStatus().addStepStatus(StepStatus.buildSkipped(this,"Not configured to run."));
            super.executeNextStep();
        }
    }

    protected List<File> serializePatches(List<RepairPatch> patchList) throws IOException {
        File parentDirectory = new File(this.getInspector().getRepoToPushLocalPath(), DEFAULT_DIR_PATCHES);
        if (!parentDirectory.exists()) {
            parentDirectory.mkdirs();
        }

        File toolDirectory = new File(parentDirectory, this.getRepairToolName());
        toolDirectory.mkdirs();

        List<File> serializedPatches = new ArrayList<>();
        int i = 1;
        String dirPath = DEFAULT_DIR_PATCHES + "/" + this.getRepairToolName() + "/";
        for (RepairPatch repairPatch : patchList) {
            File patchFile = new File(toolDirectory, "patch_" + (i++) + ".patch");
            BufferedWriter bufferedWriter = Files.newBufferedWriter(patchFile.toPath());
            bufferedWriter.write(repairPatch.getDiff());
            bufferedWriter.close();
            this.getInspector().getJobStatus().addFileToPush(dirPath + patchFile.getName());
            serializedPatches.add(patchFile);
        }

        return serializedPatches;
    }

    protected void notify(List<RepairPatch> patches) {
        this.forkRepository();

        PatchNotifier patchNotifier = this.getInspector().getPatchNotifier();
        if (patchNotifier != null) {
            patchNotifier.notify(this.getInspector(), this.getRepairToolName(), patches);
        }
    }

    protected void setPrText(String prText) {
        this.prText = prText;
    }

    protected void setPRTitle(String prTitle) {
        this.prTitle = prTitle;
    }

    private List<RepairPatch> classifyPatches(List<RepairPatch> patches) {
        RepairnatorConfig.PATCH_CLASSIFICATION_MODE mode = RepairnatorConfig.getInstance().getPatchClassificationMode();
        this.getLogger().info("Classifying patches with mode " + mode.name());

        if (patches.isEmpty()) {
            return patches;
        } else if (mode.equals(RepairnatorConfig.PATCH_CLASSIFICATION_MODE.ODS)) {
            if (this.getInspector() instanceof GitRepositoryProjectInspector) {
                GitRepositoryProjectInspector gitInspector = (GitRepositoryProjectInspector) this.getInspector();
                patches = RepairPatch.classifyByODSWithFeatures(
                        patches,
                        (gitInspector.getRepoSlug() + "-" + gitInspector.getGitRepositoryIdCommit()).replace("/", "-")
                );
            } else {
                patches = RepairPatch.classifyByODSWithFeatures(patches, this.getInspector().getBuildToBeInspected().getRunId());
            }
        } else if (mode.equals(RepairnatorConfig.PATCH_CLASSIFICATION_MODE.NONE)) {
            this.getLogger().info("Classification mode is NONE so no patch will be classified");
        }

        patches.forEach(patch -> this.getLogger().debug("patch: " + patch.getFilePath() + " " + patch.getODSLabel()));
        return patches;
    }

    private List<RepairPatch> rankPatches(List<RepairPatch> patches) {
        RepairnatorConfig.PATCH_RANKING_MODE mode = RepairnatorConfig.getInstance().getPatchRankingMode();
        this.getLogger().info("Ranking patches with mode " + mode.name());

        if (patches.isEmpty()) {
            return patches;
        } else if (mode.equals(RepairnatorConfig.PATCH_RANKING_MODE.OVERFITTING)) {
            this.getLogger().info("Ranking by overfitting score is not supported yet");
        } else if (mode.equals(RepairnatorConfig.PATCH_RANKING_MODE.NONE)) {
            this.getLogger().info("Ranking mode is NONE so no patch will be ranked");
        }

        return patches;
    }

    private List<RepairPatch> filterPatches(List<RepairPatch> patches) {
        RepairnatorConfig.PATCH_FILTERING_MODE mode = RepairnatorConfig.getInstance().getPatchFilteringMode();
        this.getLogger().info("Filtering " + patches.size() + " patches with mode " + mode.name());

        if (patches.isEmpty()) {
            return patches;
        } else if (mode.equals(RepairnatorConfig.PATCH_FILTERING_MODE.ODS_CORRECT)) {
            patches = patches.stream()
                    .filter(patch -> patch.getODSLabel().equals(RepairnatorFeatures.ODSLabel.CORRECT))
                    .collect(Collectors.toList());
        } else if (mode.equals(RepairnatorConfig.PATCH_FILTERING_MODE.NONE)) {
            this.getLogger().info("Filtering mode is NONE so no patch will be filtered");
        }

        this.getLogger().info("Number of patches after filtering: " + patches.size());
        return patches;
    }

    protected List<RepairPatch> performPatchAnalysis(List<RepairPatch> patchList) {
        if (patchList.isEmpty()) {
            return patchList;
        }
        if (RepairnatorConfig.getInstance().isPatchClassification()) {
            patchList = this.classifyPatches(patchList);
        }
        if (RepairnatorConfig.getInstance().isPatchRanking()) {
            patchList = this.rankPatches(patchList);
        }
        if (RepairnatorConfig.getInstance().isPatchFiltering()) {
            patchList = this.filterPatches(patchList);
        }

        return patchList;
    }

    protected void recordPatches(List<RepairPatch> patchList,int patchNbsLimit) {
        this.getInspector().getJobStatus().addPatches(this.getRepairToolName(), patchList);

        if (!patchList.isEmpty()) {
            this.getInspector().getJobStatus().setHasBeenPatched(true);
            List<File> serializedPatches = null;
            try {
                serializedPatches = this.serializePatches(patchList);
            } catch (IOException e) {
                this.addStepError("Error while serializing patches", e);
            }
            if (this.getConfig().isCreatePR()) {
                if (serializedPatches != null) {
                    try {
                        this.performStandardPRCreation(serializedPatches,patchNbsLimit);
                    } catch (IOException | GitAPIException | URISyntaxException e) {
                        this.addStepError("Error while creating the PR", e);
                    }
                } else {
                    this.addStepError("No file has been serialized, so no PR will be created");
                }
            }
            this.notify(patchList);
        }
    }

    protected void performStandardPRCreation(List<File> patchList,int nbPatch) throws IOException, GitAPIException, URISyntaxException {
        String newBranch = "repairnator-patch-" + DateUtils.formatFilenameDate(new Date());
        Git branchedGit = this.createGitBranch4Push(newBranch);
        String forkedRepo = this.getForkedRepoName();
        this.applyPatches(branchedGit,patchList,nbPatch);
        this.pushPatches(branchedGit,forkedRepo,newBranch);
        GitRepositoryProjectInspector ins = (GitRepositoryProjectInspector)this.getInspector();

        GitHub github = new GitHubBuilder().withOAuthToken(RepairnatorConfig.getInstance().getGithubToken()).build();
        GHRepository repo = github.getRepository(getInspector().getRepoSlug());

        Map<String, GHBranch> branches = repo.getBranches();

        Optional<String> branch = branches
                .keySet().stream()
                .filter(key -> branches.get(key).getSHA1().equals(ins.getGitRepositoryIdCommit()))
                .findFirst();

        if(branch.isPresent()){
            this.createPullRequest(branch.get(), newBranch);
        }else{
            getLogger().info("Could not create pull request");
        }
    }

    protected void applyPatches(Git git,List<File> patchList,int nbPatch) throws IOException, GitAPIException, URISyntaxException {
        for (int i = 0; i < nbPatch && i < patchList.size(); i++) {
            File patch = patchList.get(i);
            ProcessBuilder processBuilder = new ProcessBuilder("git", "apply", patch.getAbsolutePath())
                        .directory(new File(this.getInspector().getRepoLocalPath())).inheritIO();

            try {
                Process p = processBuilder.start();
                p.waitFor();
            } catch (InterruptedException|IOException e) {
                this.addStepError("Error while executing git command to apply patch " + patch.getPath(), e);
            }
            git.commit().setAll(true).setAuthor(GitHelper.getCommitterIdent()).setCommitter(GitHelper.getCommitterIdent()).setMessage("Proposal for a patch").call();
        }
    }

    protected void pushPatches(Git git, String forkedRepo,String branchName) throws IOException, GitAPIException, URISyntaxException {
        RemoteAddCommand remoteAddCommand = git.remoteAdd();
        remoteAddCommand.setUri(new URIish(forkedRepo));
        remoteAddCommand.setName("fork-patch");
        remoteAddCommand.call();

        git.push().add(branchName).setRemote("fork-patch").setCredentialsProvider(
                new UsernamePasswordCredentialsProvider(RepairnatorConfig.getInstance().getGithubToken(),"")
        ).call();
    }

    protected Git createGitBranch4Push(String branchName) throws IOException{
        Git git = Git.open(new File(this.getInspector().getRepoLocalPath()));
        int status = GitHelper.gitCreateNewBranchAndCheckoutIt(this.getInspector().getRepoLocalPath(), branchName);

        if (status != 0)  {
            return null;
        }
        return git;
    }

    protected String getForkedRepoName() {
        this.forkRepository();

        if (!this.getInspector().getJobStatus().isHasBeenForked()) {
            this.getLogger().info("The project has not been forked. The PR won't be created.");
            return null;
        }

        // fork repo
        String forkedRepo = this.getInspector().getJobStatus().getForkURL();
        if (forkedRepo.startsWith("https://api.github.com/repos")) {
            forkedRepo = forkedRepo.replace("https://api.github.com/repos", "https://github.com");
        }

        return forkedRepo;
    }


    protected void createPullRequest(String baseBranch,String newBranch) throws IOException, GitAPIException, URISyntaxException {
        GitHub github = RepairnatorConfig.getInstance().getGithub();

        GHRepository originalRepository = github.getRepository(this.getInspector().getRepoSlug());
        GHRepository ghForkedRepo = originalRepository.fork();

        String base = baseBranch;
        String head = ghForkedRepo.getOwnerName() + ":" + newBranch;

        System.out.println("base: " + base + " head:" + head);
        long buildID = this.getInspector().getBuggyBuild() == null ? 0 : this.getInspector().getBuggyBuild().getId();

        GitRepositoryProjectInspector ins = (GitRepositoryProjectInspector)getInspector();

        String commitURL = "https://github.com/" + ins.getRepoSlug() + "/commit/" + ins.getGitRepositoryIdCommit();
        Map<String, String> values = new HashMap<String, String>();
        values.put("githubCommit", commitURL);
        values.put("tools", String.join(",", this.getConfig().getRepairTools()));
        values.put("slug", this.getInspector().getRepoSlug());

        String feedbackURL =
                FEEDBACK_URL + "/" +
                this.getInspector().getRepoSlug() + "/" +
                buildID;

        values.put("helpfulURL", feedbackURL + "/0");
        values.put("incorrectURL", feedbackURL + "/1");
        values.put("uselessURL", feedbackURL + "/2");

        if (prText == null) {
            StrSubstitutor sub = new StrSubstitutor(values, "%(", ")");
            this.prText = sub.replace(IOUtils.toString(DEFAULT_TEXT_FILE, StandardCharsets.UTF_8));
        }

        GHPullRequest pullRequest = originalRepository.createPullRequest(prTitle, head, base, this.prText);
        String prURL = "https://github.com/" + this.getInspector().getRepoSlug() + "/pull/" + pullRequest.getNumber();
        this.getLogger().info("Pull request created on: " + prURL);
        this.getInspector().getJobStatus().addPRCreated(prURL);
    }

    protected void recordToolDiagnostic(JsonElement element) {
        this.getInspector().getJobStatus().addToolDiagnostic(this.getRepairToolName(), element);
    }

    public abstract String getRepairToolName();
}
