package fr.inria.spirals.repairnator.scanner;

import ch.qos.logback.classic.Level;
import fr.inria.jtravis.entities.Build;
import fr.inria.jtravis.entities.StateType;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.Utils;
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

    @Before
    public void setUp() {
        Utils.setLoggersLevel(Level.DEBUG);
    }

    @Test
    public void testTestBuildWithFailingWithoutFailingTestAndRepairMode() {
        int buildId = 230022061; // inria/spoon failing from PR without failing tests
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
        int buildId = 364156914; // inria/spoon failing from PR with failing tests
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
        int buildId = 230022061; // inria/spoon failing from PR without failing tests
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
        int buildId = 230049446; // inria/spoon passing from PR
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
        int buildId = 364156914; // inria/spoon failing from PR with failing tests
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
        int buildIdFailing = 230022061; // inria/spoon
        int buildIdNextPassing = 230049446;
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
        assertEquals(expectedBuildToBeInspected, obtainedBTB);
    }

    @Test
    public void testGetBuildToBeInspectedWithPassingWithPreviousPassingFromPR() {
        int buildIdPassing = 210093951; // inria/spoon
        int buildIdNextPassing = 211479830;
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
        assertEquals(expectedBuildToBeInspected, obtainedBTB);
    }
}
