package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.helpers.BuildHelper;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuild;
import fr.inria.spirals.repairnator.process.step.gatherinfo.BuildShouldFail;
import fr.inria.spirals.repairnator.process.step.gatherinfo.GatherTestInformation;
import fr.inria.spirals.repairnator.states.PipelineState;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by urli on 11/07/2017.
 */
public class TestNPERepair {

    @Test
    public void testNPERepair() throws IOException {
        int buildId = 252712792; // surli/failingProject build

        Build build = BuildHelper.getBuildFromId(buildId, null);
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_nperepair");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();
        System.out.println("Dirpath : "+tmpDirPath);

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

        ProjectInspector inspector = new ProjectInspector(toBeInspected, tmpDir.getAbsolutePath(), null, null);

        CloneRepository cloneStep = new CloneRepository(inspector);
        NPERepair npeRepair = new NPERepair(inspector);

        cloneStep.setNextStep(new CheckoutBuggyBuild(inspector))
                .setNextStep(new TestProject(inspector))
                .setNextStep(new GatherTestInformation(inspector, new BuildShouldFail(), false))
                .setNextStep(npeRepair);
        cloneStep.execute();

        assertThat(npeRepair.shouldStop, is(false));
        assertThat(npeRepair.getPipelineState(), is(PipelineState.NPEFIX_PATCHED));
        assertThat(inspector.getJobStatus().getNpeFixPatches().size(), is(6));
    }
}
