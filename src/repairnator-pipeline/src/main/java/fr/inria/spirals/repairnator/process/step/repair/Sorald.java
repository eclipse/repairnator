package fr.inria.spirals.repairnator.process.step.repair;

import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.git.GitHelper;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.Git;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.lang.StringBuilder;
import java.util.Map;
import java.util.HashMap;
import java.util.*;
import java.net.URISyntaxException;
import java.util.function.Function;
import java.util.stream.Collectors;

import fr.inria.spirals.repairnator.utils.DateUtils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;

import org.apache.commons.lang3.text.StrSubstitutor;

import org.eclipse.jgit.lib.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import sorald.Constants;
import sorald.Main;

public class Sorald extends AbstractRepairStep {
    public static final String TOOL_NAME = "Sorald";
    public static final String RULE_LINK_TEMPLATE = "https://rules.sonarsource.com/java/RSPEC-";
    private final List<RepairPatch> allPatches = new ArrayList<RepairPatch>();
    private Git forkedGit;
    private String forkedRepo;
    boolean skipPR;

    public Sorald(){
        skipPR = getConfig().isSoraldSkipPR();
    }

    @Override
    public String getRepairToolName() {
        return TOOL_NAME;
    }


    @Override
    protected StepStatus businessExecute() {
        boolean patchFound = false;
        this.getLogger().info("Entrance in Sorald step...");
        String pathToRepoDir = this.getInspector().getRepoLocalPath();

        Map<String, Set<String>> introducedViolationTypesToFiles;
        try {
            introducedViolationTypesToFiles = getIntroducedViolations(pathToRepoDir);
        } catch (Exception e) {
            return StepStatus.buildSkipped(this, "Error while mining with Sorald");
        }

        Map<String, String> values = new HashMap<String, String>();
        values.put("tools", String.join(",", this.getConfig().getRepairTools()));
        StrSubstitutor sub = new StrSubstitutor(values, "%(", ")");
        StringBuilder prTextBuilder = new StringBuilder().append("This PR fixes the violations for the following Sorald rules: \n");
        String newBranchName = "repairnator-patch-" + DateUtils.formatFilenameDate(new Date());

        for (String rule : RepairnatorConfig.getInstance().getSonarRules()) {
            if (RepairnatorConfig.getInstance().getSoraldCommitCollectorMode()
                    == RepairnatorConfig.SORALD_COMMIT_COLLECTOR_MODE.VIOLATION_INTRODUCING
                    && !introducedViolationTypesToFiles.keySet().contains(rule))
                continue;

            this.getLogger().info("Repo: " + pathToRepoDir);
            this.getLogger().info("Try to repair rule " + rule);
            String[] args = new String[]{
                            "--originalFilesPath",pathToRepoDir,
                            "--ruleKeys",rule,
                            "--workspace", this.getConfig().getWorkspacePath(),
                            "--gitRepoPath",pathToRepoDir,
                            "--prettyPrintingStrategy","SNIPER",
                            "--maxFixesPerRule","" + getConfig().getSoraldMaxFixesPerRule(),
                            "--repairStrategy",RepairnatorConfig.getInstance().getSoraldRepairMode().name(),
                            "--maxFilesPerSegment","" + RepairnatorConfig.getInstance().getSoraldSegmentSize()};
            try {
                Main.main(args);
            } catch (Exception e) {
                return StepStatus.buildSkipped(this, "Error while repairing with Sorald");
            }

            File patchDir = new File(RepairnatorConfig.getInstance().getWorkspacePath() + File.separator + "SoraldGitPatches");

            Set<String> violationIntroducingFiles = introducedViolationTypesToFiles.get(rule);

            if (patchDir.exists()) {
                File[] patchFilesArr = patchDir.listFiles();
                List<File> patchFiles = Arrays.stream(patchFilesArr)
                        .filter(x -> violationIntroducingFiles.stream().anyMatch(y -> y.contains(x.getName())))
                        .collect(Collectors.toList());

                this.getLogger().info("Number of patches found: " + patchFiles.size());
                if (patchFiles.size() != 0) {
                    List<RepairPatch> repairPatches = new ArrayList<RepairPatch>();
                    for (File patchFile : patchFiles) {
                        try {
                            String content = new String(Files.readAllBytes(patchFile.toPath()), StandardCharsets.UTF_8);
                            RepairPatch repairPatch = new RepairPatch(this.getRepairToolName(), "", content);
                            repairPatches.add(repairPatch);
                        } catch (Exception e) {
                            return StepStatus.buildSkipped(this, "Error while retrieving patches");
                        }
                        patchFile.delete();
                    }
                    prTextBuilder.append(RULE_LINK_TEMPLATE).append(rule + "\n");
                    this.performApplyPatch(repairPatches, repairPatches.size(), rule, newBranchName);
                    if (!patchFound) {
                        patchFound = true;
                        this.allPatches.addAll(repairPatches); // Only mailing patches will only support single rule repair - FIXME
                    }
                }
            }
        }

        prTextBuilder.append("If you do no want to receive automated PRs for Sorald warnings, reply to this PR with 'STOP'");
        if (!patchFound) {
            return StepStatus.buildPatchNotFound(this);
        }

        this.getInspector().getJobStatus().addPatches(this.getRepairToolName(), allPatches);
        if (this.getConfig().isCreatePR()) {
            this.setPrText(prTextBuilder.toString());
            try {
                this.pushPatches(this.forkedGit,this.forkedRepo,newBranchName);
                if(!skipPR){
                    this.setPRTitle("Fix Sorald violations");
                    this.createPullRequest(this.getInspector().getGitRepositoryBranch(),newBranchName);
                }
            } catch(IOException | GitAPIException | URISyntaxException e) {
                e.printStackTrace();
                return StepStatus.buildSkipped(this, "Error while creating pull request");
            }
        }
        System.out.println("All patches : " + allPatches.size());
        this.notify(allPatches);
        return StepStatus.buildSuccess(this);
    }

    /**
     * @return A map from ruleNumber to the set of files with more violation locations in the new version.
     */
    private Map<String, Set<String>> getIntroducedViolations(String pathToRepoDir)
            throws IOException, GitAPIException, ParseException {
        File copyRepoDir = new File(Files.createTempDirectory("repo_copy").toString());
        FileUtils.copyDirectory(new File(pathToRepoDir), copyRepoDir);

        Map<String, Set<String>> lastRuleToLocations = listViolationLocations(copyRepoDir);

        Git git = Git.open(copyRepoDir);
        ObjectId previousCommitId = git.getRepository().resolve("HEAD^");
        git.checkout().setName(previousCommitId.getName()).call();

        Map<String, Set<String>> previousRuleToLocations = listViolationLocations(copyRepoDir);

        FileUtils.deleteDirectory(copyRepoDir);

        Map<String, Set<String>> ret = new HashMap<String, Set<String>>();

        for (Map.Entry<String, Set<String>> e : lastRuleToLocations.entrySet()) {
            String ruleNumber = e.getKey();
            Map<String, Long> newFileToViolationCnt =
                    e.getValue().stream().map(specifier -> specifier.split(File.pathSeparator)[1])
                            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

            Map<String, Long> oldFileToViolationCnt =
                    previousRuleToLocations.get(ruleNumber).stream()
                            .map(specifier -> specifier.split(File.pathSeparator)[1])
                            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

            ret.put(ruleNumber, newFileToViolationCnt.entrySet().stream()
                    .filter(x -> !oldFileToViolationCnt.containsKey(x.getKey())
                            || x.getValue() > oldFileToViolationCnt.get(x.getKey()))
                    .map(Map.Entry::getKey).collect(Collectors.toSet()));
        }

        return ret;
    }

    /**
     * @param repoDir .
     * @return A map from ruleNumber to the set of corresponding violation locations.
     */
    private Map<String, Set<String>> listViolationLocations(File repoDir) throws IOException, ParseException {
        Map<String, Set<String>> ret = new HashMap<String, Set<String>>();

        File stats = new File(Files.createTempDirectory("mining_stats.json").toString());
        String[] args =
                new String[]{
                        Constants.MINE_COMMAND_NAME,
                        Constants.ARG_ORIGINAL_FILES_PATH,
                        repoDir.getPath(),
                        Constants.ARG_TEMP_DIR,
                        Files.createTempDirectory("mining_tmp").toString(),
                        Constants.ARG_STATS_OUTPUT_FILE,
                        stats.getPath()
                };

        Main.main(args);

        JSONParser parser = new JSONParser();
        JSONObject jo = (JSONObject) parser.parse(new FileReader(stats));
        JSONArray ja = (JSONArray) jo.get("minedRules");
        for (int i = 0; i < ja.size(); i++) {
            Set<String> violationLocations = new HashSet<String>();

            jo = (JSONObject) ja.get(i);
            String rule = jo.get("ruleKey").toString();

            JSONArray warningLocations = (JSONArray) jo.get("warningLocations");
            for (int j = 0; j < warningLocations.size(); j++) {
                String location = jo.get("violationSpecifier").toString();
                violationLocations.add(location);
            }

            if (violationLocations.size() > 0) {
                ret.put(rule, violationLocations);
            }
        }

        return ret;
    }

    private void performApplyPatch(List<RepairPatch> patchList, int patchNbsLimit, String ruleNumber, String newBranchName) {
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
                    this.applyPatches4Sonar(this.forkedGit, serializedPatches, patchNbsLimit, ruleNumber);
                } catch (IOException | GitAPIException | URISyntaxException e) {
                    this.addStepError("Error while creating the PR", e);
                }
            } else {
                this.addStepError("No file has been serialized, so no PR will be created");
            }

        }
    }

    private void applyPatches4Sonar(Git git, List<File> patchList, int nbPatch, String ruleNumber) throws IOException, GitAPIException, URISyntaxException {
        for (int i = 0; i < nbPatch && i < patchList.size(); i++) {
            File patch = patchList.get(i);
            ProcessBuilder processBuilder = new ProcessBuilder("git", "apply", patch.getAbsolutePath())
                    .directory(new File(this.getInspector().getRepoLocalPath())).inheritIO();

            try {
                Process p = processBuilder.start();
                p.waitFor();
            } catch (InterruptedException | IOException e) {
                this.addStepError("Error while executing git command to apply patch " + patch.getPath(), e);
            }
        }
        git.commit().setAll(true).setAuthor(GitHelper.getCommitterIdent()).setCommitter(GitHelper.getCommitterIdent()).setMessage("Proposal for patching Sorald rule " + ruleNumber).call();
    }
}
