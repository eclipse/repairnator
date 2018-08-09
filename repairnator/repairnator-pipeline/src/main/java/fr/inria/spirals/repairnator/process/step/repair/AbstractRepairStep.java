package fr.inria.spirals.repairnator.process.step.repair;

import com.google.gson.JsonElement;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.PatchNotifier;
import fr.inria.spirals.repairnator.process.git.GitHelper;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class AbstractRepairStep extends AbstractStep {

    public static final String DEFAULT_DIR_PATCHES = "repairnator-patches";
    public static final String TEXT_PR = "This PR has been created automatically by [repairnator](https://github.com/Spirals-Team/repairnator).\n" +
                                        "It aims at fixing the following Travis failing build: %s \n\n" +
                                        "If you don't want to receive those PR on the future, [open an issue on Repairnator Github repository](https://github.com/Spirals-Team/repairnator/issues/new?title=[BLACKLIST]%%20%s) with the following subject: `[BLACKLIST] %s`.";

    public static final int MAX_PATCH_PER_TOOL = 1;

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

    private List<File> serializePatches(List<RepairPatch> patchList) throws IOException {
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

    private void notify(List<RepairPatch> patches) {
        this.forkRepository();

        PatchNotifier patchNotifier = this.getInspector().getPatchNotifier();
        if (patchNotifier != null) {
            patchNotifier.notify(this.getInspector(), this.getRepairToolName(), patches);
        }
    }

    protected void recordPatches(List<RepairPatch> patchList) {
        this.getInspector().getJobStatus().addPatches(this.getRepairToolName(), patchList);

        if (!patchList.isEmpty()) {
            this.getInspector().getJobStatus().setHasBeenPatched(true);
            List<File> serializedPatches = null;
            try {
                serializedPatches = this.serializePatches(patchList);
            } catch (IOException e) {
                this.addStepError("Error while serializing patches", e);
            }

            if (serializedPatches != null) {
                try {
                    this.createPullRequest(serializedPatches, MAX_PATCH_PER_TOOL);
                } catch (IOException|GitAPIException|URISyntaxException e) {
                    this.addStepError("Error while creating the PR", e);
                }
            } else {
                this.addStepError("No file has been serialized, so no PR will be created");
            }


            this.notify(patchList);
        }
    }

    protected void createPullRequest(List<File> patchList, int nbPatch) throws IOException, GitAPIException, URISyntaxException {
        if (patchList.isEmpty()) {
            return;
        }

        if (this.getInspector().getBuggyBuild().isPullRequest()) {
            this.getLogger().warn("Skipping creating a PR if it's already a PR, for now. Much more complicated stuff to do there.");
            return;
        }

        this.forkRepository();

        if (!this.getInspector().getJobStatus().isHasBeenForked()) {
            this.getLogger().info("The project has not been forked. The PR won't be created.");
            return;
        }

        // fork repo
        String forkedRepo = this.getInspector().getJobStatus().getForkURL();

        if (forkedRepo.startsWith("https://api.github.com/repos")) {
            forkedRepo = forkedRepo.replace("https://api.github.com/repos", "https://github.com");
        }

        // we will work directly in the
        Git git = Git.open(new File(this.getInspector().getRepoLocalPath()));


        for (int i = 0; i < nbPatch && i < patchList.size(); i++) {
            File patch = patchList.get(i);

            String branchName = "repairnator-patch-" + Utils.formatFilenameDate(new Date()) + "-" + i;
            int status = GitHelper.gitCreateNewBranchAndCheckoutIt(this.getInspector().getRepoLocalPath(), branchName);
            if (status == 0) {
                ProcessBuilder processBuilder = new ProcessBuilder("git", "apply", patch.getAbsolutePath())
                        .directory(new File(this.getInspector().getRepoLocalPath())).inheritIO();

                try {
                    Process p = processBuilder.start();
                    p.waitFor();
                } catch (InterruptedException|IOException e) {
                    this.addStepError("Error while executing git command to apply patch " + patch.getPath(), e);
                }
                git.commit().setAll(true).setAuthor(GitHelper.getCommitterIdent()).setCommitter(GitHelper.getCommitterIdent()).setMessage("Proposal for a patch").call();

                RemoteAddCommand remoteAddCommand = git.remoteAdd();
                remoteAddCommand.setUri(new URIish(forkedRepo));
                remoteAddCommand.setName("fork-patch");
                remoteAddCommand.call();

                git.push().add(branchName).setRemote("fork-patch").setCredentialsProvider(new UsernamePasswordCredentialsProvider(RepairnatorConfig.getInstance().getGithubToken(), "")).call();

                GitHub github = RepairnatorConfig.getInstance().getGithub();

                GHRepository originalRepository = github.getRepository(this.getInspector().getRepoSlug());
                GHRepository ghForkedRepo = originalRepository.fork();

                String base = this.getInspector().getBuggyBuild().getBranch().getName();
                String head = ghForkedRepo.getOwnerName() + ":" + branchName;

                String travisURL = Utils.getTravisUrl(this.getInspector().getBuggyBuild().getId(), this.getInspector().getRepoSlug());

                String prText = String.format(TEXT_PR, travisURL, this.getInspector().getRepoSlug(), this.getInspector().getRepoSlug());

                GHPullRequest pullRequest = originalRepository.createPullRequest("Patch proposal", head, base, prText);
                String prURL = "https://github.com/" + this.getInspector().getRepoSlug() + "/pull/" + pullRequest.getNumber();
                this.getLogger().info("Pull request created on: " + prURL);
                this.getInspector().getJobStatus().addPRCreated(prURL);
            } else {
                this.addStepError("Error while creating a dedicated branch for the patch.");
            }
        }
    }

    protected void recordToolDiagnostic(JsonElement element) {
        this.getInspector().getJobStatus().addToolDiagnostic(this.getRepairToolName(), element);
    }

    public abstract String getRepairToolName();
}
