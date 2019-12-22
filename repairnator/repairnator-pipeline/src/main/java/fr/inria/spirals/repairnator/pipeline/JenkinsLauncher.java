package fr.inria.spirals.repairnator.pipeline;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.serializer.HardwareInfoSerializer;
import fr.inria.spirals.repairnator.serializer.InspectorSerializer;
import fr.inria.spirals.repairnator.serializer.InspectorSerializer4Bears;
import fr.inria.spirals.repairnator.serializer.InspectorTimeSerializer;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.serializer.InspectorSerializer;
import fr.inria.spirals.repairnator.serializer.PatchesSerializer;
import fr.inria.spirals.repairnator.serializer.PipelineErrorSerializer;
import fr.inria.spirals.repairnator.serializer.PropertiesSerializer;
import fr.inria.spirals.repairnator.serializer.ToolDiagnosticSerializer;
import fr.inria.spirals.repairnator.serializer.PullRequestSerializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.HashSet;
import java.util.Arrays;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import java.io.File;
import java.io.IOException;
import com.google.common.io.Files;

/* Entry point as Jenkins plugin - skip JSAP */
public class JenkinsLauncher extends Launcher {
    private static Logger LOGGER = LoggerFactory.getLogger(JenkinsLauncher.class);
    private final File tempDir = Files.createTempDir();
    private static RepairnatorConfig getConfig() {
      return RepairnatorConfig.getInstance();
  }

    public JenkinsLauncher() {}

  public boolean mainProcess() {
    LOGGER.info("Start by getting the build (buildId: "+this.getConfig().getBuildId()+") with the following config: "+this.getConfig());
    if (!this.getBuildToBeInspected()) {
        return false;
    }

    HardwareInfoSerializer hardwareInfoSerializer = new HardwareInfoSerializer(this.engines, this.getConfig().getRunId(), this.getConfig().getBuildId()+"");
    hardwareInfoSerializer.serialize();

    List<AbstractDataSerializer> serializers = new ArrayList<>();

    /* instantiate with temp dir instead */

    inspector = new ProjectInspector(buildToBeInspected,this.tempDir.getAbsolutePath(),serializers, this.notifiers);

    serializers.add(new InspectorSerializer(this.engines, inspector));
    serializers.add(new PropertiesSerializer(this.engines, inspector));
    serializers.add(new InspectorTimeSerializer(this.engines, inspector));
    serializers.add(new PipelineErrorSerializer(this.engines, inspector));
    serializers.add(new PatchesSerializer(this.engines, inspector));
    serializers.add(new ToolDiagnosticSerializer(this.engines, inspector));
    serializers.add(new PullRequestSerializer(this.engines, inspector));

    inspector.setPatchNotifier(this.patchNotifier);
    inspector.run();
    try {
      FileUtils.deleteDirectory(this.tempDir.getAbsolutePath());
    } catch (IOException e) {
      e.printStackTrace(System.out);
    }

    LOGGER.info("Inspector is finished. The process will exit now.");
    return true;
  }

  public void jenkinsMain(int buildId) {
    /* Setting config */
    this.getConfig().setClean(true);
    this.getConfig().setRunId("1234");
    this.getConfig().setGithubToken("");
    this.getConfig().setLauncherMode(LauncherMode.REPAIR);
    this.getConfig().setBuildId(buildId);
    this.getConfig().setZ3solverPath(new File("./z3_for_linux").getPath());

    this.getConfig().setRepairTools(new HashSet<>(Arrays.asList("NPEFix".split(" "))));
    if (this.getConfig().getLauncherMode() == LauncherMode.REPAIR) {
        LOGGER.info("The following repair tools will be used: " + StringUtils.join(this.getConfig().getRepairTools(), ", "));
    }

    this.initSerializerEngines();
    this.initNotifiers();
    this.mainProcess();
  }

  public static String getClassPath() {
      return System.getProperty("java.class.path");
  }

  public static void setOutErrStream(PrintStream ps) {
      System.setOut(ps);
      System.setErr(ps);

  }

  public static Logger getRootLogger() {
    return LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
  }

  public static void main(String[] args) {
      JenkinsLauncher launcher = new JenkinsLauncher();
      launcher.jenkinsMain(Integer.parseInt(args[0]));
  }
}