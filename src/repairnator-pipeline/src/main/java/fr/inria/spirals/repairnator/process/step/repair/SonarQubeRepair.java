package fr.inria.spirals.repairnator.process.step.repair;

import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;
import fr.inria.spirals.repairnator.process.testinformation.FailureLocation;
import fr.inria.spirals.repairnator.process.testinformation.FailureType;
import org.apache.commons.io.FileUtils;

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

import fr.inria.spirals.repairnator.config.RepairnatorConfig;

import org.apache.commons.lang3.text.StrSubstitutor;

import sonarquberepair.Main;

public class SonarQubeRepair extends AbstractRepairStep {
    public static final String TOOL_NAME = "SonarQubeRepair";
    public static final String RULE_LINK_TEMPLATE = "https://rules.sonarsource.com/java/RSPEC-";

    @Override
    public String getRepairToolName() {
        return TOOL_NAME;
    }

    @Override
    protected StepStatus businessExecute() {
        this.getLogger().info("Entrance in SonarQubeRepair step...");
        String pathToRepoDir = this.getInspector().getRepoLocalPath();
        
        this.getLogger().info("Repo: " + pathToRepoDir);
        this.getLogger().info("Rule: " + RepairnatorConfig.getInstance().getSonarRules());

        try {
            Main.main(new String[]{
                        "--repairPath",pathToRepoDir,
                        "--ruleNumbers",RepairnatorConfig.getInstance().getSonarRules(),
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
            }

            Map<String, String> values = new HashMap<String, String>();
            values.put("tools", String.join(",", this.getConfig().getRepairTools()));
            StrSubstitutor sub = new StrSubstitutor(values, "%(", ")");

            StringBuilder prTextBuilder = new StringBuilder(sub.replace(GITHUB_TEXT_PR))
                                                .append("The following PR aims to suggest fix for the following SonarQube rules: \n")
                                                .append(RULE_LINK_TEMPLATE).append(RepairnatorConfig.getInstance().getSonarRules());
            this.setPrText(prTextBuilder.toString());

            this.recordPatches(repairPatches,repairPatches.size());
        } else {
            return StepStatus.buildPatchNotFound(this);
        }
        return StepStatus.buildSuccess(this);
    }


}
