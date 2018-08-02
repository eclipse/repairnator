package fr.inria.spirals.repairnator.process.step.repair;

import com.google.gson.JsonElement;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.PatchNotifier;
import fr.inria.spirals.repairnator.process.git.GitHelper;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;
import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.URIish;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.List;

public abstract class AbstractRepairStep extends AbstractStep {

    public static final String DEFAULT_DIR_PATCHES = "repairnator-patches";

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

    private void serializePatches(List<RepairPatch> patchList) throws IOException {
        File parentDirectory = new File(this.getInspector().getRepoToPushLocalPath(), DEFAULT_DIR_PATCHES);

        if (!parentDirectory.exists()) {
            parentDirectory.mkdirs();
        }

        File toolDirectory = new File(parentDirectory, this.getRepairToolName());
        toolDirectory.mkdirs();

        int i = 1;
        String dirPath = DEFAULT_DIR_PATCHES + "/" + this.getRepairToolName() + "/";
        for (RepairPatch repairPatch : patchList) {
            File patchFile = new File(toolDirectory, "patch_" + (i++) + ".patch");
            BufferedWriter bufferedWriter = Files.newBufferedWriter(patchFile.toPath());
            bufferedWriter.write(repairPatch.getDiff());
            bufferedWriter.close();
            this.getInspector().getJobStatus().addFileToPush(dirPath + patchFile.getName());
        }
    }

    private void notify(List<RepairPatch> patches) {
        PatchNotifier patchNotifier = this.getInspector().getPatchNotifier();
        if (patchNotifier != null) {
            patchNotifier.notify(this.getInspector(), this.getRepairToolName(), patches);
        }
    }

    protected void recordPatches(List<RepairPatch> patchList) {
        this.getInspector().getJobStatus().addPatches(this.getRepairToolName(), patchList);

        if (!patchList.isEmpty()) {
            this.getInspector().getJobStatus().setHasBeenPatched(true);
            try {
                this.serializePatches(patchList);
            } catch (IOException e) {
                this.addStepError("Error while serializing patches", e);
            }
            this.notify(patchList);
        }
    }

    protected void createPullRequest(List<RepairPatch> patchList, int nbPatch) throws IOException, GitAPIException, URISyntaxException {
        if (patchList.isEmpty()) {
            return;
        }

        if (this.getInspector().getBuggyBuild().isPullRequest()) {
            this.getLogger().warn("Skipping creating a PR if it's already a PR, for now. Much more complicated stuff to do there.");
            return;
        }

        // fork repo
        String forkedRepo = this.getInspector().getJobStatus().getForkURL();

        // we will work directly in the
        Git git = Git.open(new File(this.getInspector().getRepoLocalPath()));

        for (int i = 0; i < nbPatch && i < patchList.size(); i++) {
            RepairPatch patch = patchList.get(i);

            int status = GitHelper.gitCreateNewBranchAndCheckoutIt(this.getInspector().getRepoLocalPath(), "patch-" + nbPatch);
            if (status == 0) {
                ProcessBuilder processBuilder = new ProcessBuilder("git", "apply", "<", patch.getDiff())
                        .directory(new File(this.getInspector().getRepoLocalPath())).inheritIO();

                try {
                    Process p = processBuilder.start();
                    p.waitFor();
                } catch (InterruptedException|IOException e) {
                    this.getLogger().error("Error while executing git command to apply patch: " + e);
                }

                git.add().setUpdate(true).call();
                git.commit().setAuthor(GitHelper.getCommitterIdent()).setCommitter(GitHelper.getCommitterIdent()).setMessage("Proposal for a patch").call();


                RemoteAddCommand remoteAddCommand = git.remoteAdd();
                remoteAddCommand.setUri(new URIish(forkedRepo));
                remoteAddCommand.setName("fork-patch");
                remoteAddCommand.call();

                GitHub github = RepairnatorConfig.getInstance().getGithub();
                GHRepository originalRepository = github.getRepository(this.getInspector().getRepoSlug());
                GHRepository ghForkedRepo = originalRepository.fork();


                String base = this.getInspector().getBuggyBuild().getBranch().getName();
                String head = ghForkedRepo.getOwnerName() + ":fork-patch";
                GHPullRequest pullRequest = originalRepository.createPullRequest("Patch proposal", base, head, "This PR intends to provide a patch for the following failing build: " + this.getInspector().getBuggyBuild().getUri());
                this.getLogger().info("Pull request created on: " + pullRequest.getUrl());
            } else {
                this.getLogger().error("Error while creating a dedicated branch for the patch.");
            }
        }
    }

    protected void recordToolDiagnostic(JsonElement element) {
        this.getInspector().getJobStatus().addToolDiagnostic(this.getRepairToolName(), element);
    }

    public abstract String getRepairToolName();
}
