package fr.inria.spirals.repairnator.pipeline;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.serializer.HardwareInfoSerializer;
import fr.inria.spirals.repairnator.serializer.InspectorSerializer;
import fr.inria.spirals.repairnator.serializer.InspectorSerializer4Bears;
import fr.inria.spirals.repairnator.serializer.InspectorTimeSerializer;
import fr.inria.spirals.repairnator.process.inspectors.JenkinsProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
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
import java.util.Properties;

/* Entry point as Jenkins plugin - skip JSAP */
public class JenkinsLauncher extends Launcher {
  private static Logger LOGGER = LoggerFactory.getLogger(JenkinsLauncher.class);
  private static String gitUrl;
  private String pushUrl;
  private String gitToken;
  private String gitBranch;
  private static File tempDir;
  private ProjectInspector inspector;

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

    this.inspector = new JenkinsProjectInspector(this.getConfig().getWorkspacePath(),this.getConfig().getGitUrl(),this.getConfig().getGitBranch(),this.getConfig().getGitCommitHash(),serializers, this.notifiers);
    serializers.add(new InspectorSerializer(this.engines, inspector));
    serializers.add(new PropertiesSerializer(this.engines, inspector));
    serializers.add(new InspectorTimeSerializer(this.engines, inspector));
    serializers.add(new PipelineErrorSerializer(this.engines, inspector));
    serializers.add(new PatchesSerializer(this.engines, inspector));
    serializers.add(new ToolDiagnosticSerializer(this.engines, inspector));
    serializers.add(new PullRequestSerializer(this.engines, inspector));

    inspector.setPatchNotifier(this.patchNotifier);
    inspector.run();

    LOGGER.info("Inspector is finished. The process will exit now.");
    return true;
  }


  private void printAllEnv() {
    Properties properties = System.getProperties();
    System.out.println("---------------------------------All envVars---------------------------------");
    properties.forEach((k, v) -> LOGGER.info(k + ":" + v));
    System.out.println("-----------------------------------------------------------------------------");
  }

  /* used for no travis */
  public void noTravisMain() {
    LOGGER.info("Repairnator will be running for - GitUrl: " + this.getConfig().getGitUrl() + " --  GitBranch: " + this.getConfig().getGitBranch() + " -- GitCommit: " + this.getConfig().getGitCommitHash());
    File f = new File(System.getProperty("java.class.path"));
    String oldUserDir = System.getProperty("user.dir");
    System.setProperty("java.class.path",f.getAbsolutePath());
    System.setProperty("user.dir",this.getConfig().getWorkspacePath());
    
    System.out.println("user.dir=" + System.getProperty("user.dir"));
    System.out.println("java.class.path=" + System.getProperty("java.class.path"));
    this.getConfig().setClean(true);
    this.getConfig().setRunId("1234");
    this.getConfig().setLauncherMode(LauncherMode.REPAIR);
    this.getConfig().setBuildId(0);
    this.getConfig().setGithubUserEmail("noreply@github.com");
    this.getConfig().setGithubUserName("repairnator");
    if (this.getConfig().getLauncherMode() == LauncherMode.REPAIR) {
        LOGGER.info("The following repair tools will be used: " + StringUtils.join(this.getConfig().getRepairTools(), ", "));
    }
    this.initSerializerEngines();
    this.initNotifiers();
    this.mainProcess();

    /* Do not erase workspace dir if jenkins - jenkins will clean this up, otherwise Filesys bug*/
    System.setProperty("user.dir",oldUserDir);
  }

  /* use for jenkins pure */
  public void jenkinsMain(String gitUrl,String gitToken,String gitBranch,String[] tools) {
    LOGGER.info("Repairnator will be running for - GitUrl: " + gitUrl + " --  GitBranch: " + gitBranch);
    File f = new File(System.getProperty("java.class.path"));
    this.tempDir = Files.createTempDir();
    String oldUserDir = System.getProperty("user.dir");
    System.setProperty("java.class.path",f.getAbsolutePath());
    System.setProperty("user.dir",this.tempDir.getAbsolutePath());

    this.gitUrl = gitUrl;
    this.gitBranch = gitBranch;
    this.getConfig().setClean(true);
    this.getConfig().setRunId("1234");
    this.getConfig().setLauncherMode(LauncherMode.REPAIR);
    this.getConfig().setBuildId(0);
    this.getConfig().setZ3solverPath(new File("./z3_for_linux").getPath());

    this.getConfig().setWorkspacePath(this.tempDir.getAbsolutePath());
    this.getConfig().setGithubToken(gitToken);
    this.getConfig().setPush(true);
    this.getConfig().setGithubUserEmail("noreply@github.com");
    this.getConfig().setGithubUserName("repairnator");
    this.getConfig().setCreatePR(true);
    this.getConfig().setFork(true);
    this.getConfig().setRepairTools(new HashSet<>(Arrays.asList(tools)));
    if (this.getConfig().getLauncherMode() == LauncherMode.REPAIR) {
        LOGGER.info("The following repair tools will be used: " + StringUtils.join(this.getConfig().getRepairTools(), ", "));
    }
    this.getConfig().setOutputPath(this.tempDir.getAbsolutePath());
    this.getConfig().setZ3solverPath(new File(this.tempDir.getAbsolutePath() + File.separator + "z3_for_linux").getPath());
    this.initSerializerEngines();
    this.initNotifiers();
    this.mainProcess();

    try {
      FileUtils.deleteDirectory(this.tempDir.getAbsolutePath());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    System.setProperty("user.dir",oldUserDir);
  }

  public static void main(String[] args) {
      JenkinsLauncher launcher = new JenkinsLauncher();
      launcher.jenkinsMain(args[0],args[1],args[2],args[3].split(","));
  }
}