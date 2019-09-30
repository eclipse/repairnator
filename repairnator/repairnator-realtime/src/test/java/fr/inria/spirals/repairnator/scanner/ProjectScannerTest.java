package fr.inria.spirals.repairnator.scanner;

import fr.inria.jtravis.entities.Build;
import fr.inria.jtravis.entities.StateType;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.utils.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProjectScannerTest {

    @Test
    public void testTestBuildWithFailingWithoutFailingTestAndRepairMode() {
        long buildId = 230022061; // inria/spoon failing from PR without failing tests
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setLauncherMode(LauncherMode.REPAIR);

        Optional<Build> buildOptional = config.getJTravis().build().fromId(buildId);
        assertTrue(buildOptional.isPresent());

        Build build = buildOptional.get();
        ProjectScanner projectScanner = new ProjectScanner(new Date(), new Date(), "test");
        boolean isOKForRepair = projectScanner.testBuild(build);

        assertFalse(isOKForRepair);
    }

    @Test
    public void testTestBuildWithFailingTestAndRepairMode() {
        long buildId = 364156914; // inria/spoon failing from PR with failing tests
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setLauncherMode(LauncherMode.REPAIR);

        Optional<Build> buildOptional = config.getJTravis().build().fromId(buildId);
        assertTrue(buildOptional.isPresent());

        Build build = buildOptional.get();
        ProjectScanner projectScanner = new ProjectScanner(new Date(), new Date(), "test");
        boolean isOKForRepair = projectScanner.testBuild(build);

        assertTrue(isOKForRepair);
    }

    @Test
    public void testTestBuildWithFailingAndBearsMode() {
        long buildId = 230022061; // inria/spoon failing from PR without failing tests
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setLauncherMode(LauncherMode.BEARS);

        Optional<Build> buildOptional = config.getJTravis().build().fromId(buildId);
        assertTrue(buildOptional.isPresent());

        Build build = buildOptional.get();
        ProjectScanner projectScanner = new ProjectScanner(new Date(), new Date(), "test");
        boolean isOKForBears = projectScanner.testBuild(build);

        assertFalse(isOKForBears);
    }

    @Test
    public void testTestBuildWithPassingAndBearsMode() {
        long buildId = 230049446; // inria/spoon passing from PR
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setLauncherMode(LauncherMode.BEARS);

        Optional<Build> buildOptional = config.getJTravis().build().fromId(buildId);
        assertTrue(buildOptional.isPresent());

        Build build = buildOptional.get();
        ProjectScanner projectScanner = new ProjectScanner(new Date(), new Date(), "test");
        boolean isOKForBears = projectScanner.testBuild(build);

        assertTrue(isOKForBears);
    }

    @Test
    public void testGetBuildToBeInspectedWithFailingFromPR() {
        long buildId = 364156914; // inria/spoon failing from PR with failing tests
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setLauncherMode(LauncherMode.REPAIR);

        Optional<Build> buildOptional = config.getJTravis().build().fromId(buildId);
        assertTrue(buildOptional.isPresent());

        Build build = buildOptional.get();
        assertEquals(StateType.FAILED, build.getState());
        ProjectScanner projectScanner = new ProjectScanner(new Date(), new Date(), "test");

        BuildToBeInspected expectedBuildToBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "test");
        BuildToBeInspected obtainedBTB = projectScanner.getBuildToBeInspected(build);
        assertEquals(expectedBuildToBeInspected, obtainedBTB);
    }

    @Test
    public void testGetBuildToBeInspectedWithPassingWithPreviousFailingFromPR() {
        long buildIdFailing = 324127095; // inria/spoon
        long buildIdNextPassing = 324525330;
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setLauncherMode(LauncherMode.BEARS);

        Optional<Build> buildOptional = config.getJTravis().build().fromId(buildIdFailing);
        assertTrue(buildOptional.isPresent());

        Build buildFailing = buildOptional.get();
        assertEquals(StateType.FAILED, buildFailing.getState());

        buildOptional = config.getJTravis().build().fromId(buildIdNextPassing);
        assertTrue(buildOptional.isPresent());

        Build buildNextPassing = buildOptional.get();
        assertEquals(StateType.PASSED, buildNextPassing.getState());
        ProjectScanner projectScanner = new ProjectScanner(new Date(), new Date(), "test");

        BuildToBeInspected expectedBuildToBeInspected = new BuildToBeInspected(buildFailing, buildNextPassing, ScannedBuildStatus.FAILING_AND_PASSING, "test");
        BuildToBeInspected obtainedBTB = projectScanner.getBuildToBeInspected(buildNextPassing);
        assertEquals(expectedBuildToBeInspected.getBuggyBuild().getId(), obtainedBTB.getBuggyBuild().getId());
        assertEquals(expectedBuildToBeInspected.getPatchedBuild().getId(), obtainedBTB.getPatchedBuild().getId());
    }

    @Test
    public void testGetBuildToBeInspectedWithPassingWithPreviousPassingFromPR() {
        long buildIdPassing = 210093951; // inria/spoon
        long buildIdNextPassing = 211479830;
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setLauncherMode(LauncherMode.BEARS);

        Optional<Build> buildOptional = config.getJTravis().build().fromId(buildIdPassing);
        assertTrue(buildOptional.isPresent());

        Build buildPassing = buildOptional.get();
        assertEquals(StateType.PASSED, buildPassing.getState());

        buildOptional = config.getJTravis().build().fromId(buildIdNextPassing);
        assertTrue(buildOptional.isPresent());

        Build buildNextPassing = buildOptional.get();
        assertEquals(StateType.PASSED, buildNextPassing.getState());
        ProjectScanner projectScanner = new ProjectScanner(new Date(), new Date(), "test");

        BuildToBeInspected expectedBuildToBeInspected = new BuildToBeInspected(buildPassing, buildNextPassing, ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES, "test");
        BuildToBeInspected obtainedBTB = projectScanner.getBuildToBeInspected(buildNextPassing);
        assertEquals(expectedBuildToBeInspected.getBuggyBuild().getId(), obtainedBTB.getBuggyBuild().getId());
    }

    /*
        The pair of builds is failing and passing.
        This pair should not be selected by the scanner, i.e. a BuildToBeInspected should not be created,
        because the failing build did not fail in Travis due to test failure.
     */
    @Test
    public void testGetBuildToBeInspectedWithPassingBuildWithPreviousFailingBuildBears() {
        long buildIdFailing = 230022061; // inria/spoon
        long buildIdNextPassing = 230049446;
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setLauncherMode(LauncherMode.BEARS);

        Optional<Build> buildOptional = config.getJTravis().build().fromId(buildIdFailing);
        assertTrue(buildOptional.isPresent());
        Build buildFailing = buildOptional.get();
        assertEquals(StateType.FAILED, buildFailing.getState());

        buildOptional = config.getJTravis().build().fromId(buildIdNextPassing);
        assertTrue(buildOptional.isPresent());
        Build buildNextPassing = buildOptional.get();
        assertEquals(StateType.PASSED, buildNextPassing.getState());

        ProjectScanner projectScanner = new ProjectScanner(new Date(), new Date(), "test");

        BuildToBeInspected obtainedBTB = projectScanner.getBuildToBeInspected(buildNextPassing);
        assertEquals(null, obtainedBTB);
    }

    /*
        The pair of builds is passing and passing.
        This pair should not be selected by the scanner, i.e. a BuildToBeInspected should not be created,
        because there is no java file changed between the two builds (there is only test file).
     */
    @Test
    public void testGetBuildToBeInspectedWithPassingBuildWithPreviousPassingBuildBears() {
        long buildIdPassing = 323802157; // inria/spoon
        long buildIdNextPassing = 323823511;
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setLauncherMode(LauncherMode.BEARS);

        Optional<Build> buildOptional = config.getJTravis().build().fromId(buildIdPassing);
        assertTrue(buildOptional.isPresent());
        Build buildPassing = buildOptional.get();
        assertEquals(StateType.PASSED, buildPassing.getState());

        buildOptional = config.getJTravis().build().fromId(buildIdNextPassing);
        assertTrue(buildOptional.isPresent());
        Build buildNextPassing = buildOptional.get();
        assertEquals(StateType.PASSED, buildNextPassing.getState());

        ProjectScanner projectScanner = new ProjectScanner(new Date(), new Date(), "test");

        BuildToBeInspected obtainedBTB = projectScanner.getBuildToBeInspected(buildNextPassing);
        assertEquals(null, obtainedBTB);
    }
}
