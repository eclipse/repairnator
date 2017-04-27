package fr.inria.spirals.repairnator.process.step.push;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.states.PushState;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

/**
 * Created by urli on 27/04/2017.
 */
public class PushPatch extends AbstractStep {
    private boolean pushHumanPatch;

    public PushPatch(ProjectInspector inspector, boolean pushHumanPatch) {
        super(inspector);
        this.pushHumanPatch = pushHumanPatch;
    }

    public PushPatch(ProjectInspector inspector, String name, boolean pushHumanPatch) {
        super(inspector, name);
        this.pushHumanPatch = pushHumanPatch;
    }

    @Override
    protected void businessExecute() {
        if (RepairnatorConfig.getInstance().isPush()) {
            this.getLogger().info("Start the step push patch");

            if (this.getPushState() == PushState.REPO_INITIALIZED ||
                    this.getPushState() == PushState.REPO_PUSHED ||
                    this.getPushState() == PushState.REPAIR_INFO_PUSHED) {
                File sourceDir = new File(this.getInspector().getRepoLocalPath());
                File targetDir = new File(this.getInspector().getRepoToPushLocalPath());

                try {
                    FileUtils.copyDirectory(sourceDir, targetDir, new FileFilter() {
                        @Override
                        public boolean accept(File pathname) {
                            return !pathname.toString().contains(".git");
                        }
                    });

                    Git git = Git.open(targetDir);
                    git.add().addFilepattern(".").call();

                    String commitMsg = (pushHumanPatch) ? "Human patch for the bug." : "Automated patch and/or related information";

                    PersonIdent personIdent = new PersonIdent("Luc Esape", "luc.esape@gmail.com");
                    git.commit().setMessage(commitMsg)
                            .setAuthor(personIdent).setCommitter(personIdent).call();

                    if (this.getInspector().getJobStatus().isHasBeenPushed()) {
                        git.push().call();
                    }

                    if (pushHumanPatch) {
                        this.setPushState(PushState.PATCH_PUSHED);
                    } else {
                        this.setPushState(PushState.REPAIR_INFO_PUSHED);
                    }
                    return;
                } catch (IOException e) {
                    this.addStepError("Error while copying the folder to prepare the git repository.", e);
                } catch (GitAPIException e) {
                    this.addStepError("Error while initializing the new git repository.", e);
                }
                if (pushHumanPatch) {
                    this.setPushState(PushState.PATCH_NOT_PUSHED);
                } else {
                    this.setPushState(PushState.REPAIR_INFO_NOT_PUSHED);
                }
            } else {
                this.getLogger().warn("The distant repo has not been initialized, so nothing will be done here");
            }
        } else {
            this.getLogger().info("Repairnator is configured to not push anything. This step will be bypassed.");
        }
    }
}
