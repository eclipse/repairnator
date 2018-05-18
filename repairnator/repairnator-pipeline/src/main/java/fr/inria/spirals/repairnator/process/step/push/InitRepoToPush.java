package fr.inria.spirals.repairnator.process.step.push;

import fr.inria.spirals.repairnator.process.git.GitHelper;
import fr.inria.spirals.repairnator.process.inspectors.Metrics;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.states.PushState;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by urli on 26/04/2017.
 */
public class InitRepoToPush extends AbstractStep {

    private static final String TRAVIS_FILE = ".travis.yml";

    public InitRepoToPush(ProjectInspector inspector) {
        super(inspector, false);
    }

    private void removeNotificationFromTravisYML(File directory) {
        File travisFile = new File(directory, TRAVIS_FILE);

        if (!travisFile.exists()) {
            getLogger().warn("Travis file has not been detected. It should however exists.");
        } else {
            try {
                List<String> lines = Files.readAllLines(travisFile.toPath());
                List<String> newLines = new ArrayList<>();
                boolean changed = false;
                boolean inNotifBlock = false;

                for (String line : lines) {
                    if (line.trim().equals("notifications:")) {
                        changed = true;
                        inNotifBlock = true;
                    }
                    if (inNotifBlock) {
                        if (line.trim().isEmpty()) {
                            inNotifBlock = false;
                            newLines.add(line);
                        } else {
                            newLines.add("#"+line);
                        }
                    } else {
                        newLines.add(line);
                    }
                }

                if (changed) {
                    getLogger().info("Notification block detected. The travis file will be changed.");
                    File bakTravis = new File(directory, "bak"+TRAVIS_FILE);
                    Files.move(travisFile.toPath(), bakTravis.toPath());
                    FileWriter fw = new FileWriter(travisFile);
                    for (String line : newLines) {
                        fw.append(line);
                        fw.append("\n");
                        fw.flush();
                    }
                    fw.close();

                    this.getInspector().getJobStatus().getCreatedFilesToPush().add(".travis.yml");
                    this.getInspector().getJobStatus().getCreatedFilesToPush().add("bak.travis.yml");
                }
            } catch (IOException e) {
                getLogger().warn("Error while changing travis file", e);
            }
        }
    }

    @Override
    protected StepStatus businessExecute() {

        if (this.getConfig().isPush()) {
            this.getLogger().info("Repairnator configured to push. Start init repo to push.");

            File sourceDir = new File(this.getInspector().getRepoLocalPath());
            File targetDir = new File(this.getInspector().getRepoToPushLocalPath());

            try {
                GitHelper gitHelper = this.getInspector().getGitHelper();

                String[] nonDesirableFileExtensions = { ".git", ".m2" };
                gitHelper.copyDirectory(sourceDir, targetDir, nonDesirableFileExtensions, this);

                this.removeNotificationFromTravisYML(targetDir);

                Git git = Git.init().setDirectory(targetDir).call();
                git.add().addFilepattern(".").call();

                gitHelper.gitAdd(this.getInspector().getJobStatus().getCreatedFilesToPush(), git);


                PersonIdent personIdent = new PersonIdent("Luc Esape", "luc.esape@gmail.com");
                String message = "Bug commit from the following repository "+this.getInspector().getRepoSlug()+"\n";

                Metrics metrics = this.getInspector().getJobStatus().getMetrics();
                message += "This bug commit is a reflect of source code from: "+metrics.getBugCommitUrl()+".";

                git.commit().setMessage(message)
                        .setAuthor(personIdent).setCommitter(personIdent).call();

                this.setPushState(PushState.REPO_INITIALIZED);
                return StepStatus.buildSuccess(this);
            } catch (GitAPIException e) {
                this.addStepError("Error while initializing the new git repository.", e);
                this.setPushState(PushState.REPO_NOT_INITIALIZED);
            }
            return StepStatus.buildSkipped(this, "Error while initializing the new git repository.");
        } else {
            this.getLogger().info("Repairnator configured to NOT push. Step bypassed.");
            return StepStatus.buildSkipped(this);
        }
    }
}
