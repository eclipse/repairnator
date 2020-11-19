package fr.inria.spirals.repairnator.process.step.repair.sequencer.detection;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.UUID;
import org.slf4j.Logger;

public class MavenPatchTester {

    ProjectInspector inspector;
    String pom;
    Logger logger;

    MavenPatchTester(ProjectInspector inspector, String pom, Logger logger){
        this.inspector = inspector;
        this.pom = pom;
        this.logger = logger;
    }

    public boolean apply(RepairPatch patch, String goal, Properties properties){

        //Create replica as git branch and apply patch
        logger.info("testing patch: " + patch.getFilePath());
        String patchName = Paths.get(patch.getFilePath()).getFileName().toString() + "-" + UUID.randomUUID();
        boolean success = false;
        try {
            Git git = Git.open(new File(inspector.getRepoLocalPath()));
            String defaultBranch = git.getRepository().getBranch();

            git.branchCreate().setStartPoint(defaultBranch).setName(patchName).call();
            git.checkout().setName(patchName).call();

            InputStream is = new ByteArrayInputStream(patch.getDiff().getBytes());
            git.apply().setPatch(is).call();

            //Build and test with applied patch
            MavenHelper maven = new MavenHelper(pom, goal, properties, "sequencer-builder", inspector, true);

            int result  = maven.run();

            git.reset().setMode(ResetCommand.ResetType.HARD).call();

            git.checkout().setName(defaultBranch).call();
            git.branchDelete().setBranchNames(patchName).call();

            return result == MavenHelper.MAVEN_SUCCESS;

        } catch (Exception e) {
            logger.error("error while testing if patch is buildable:" + e) ;
            return false;
        }
    }
}
