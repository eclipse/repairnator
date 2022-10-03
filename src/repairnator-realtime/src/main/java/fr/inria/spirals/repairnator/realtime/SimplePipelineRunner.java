package fr.inria.spirals.repairnator.realtime;

import com.martiansoftware.jsap.JSAPException;
import fr.inria.spirals.repairnator.GithubInputBuild;
import fr.inria.spirals.repairnator.InputBuild;
import fr.inria.spirals.repairnator.pipeline.Launcher;
import fr.inria.spirals.repairnator.states.LauncherMode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static fr.inria.spirals.repairnator.realtime.Constants.SORALD_NAME;
import static fr.inria.spirals.repairnator.realtime.Constants.SOBO_NAME;

public class SimplePipelineRunner implements PipelineRunner {
    private Path tmpWorkspaceFolder, tmpOutputFolder;

    @Override
    public void initRunner() {
        try {
            tmpWorkspaceFolder = Files.createTempDirectory("workspace");
            tmpOutputFolder = Files.createTempDirectory("output");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void submitBuild(InputBuild b) {
        String launcherMode=System.getenv("launcherMode");
        if (launcherMode.equals(LauncherMode.FEEDBACK.name())) {
            String feedbackTool = System.getenv("FEEDBACK_TOOL");
            if (feedbackTool != null && feedbackTool.equals(SOBO_NAME)) {
                if (!(b instanceof GithubInputBuild))
                    throw new RuntimeException("The input build is not a github build");

                GithubInputBuild githubInputBuild = (GithubInputBuild) b;
                String sonarRules = System.getenv("SONAR_RULES"), gitUrl = githubInputBuild.getUrl(),
                        gitCommit = githubInputBuild.getSha(),
                        workspace = System.getenv().containsKey("WORKSPACE_FOLDER") ? System.getenv("WORKSPACE_FOLDER") :
                                tmpWorkspaceFolder.toAbsolutePath().toString(),
                        output = System.getenv().containsKey("OUTPUT_FOLDER") ? System.getenv("OUTPUT_FOLDER") :
                                tmpOutputFolder.toAbsolutePath().toString();

                List<String> launcherConfig = new ArrayList<String>(Arrays.asList(
                        "--gitrepourl", gitUrl,
                        "--gitcommithash", gitCommit,
                        "--sonarRules", "S109",
                        "--feedbackTools", SOBO_NAME,
                        "--launcherMode", LauncherMode.FEEDBACK.name(),
                        "--workspace", workspace,
                        "--output", output));


                try {
                    Launcher launcher = new Launcher(launcherConfig.toArray(new String[0]));
                    launcher.mainProcess(); // what happens after this
                } catch (JSAPException e) {
                    throw new RuntimeException("Cannot create launcher.");
                }


            }
            else {
                throw new RuntimeException("Simple pipeline is not defined for feedback tools other than SOBO");
            }
        }
        else{
            String repairTool = System.getenv("REPAIR_TOOL");

            if (repairTool != null && repairTool.equals(SORALD_NAME)) {
                if (!(b instanceof GithubInputBuild))
                    throw new RuntimeException("The input build is not a github build");

                GithubInputBuild githubInputBuild = (GithubInputBuild) b;
                String sonarRules = System.getenv("SONAR_RULES"), gitUrl = githubInputBuild.getUrl(),
                        gitCommit = githubInputBuild.getSha(),
                        workspace = System.getenv().containsKey("WORKSPACE_FOLDER") ? System.getenv("WORKSPACE_FOLDER") :
                                tmpWorkspaceFolder.toAbsolutePath().toString(),
                        output = System.getenv().containsKey("OUTPUT_FOLDER") ? System.getenv("OUTPUT_FOLDER") :
                                tmpOutputFolder.toAbsolutePath().toString();

                List<String> launcherConfig = new ArrayList<String>(Arrays.asList(
                        "--gitrepourl", gitUrl,
                        "--gitcommithash", gitCommit,
                        "--sonarRules", sonarRules,
                        "--repairTools", SORALD_NAME,
                        "--launcherMode", LauncherMode.GIT_REPOSITORY.name(),
                        "--workspace", workspace,
                        "--output", output));

                if (System.getenv().containsKey("CREATE_FORK") && Boolean.parseBoolean(System.getenv().get("CREATE_FORK")))
                    launcherConfig.add("--createFork");

                try {
                    Launcher launcher = new Launcher(launcherConfig.toArray(new String[0]));
                    launcher.mainProcess(); // what happens after this
                } catch (JSAPException e) {
                    throw new RuntimeException("Cannot create launcher.");
                }


            }
            else {
                throw new RuntimeException("Simple pipeline is not defined for repair tools other than Sorald");
            }
        }
    }
}
