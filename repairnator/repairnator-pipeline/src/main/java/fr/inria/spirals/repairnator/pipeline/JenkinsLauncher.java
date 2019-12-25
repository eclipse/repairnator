package fr.inria.spirals.repairnator.pipeline;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.serializer.HardwareInfoSerializer;
import fr.inria.spirals.repairnator.serializer.InspectorSerializer;
import fr.inria.spirals.repairnator.serializer.InspectorSerializer4Bears;
import fr.inria.spirals.repairnator.serializer.InspectorTimeSerializer;
import fr.inria.spirals.repairnator.process.inspectors.JenkinsProjectInspector;
import fr.inria.spirals.repairnator.serializer.InspectorSerializer;
import fr.inria.spirals.repairnator.serializer.PatchesSerializer;
import fr.inria.spirals.repairnator.serializer.PipelineErrorSerializer;
import fr.inria.spirals.repairnator.serializer.PropertiesSerializer;
import fr.inria.spirals.repairnator.serializer.ToolDiagnosticSerializer;
import fr.inria.spirals.repairnator.serializer.PullRequestSerializer;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.notifier.PatchNotifier;
import fr.inria.spirals.repairnator.notifier.JenkinsPatchNotifierImpl;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.notifier.BugAndFixerBuildsNotifier;
import fr.inria.spirals.repairnator.LauncherUtils;
import fr.inria.spirals.repairnator.notifier.ErrorNotifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.HashSet;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import java.io.File;
import java.io.IOException;
import com.google.common.io.Files;

/* Entry point as Jenkins plugin - skip JSAP */
public class JenkinsLauncher extends Launcher {
  private static Logger LOGGER = LoggerFactory.getLogger(JenkinsLauncher.class);
  private static String gitUrl;
  private String pushUrl;
  private String gitToken;
  private final File tempDir = Files.createTempDir();
  private JenkinsProjectInspector inspector;

  private static RepairnatorConfig getConfig() {
    return RepairnatorConfig.getInstance();
  }

  public JenkinsLauncher() {}

  @Override
  protected void initNotifiers() {
      List<NotifierEngine> notifierEngines = LauncherUtils.initNotifierEngines(LOGGER);
      ErrorNotifier.getInstance(notifierEngines);

      this.notifiers = new ArrayList<>();
      this.notifiers.add(new BugAndFixerBuildsNotifier(notifierEngines));

      this.patchNotifier = new JenkinsPatchNotifierImpl(notifierEngines);
  }

  @Override
  public boolean mainProcess() {

    HardwareInfoSerializer hardwareInfoSerializer = new HardwareInfoSerializer(this.engines, this.getConfig().getRunId(),this.getConfig().getBuildId()+"");
    hardwareInfoSerializer.serialize();

    List<AbstractDataSerializer> serializers = new ArrayList<>();

    /* instantiate with temp dir instead */

    this.inspector = new JenkinsProjectInspector(this.tempDir.getAbsolutePath(),this.gitUrl,serializers, this.notifiers);

    serializers.add(new InspectorSerializer(this.engines, inspector));
    serializers.add(new PropertiesSerializer(this.engines, inspector));
    serializers.add(new InspectorTimeSerializer(this.engines, inspector));
    serializers.add(new PipelineErrorSerializer(this.engines, inspector));
    serializers.add(new PatchesSerializer(this.engines, inspector));
    serializers.add(new ToolDiagnosticSerializer(this.engines, inspector));
    serializers.add(new PullRequestSerializer(this.engines, inspector));

    inspector.setPatchNotifier(this.patchNotifier);
    inspector.run();

    /* Delete the temp dir */
    /*try {
      FileUtils.deleteDirectory(this.tempDir.getAbsolutePath());
    } catch (IOException e) {
      e.printStackTrace(System.out);
    }
*/
    LOGGER.info("Inspector is finished. The process will exit now.");
    return true;
  }

  public void jenkinsMain(String gitUrl,String gitToken) {
    this.gitUrl = gitUrl;
    /* Setting config */
    this.getConfig().setClean(true);
    this.getConfig().setRunId("1234");
    this.getConfig().setGithubToken("");
    this.getConfig().setLauncherMode(LauncherMode.REPAIR);
    this.getConfig().setBuildId(0);
    this.getConfig().setZ3solverPath(new File("./z3_for_linux").getPath());

    this.getConfig().setGithubToken(gitToken);
    this.getConfig().setPush(true);
    this.getConfig().setGithubUserEmail("noreply@github.com");
    this.getConfig().setGithubUserName("repairnator");
    this.getConfig().setCreatePR(true);
    this.getConfig().setFork(true);

    this.getConfig().setRepairTools(new HashSet<>(Arrays.asList("NPEFix".split(" "))));
    if (this.getConfig().getLauncherMode() == LauncherMode.REPAIR) {
        LOGGER.info("The following repair tools will be used: " + StringUtils.join(this.getConfig().getRepairTools(), ", "));
    }

    this.initSerializerEngines();
    this.initNotifiers();
    this.mainProcess();
  }


  public static void main(String[] args) {
      JenkinsLauncher launcher = new JenkinsLauncher();
      launcher.jenkinsMain(args[0],"94450c6e53901ca87155a79662e8c9b88d7fbc03");
  }
}