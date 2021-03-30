package fr.inria.spirals.repairnator.process.step.repair.soraldbot;

import fr.inria.spirals.repairnator.process.step.repair.soraldbot.models.SoraldTargetCommit;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.inria.spirals.repairnator.process.step.repair.soraldbot.SoraldConstants.SPOON_SNIPER_MODE;

public class SoraldCIAdapter {
    private static final Logger logger = LoggerFactory.getLogger(SoraldCIAdapter.class);
    private static SoraldCIAdapter _instance;

    private String patchPrintingMode = SPOON_SNIPER_MODE;
    private String tmpdir;

    public SoraldCIAdapter(String tmpdir) {
        this.tmpdir = tmpdir;
    }

    public SoraldCIAdapter(String tmpdir, String patchPrintingMode) {
        this.tmpdir = tmpdir;
        this.patchPrintingMode = patchPrintingMode;
    }

    public static SoraldCIAdapter getInstance(String tmpdir, String patchPrintingMode) {
        if (_instance == null)
            _instance = patchPrintingMode == null ? new SoraldCIAdapter(tmpdir)
                    : new SoraldCIAdapter(tmpdir, patchPrintingMode);
        return _instance;
    }

    // returns list of fixed-commit generated urls
    public List<String> repairAndCreateForks(SoraldTargetCommit commit, List<String> rules)
            throws ParseException, GitAPIException, IOException, InterruptedException {
        logger.info("repairing: " + commit.getCommitUrl());

        File repoDir = SoraldGithubInteractionsAdapter.getInstance(tmpdir)
                .cloneRepo(commit.getRepoUrl(), commit.getCommitId(), "repo");
        logger.info("repo cloned: " + commit.getRepoName());

        Map<String, Set<String>> ruleToIntroducingFiles =
                SoraldCLAdapter.getInstance(tmpdir).getIntroducedViolations(repoDir);
        logger.info("number of introduced rules: " + ruleToIntroducingFiles.entrySet().size());

        List<String> forkUrls = new ArrayList<String>();

        ruleToIntroducingFiles.entrySet().forEach(e -> {
            String rule = e.getKey();
            if (!rules.contains(rule))
                return;

            List<File> patchedFiles = repair(rule, repoDir);

            Set<String> violationIntroducingFiles = ruleToIntroducingFiles.get(rule);

            patchedFiles = patchedFiles.stream()
                    .filter(x -> violationIntroducingFiles.stream()
                            .anyMatch(y -> {
                                try {
                                    return FileUtils.readFileToString(x, "UTF-8").contains(y);
                                } catch (IOException ioException) {
                                    ioException.printStackTrace();
                                    return false;
                                }
                            }))
                    .collect(Collectors.toList());

            logger.info("patch files generated for rule: " + rule);

            String forkUrl = SoraldGithubInteractionsAdapter.getInstance(tmpdir).createFork(patchedFiles, rule, commit);

            if (forkUrl != null)
                forkUrls.add(forkUrl);
        });

        return forkUrls;
    }



    // returns patch files
    private List<File> repair(String rule, File repoDir) {
        return SoraldCLAdapter.getInstance(tmpdir).repair(rule, repoDir, patchPrintingMode);
    }
}
