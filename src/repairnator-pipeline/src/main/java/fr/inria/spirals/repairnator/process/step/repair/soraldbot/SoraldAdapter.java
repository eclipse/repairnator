package fr.inria.spirals.repairnator.process.step.repair.soraldbot;

import fr.inria.spirals.repairnator.process.step.repair.soraldbot.models.SoraldTargetCommit;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorald.Constants;
import sorald.Main;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static fr.inria.spirals.repairnator.process.step.repair.soraldbot.SoraldConstants.SORALD_GIT_PATCHES_DIR;

public class SoraldAdapter {
    private static final Logger logger = LoggerFactory.getLogger(SoraldAdapter.class);
    private static final String MINING_STATS_FILENAME = "mining_stats.json";
    private static final String MINING_TMP_DIR_PATH = "mining_tmp_dir";
    private static final String MINED_RULES_KEY = "minedRules";
    private static final String PREVIOUS_COMMIT_REF = "HEAD^";
    private static SoraldAdapter _instance;

    private String tmpdir;

    public SoraldAdapter(String tmpdir) {
        this.tmpdir = tmpdir;
    }

    public static SoraldAdapter getInstance(String tmpdir) {
        if (_instance == null)
            _instance = new SoraldAdapter(tmpdir);
        return _instance;
    }

    // Clones the repo in @repoPath, fixes all violations, returns violation introducing file paths.
    public Set<String> repairRepoAndReturnViolationIntroducingFiles (SoraldTargetCommit commit, String rule,
                                                                     String repoPath, String printingMode)
            throws ParseException, GitAPIException, IOException, InterruptedException {
        logger.info("repairing: " + commit.getCommitUrl());

        File repoDir = cloneRepo(commit.getRepoUrl(), commit.getCommitId(), tmpdir + File.separator + repoPath);
        logger.info("repo cloned: " + commit.getRepoName());

        Map<String, Set<String>> ruleToIntroducingFiles = getIntroducedViolations(repoDir);
        logger.info("number of introduced rules: " + ruleToIntroducingFiles.entrySet().size());

        if(!ruleToIntroducingFiles.containsKey(rule))
            return null;

        logger.info("new violations introduced for rule: " + rule);

//        repair(rule, repoDir, printingMode);

        return ruleToIntroducingFiles.get(rule);
    }


    // returns patch files
    public void repair(String rule, File repoDir, String patchPrintingMode) {
        String[] args = new String[]{
                Constants.REPAIR_COMMAND_NAME,
                Constants.ARG_SOURCE, repoDir.getPath(),
                Constants.ARG_RULE_KEY, rule,
//                Constants.ARG_TEMP_DIR, tmpdir,
                Constants.ARG_PRETTY_PRINTING_STRATEGY, patchPrintingMode};

        Main.main(args);
    }

    //TODO: just add the output file and use it in the server

    /**
     * Method to use the mine call in sorald
     * @param rule rule or rules to mine
     * @param repoDir repo to mine
     * @param outputFile name of the exist Json file with the data
     */
    public void mine(String rule, File repoDir, String outputFile) {
        String[] args = new String[]{
                Constants.MINE_COMMAND_NAME,
                Constants.ARG_SOURCE, repoDir.getPath(),
                Constants.ARG_RULE_KEYS, rule,
                Constants.ARG_STATS_OUTPUT_FILE,outputFile};

        Main.main(args);
    }

    /**
     * @return A map from ruleNumber to the set of files with more violation locations in the new version.
     */
    public Map<String, Set<String>> getIntroducedViolations(File repoDir)
            throws IOException, GitAPIException, ParseException, InterruptedException {
        File copyRepoDir = new File(tmpdir + File.separator + "copy_repo");
        if (copyRepoDir.exists())
            FileUtils.deleteDirectory(copyRepoDir);

        copyRepoDir.mkdirs();

        FileUtils.copyDirectory(repoDir, copyRepoDir);

        Map<String, Set<String>> lastRuleToLocations = listViolationLocations(copyRepoDir);
//        Map<String, Set<String>> lastRuleToLocations = null;

        ProcessBuilder processBuilder =
                new ProcessBuilder("git", "stash")
                        .directory(copyRepoDir).inheritIO();
        Process p = processBuilder.start();
        int res = p.waitFor();
        if (res != 0) {
            logger.error("cannot stash");
            return new HashMap<String, Set<String>>();
        }

        processBuilder =
                new ProcessBuilder("git", "checkout", PREVIOUS_COMMIT_REF)
                        .directory(copyRepoDir).inheritIO();
        p = processBuilder.start();
        res = p.waitFor();
        if (res != 0) {
            logger.error("cannot checkout to " + PREVIOUS_COMMIT_REF);
            return new HashMap<String, Set<String>>();
        }

        Map<String, Set<String>> previousRuleToLocations = listViolationLocations(copyRepoDir);
//        Map<String, Set<String>> previousRuleToLocations = null;

        FileUtils.deleteDirectory(copyRepoDir);

        Map<String, Set<String>> ret = new HashMap<String, Set<String>>();

        for (Map.Entry<String, Set<String>> e : lastRuleToLocations.entrySet()) {
            String ruleNumber = e.getKey();

            // a map from the filename to the number of violations of this type in that file
            Map<String, Long> newFileToViolationCnt =
                    e.getValue().stream().map(specifier -> specifier.split(File.pathSeparator)[1])
                            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

            Map<String, Long> oldFileToViolationCnt =
                    !previousRuleToLocations.containsKey(ruleNumber) ? new HashMap<String, Long>() :
                            previousRuleToLocations.get(ruleNumber).stream()
                                    .map(specifier -> specifier.split(File.pathSeparator)[1])
                                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

            ret.put(ruleNumber, newFileToViolationCnt.entrySet().stream()
                    .filter(x -> !oldFileToViolationCnt.containsKey(x.getKey())
                            || x.getValue() > oldFileToViolationCnt.get(x.getKey()))
                    .map(Map.Entry::getKey).collect(Collectors.toSet()));

            if (ret.containsKey(ruleNumber) && ret.get(ruleNumber).size() == 0)
                ret.remove(ruleNumber);
        }

        return ret;
    }

    /**
     * @param repoDir .
     * @return A map from ruleNumber to the set of corresponding violation locations.
     */
    public Map<String, Set<String>> listViolationLocations(File repoDir) throws IOException, ParseException {
        Map<String, Set<String>> ret = new HashMap<String, Set<String>>();

        File stats = new File(tmpdir + File.separator + MINING_STATS_FILENAME);
//                miningTmpFile = new File(tmpdir + File.separator + MINING_TMP_DIR_PATH);

        if (stats.exists())
            stats.delete();

//        if (miningTmpFile.exists())
//            FileUtils.deleteDirectory(miningTmpFile);

        stats.createNewFile();

//        miningTmpFile.mkdirs();

        String[] args =
                new String[]{
                        Constants.MINE_COMMAND_NAME,
                        Constants.ARG_SOURCE,
                        repoDir.getPath(),
//                        Constants.ARG_TEMP_DIR,
//                        miningTmpFile.getPath(),
                        Constants.ARG_STATS_OUTPUT_FILE,
                        stats.getPath(),
                        Constants.ARG_HANDLED_RULES
                };

//        FileUtils.deleteDirectory(miningTmpFile);

        Main.main(args);

        JSONParser parser = new JSONParser();
        JSONObject jo = (JSONObject) parser.parse(new FileReader(stats));
        JSONArray ja = (JSONArray) jo.get(MINED_RULES_KEY);
        for (int i = 0; i < ja.size(); i++) {
            Set<String> violationLocations = new HashSet<String>();

            jo = (JSONObject) ja.get(i);
            String rule = jo.get("ruleKey").toString();

            JSONArray warningLocations = (JSONArray) jo.get("warningLocations");
            for (int j = 0; j < warningLocations.size(); j++) {
                jo = (JSONObject) warningLocations.get(j);
                String location = jo.get("violationSpecifier").toString();
                violationLocations.add(location);
            }

            if (violationLocations.size() > 0) {
                ret.put(rule, violationLocations);
            }
        }

        stats.delete();

        return ret;
    }



    public static File cloneRepo(String repoUrl, String commitId, String dirname)
            throws IOException, GitAPIException {
        File repoDir = new File(dirname);

        if (repoDir.exists())
            FileUtils.deleteDirectory(repoDir);

        repoDir.mkdirs();

        Git git = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(repoDir)
                .call();

        if (commitId != null)
            git.checkout().setName(commitId).call();

        git.close();

        return repoDir;
    }
}
