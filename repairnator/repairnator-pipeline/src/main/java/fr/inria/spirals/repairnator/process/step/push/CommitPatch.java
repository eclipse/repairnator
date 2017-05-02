package fr.inria.spirals.repairnator.process.step.push;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.inspectors.Metrics;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.states.PushState;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.HunkHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by urli on 27/04/2017.
 */
public class CommitPatch extends AbstractStep {
    private boolean pushHumanPatch;

    public CommitPatch(ProjectInspector inspector, boolean pushHumanPatch) {
        super(inspector);
        this.pushHumanPatch = pushHumanPatch;
    }

    public CommitPatch(ProjectInspector inspector, String name, boolean pushHumanPatch) {
        super(inspector, name);
        this.pushHumanPatch = pushHumanPatch;
    }

    private void computePatchStats(Git git, RevCommit headRev, RevCommit commit) {
        try {
            ObjectReader reader = git.getRepository().newObjectReader();
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            oldTreeIter.reset(reader, headRev.getTree());
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            newTreeIter.reset(reader, commit.getTree());

            DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
            diffFormatter.setRepository(git.getRepository());
            diffFormatter.setContext(0);
            List<DiffEntry> entries = diffFormatter.scan(newTreeIter, oldTreeIter);

            int nbLineAdded = 0;
            int nbLineDeleted = 0;
            Set<String> changedFiles = new HashSet<>();

            for (DiffEntry entry : entries) {
                String path;
                if (entry.getChangeType() == DiffEntry.ChangeType.DELETE) {
                    path = entry.getOldPath();
                } else {
                    path = entry.getNewPath();
                }
                if (!path.contains("repairnator")) {
                    changedFiles.add(path);

                    FileHeader fileHeader = diffFormatter.toFileHeader(entry);
                    List<? extends HunkHeader> hunks = fileHeader.getHunks();
                    for (HunkHeader hunk : hunks) {
                        nbLineAdded += hunk.getOldImage().getLinesAdded();
                        nbLineDeleted += hunk.getOldImage().getLinesDeleted();
                    }
                }
            }

            Metrics metric = this.getInspector().getJobStatus().getMetrics();
            metric.setPatchAddedLines(nbLineAdded);
            metric.setPatchDeletedLines(nbLineDeleted);
            metric.setPatchChangedFiles(changedFiles.size());
        } catch (IOException e) {
            this.getLogger().error("Error while computing stat on the patch", e);
        }
    }

    @Override
    protected void businessExecute() {
        if (RepairnatorConfig.getInstance().isPush()) {
            this.getLogger().info("Start the step push patch");

            File sourceDir = new File(this.getInspector().getRepoLocalPath());
            File targetDir = new File(this.getInspector().getRepoToPushLocalPath());

            try {
                Git git = Git.open(targetDir);
                Ref oldHeadRef = git.getRepository().exactRef("HEAD");

                RevWalk revWalk = new RevWalk(git.getRepository());
                RevCommit headRev = revWalk.parseCommit(oldHeadRef.getObjectId());
                revWalk.dispose();

                FileUtils.copyDirectory(sourceDir, targetDir, new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return !pathname.toString().contains(".git") && !pathname.toString().contains(".m2");
                    }
                });

                git.add().addFilepattern(".").call();

                String commitMsg;
                if (pushHumanPatch) {
                    commitMsg = "Human patch from the following repository "+this.getInspector().getRepoSlug()+"\n";

                    Metrics metrics = this.getInspector().getJobStatus().getMetrics();
                    commitMsg += "This commit is a reflect of the following : "+metrics.getBugCommitUrl()+".";
                } else {
                    commitMsg = "Automatic repair information (optionally automatic patches).";
                }

                PersonIdent personIdent = new PersonIdent("Luc Esape", "luc.esape@gmail.com");
                RevCommit commit = git.commit().setMessage(commitMsg)
                        .setAuthor(personIdent).setCommitter(personIdent).call();

                this.computePatchStats(git, headRev, commit);

                if (this.getInspector().getJobStatus().isHasBeenPushed()) {
                    CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(System.getenv("GITHUB_OAUTH"), "");
                    git.push().setRemote(PushIncriminatedBuild.REMOTE_NAME).setCredentialsProvider(credentialsProvider).call();
                }

                if (pushHumanPatch) {
                    this.setPushState(PushState.PATCH_COMMITTED);
                } else {
                    this.setPushState(PushState.REPAIR_INFO_COMMITTED);
                }
                return;
            } catch (IOException e) {
                this.addStepError("Error while copying the folder to prepare the git repository.", e);
            } catch (GitAPIException e) {
                this.addStepError("Error while opening the git repository, maybe it has not been initialized yet.", e);
            }
            if (pushHumanPatch) {
                this.setPushState(PushState.PATCH_COMMITTED);
            } else {
                this.setPushState(PushState.REPAIR_INFO_COMMITTED);
            }
        } else {
            this.getLogger().info("Repairnator is configured to not push anything. This step will be bypassed.");
        }
    }
}
