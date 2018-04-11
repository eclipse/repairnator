package fr.inria.spirals.repairnator.scanner;

import ch.qos.logback.classic.Level;
import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.states.LauncherMode;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.Optional;

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
        boolean isOKForRepair = projectScanner.testBuild(build);

        assertFalse(isOKForRepair);
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
        boolean isOKForRepair = projectScanner.testBuild(build);

        assertTrue(isOKForRepair);
    }
}
