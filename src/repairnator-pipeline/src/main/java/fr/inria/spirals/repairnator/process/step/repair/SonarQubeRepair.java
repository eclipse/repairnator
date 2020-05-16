package fr.inria.spirals.repairnator.process.step.repair;

import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;
import fr.inria.spirals.repairnator.process.testinformation.FailureLocation;
import fr.inria.spirals.repairnator.process.testinformation.FailureType;
import fr.inria.spirals.repairnator.process.git.GitHelper;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.Git;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.lang.StringBuilder;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.net.URISyntaxException;
import java.util.Date;

import fr.inria.spirals.repairnator.utils.DateUtils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;

import org.apache.commons.lang3.text.StrSubstitutor;

import sonarquberepair.Main;

public class SonarQubeRepair extends AbstractRepairStep {
    public static final String TOOL_NAME = "SonarQubeRepair";
    public static final String RULE_LINK_TEMPLATE = "https://rules.sonarsource.com/java/RSPEC-";
    private final List<RepairPatch> allPatches = new ArrayList<RepairPatch>();
    private Git forkedGit;
    private String forkedRepo;

    @Override
    public String getRepairToolName() {
        return TOOL_NAME;
    }


    @Override
    protected StepStatus businessExecute() {
        boolean patchFound = false;
        this.getLogger().info("Entrance in SonarQubeRepair step...");
        String pathToRepoDir = this.getInspector().getRepoLocalPath();

        Map<String, String> values = new HashMap<String, String>();
                values.put("tools", String.join(",", this.getConfig().getRepairTools()));
                StrSubstitutor sub = new StrSubstitutor(values, "%(", ")");
        StringBuilder prTextBuilder = new StringBuilder(sub.replace(GITHUB_TEXT_PR)).append("The following PR aims to suggest fix for the following SonarQube rules: \n");
        String newBranchName = "repairnator-patch-" + DateUtils.formatFilenameDate(new Date());

        for (String rule : RepairnatorConfig.getInstance().getSonarRules()) {
            this.getLogger().info("Repo: " + pathToRepoDir);
            this.getLogger().info("Try to repair rule " + rule);

            try {
                Main.main(new String[]{
                            "--originalFilesPath",pathToRepoDir,
                            "--ruleKeys",rule,
                            "--workspace",RepairnatorConfig.getInstance().getWorkspacePath(),
                            "--gitRepoPath",pathToRepoDir});
            } catch(Exception e) {
                return StepStatus.buildSkipped(this,"Error while repairing with SonarQubeRepair");
            }

            File patchDir = new File(RepairnatorConfig.getInstance().getWorkspacePath() + File.separator + "SonarGitPatches");

            if (!patchDir.exists()) {
                return StepStatus.buildPatchNotFound(this);
            }

            File[] patchFiles = patchDir.listFiles();

            this.getLogger().info("Number of patches found: " + patchFiles.length);
            if (patchFiles.length != 0) {
                List<RepairPatch> repairPatches = new ArrayList<RepairPatch>();
                for (File patchFile : patchFiles) {
                    try {
                        String content = new String(Files.readAllBytes(patchFile.toPath()), StandardCharsets.UTF_8);
                        RepairPatch repairPatch = new RepairPatch(this.getRepairToolName(), "", content);
                        repairPatches.add(repairPatch);
                    } catch (Exception e) {
                        return StepStatus.buildSkipped(this,"Error while retrieving patches");
                    }
                    patchFile.delete();
                }
                prTextBuilder.append(RULE_LINK_TEMPLATE).append(rule + "\n");
                this.performApplyPatch(repairPatches,repairPatches.size(),rule,newBranchName);
                if (!patchFound) {
                    patchFound = true;
                    this.allPatches.addAll(repairPatches); // Only mailing patches will only support single rule repair - FIXME
                }
            }
        }

        if (!patchFound) {
            return StepStatus.buildPatchNotFound(this);
        }

        this.getInspector().getJobStatus().addPatches(this.getRepairToolName(), allPatches);   
        if (this.getConfig().isCreatePR()) {
            this.setPrText(prTextBuilder.toString());
            try {
                this.pushPatches(this.forkedGit,this.forkedRepo,newBranchName);
                this.createPullRequest(this.getInspector().getGitRepositoryBranch(),newBranchName);
            } catch(IOException | GitAPIException | URISyntaxException e) {
                e.printStackTrace();
                return StepStatus.buildSkipped(this,"Error while creating pull request");
            }
        } 
        System.out.println("All patches : " + allPatches.size());
        this.notify(allPatches);
        return StepStatus.buildSuccess(this);
    }

    private void performApplyPatch(List<RepairPatch> patchList,int patchNbsLimit,String ruleNumber,String newBranchName) {
        if (!patchList.isEmpty()) {
            this.getInspector().getJobStatus().setHasBeenPatched(true);
            List<File> serializedPatches = null;

            try {
                serializedPatches = this.serializePatches(patchList);
            } catch (IOException e) {
                this.addStepError("Error while serializing patches", e);
            }
            if (serializedPatches != null) {
                try {
                    if (this.forkedGit == null) {
                        this.forkedRepo = this.getForkedRepoName();
                        if (this.forkedRepo == null) {
                            return;
                        }
                        this.forkedGit = this.createGitBranch4Push(newBranchName);
                    }
                    this.applyPatches4Sonar(this.forkedGit,serializedPatches,patchNbsLimit,ruleNumber);
                } catch (IOException | GitAPIException | URISyntaxException e) {
                    this.addStepError("Error while creating the PR", e);
                }
            } else {
                this.addStepError("No file has been serialized, so no PR will be created");
            }

        }
    }

    private void applyPatches4Sonar(Git git,List<File> patchList,int nbPatch,String ruleNumber) throws IOException, GitAPIException, URISyntaxException {
        for (int i = 0; i < nbPatch && i < patchList.size(); i++) {
            File patch = patchList.get(i);
            ProcessBuilder processBuilder = new ProcessBuilder("git", "apply", patch.getAbsolutePath())
                        .directory(new File(this.getInspector().getRepoLocalPath())).inheritIO();

            try {
                Process p = processBuilder.start();
                p.waitFor();
            } catch (InterruptedException|IOException e) {
                this.addStepError("Error while executing git command to apply patch " + patch.getPath(), e);
            }
        }
        git.commit().setAll(true).setAuthor(GitHelper.getCommitterIdent()).setCommitter(GitHelper.getCommitterIdent()).setMessage("Proposal for patching SonarQube rule " + ruleNumber).call();
    }
}
