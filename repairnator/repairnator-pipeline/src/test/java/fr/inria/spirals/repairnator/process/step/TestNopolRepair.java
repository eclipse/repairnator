package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.helpers.BuildHelper;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.LauncherMode;
import fr.inria.spirals.repairnator.ProjectState;
import fr.inria.spirals.repairnator.ScannedBuildStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.gatherinfocontract.BuildShouldFail;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by urli on 07/03/2017.
 */
public class TestNopolRepair {

    private static final String SOLVER_PATH_DIR = "src/test/resources/z3/";
    private static final String SOLVER_NAME_LINUX = "z3_for_linux";
    private static final String SOLVER_NAME_MAC = "z3_for_mac";
    public static String solverPath;

    static {
        if (isMac()) {
            solverPath = SOLVER_PATH_DIR+SOLVER_NAME_MAC;
        } else {
            solverPath = SOLVER_PATH_DIR+SOLVER_NAME_LINUX;
        }
    }

    public static boolean isMac() {
        String OS = System.getProperty("os.name").toLowerCase();
        return (OS.indexOf("mac") >= 0);
    }

    @Test
    public void testNopolRepair() throws IOException {
        int buildId = 207890790; // surli/failingProject build

        Build build = BuildHelper.getBuildFromId(buildId, null);
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_nopolrepair");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL);

        ProjectInspector inspector = new ProjectInspector(toBeInspected, tmpDir.getAbsolutePath(), null, solverPath, false, LauncherMode.BUILD);

        CloneRepository cloneStep = new CloneRepository(inspector);
        NopolRepair nopolRepair = new NopolRepair(inspector);
        NopolRepair.TOTAL_MAX_TIME = 2;

        cloneStep.setNextStep(new CheckoutBuild(inspector))
                .setNextStep(new TestProject(inspector))
                .setNextStep(new GatherTestInformation(inspector, new BuildShouldFail()))
                .setNextStep(new ComputeClasspath(inspector))
                .setNextStep(new ComputeSourceDir(inspector))
                .setNextStep(nopolRepair);
        cloneStep.execute();

        assertThat(nopolRepair.shouldStop, is(false));
        assertThat(nopolRepair.getState(), is(ProjectState.PATCHED));
        assertThat(nopolRepair.getNopolInformations().size(), is(10));
    }
}
