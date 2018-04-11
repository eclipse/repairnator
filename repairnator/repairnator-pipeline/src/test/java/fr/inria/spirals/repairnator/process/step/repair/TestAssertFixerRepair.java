package fr.inria.spirals.repairnator.process.step.repair;

import ch.qos.logback.classic.Level;
import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.CloneRepository;
import fr.inria.spirals.repairnator.process.step.TestProject;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuild;
import fr.inria.spirals.repairnator.process.step.gatherinfo.BuildShouldFail;
import fr.inria.spirals.repairnator.process.step.gatherinfo.GatherTestInformation;
import fr.inria.spirals.repairnator.process.step.pathes.ComputeClasspath;
import fr.inria.spirals.repairnator.process.step.pathes.ComputeSourceDir;
import fr.inria.spirals.repairnator.process.step.pathes.ComputeTestDir;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestAssertFixerRepair {

    @Before
    public void setup() {
        Utils.setLoggersLevel(Level.DEBUG);
    }

    @Test
    public void testAssertFixerFixes() throws IOException {
        int buildId = 365127838; // surli/failingProject build

        Optional<Build> optionalBuild = RepairnatorConfig.getInstance().getJTravis().build().fromId(buildId);
        assertTrue(optionalBuild.isPresent());
        Build build = optionalBuild.get();
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_assertfixer");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

        ProjectInspector inspector = new ProjectInspector(toBeInspected, tmpDir.getAbsolutePath(), null, null);

        CloneRepository cloneStep = new CloneRepository(inspector);
        AssertFixerRepair assertFixerRepair = new AssertFixerRepair(inspector);

        cloneStep.setNextStep(new CheckoutBuggyBuild(inspector))
                .setNextStep(new TestProject(inspector))
                .setNextStep(new GatherTestInformation(inspector, new BuildShouldFail(), false))
                .setNextStep(new ComputeClasspath(inspector))
                .setNextStep(new ComputeSourceDir(inspector, false))
                .setNextStep(new ComputeTestDir(inspector))
                .setNextStep(assertFixerRepair);
        cloneStep.execute();

        assertThat(assertFixerRepair.isShouldStop(), is(false));
        assertThat(inspector.getJobStatus().getAssertFixerResults().size(), is(13));
    }
}
