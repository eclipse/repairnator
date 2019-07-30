package fr.inria.spirals.repairnator.pipeline;

import ch.qos.logback.classic.Level;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.pipeline.Launcher;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.utils4tests.ProjectInspectorMocker;
import fr.inria.spirals.repairnator.utils.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by urli on 21/02/2017.
 */
public class TestPipeline {

    @Test
    public void testPipeline() throws Exception {
        // requires env variable M2_HOME and GITHUB_OAUTH
        // (set in Travis config)
        // eg export M2_HOME=/usr/share/maven
        // from surli/failingBuild
        Launcher l = new Launcher(new String[]{"--build", "564711868"});
        l.mainProcess();
        assertEquals("PATCHED", l.getInspector().getFinding());
    }
}
