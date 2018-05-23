package fr.inria.spirals.repairnator.scanner;

import fr.inria.jtravis.JTravis;
import fr.inria.jtravis.entities.*;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.states.BearsMode;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This class aims to provide utility methods to scan the projects and get
 * failing builds
 *
 * @author Simon Urli
 */
public class ProjectScanner {
    private final Logger logger = LoggerFactory.getLogger(ProjectScanner.class);
    private int totalRepoNumber;
    private int totalRepoUsingTravis;
    private int totalScannedBuilds;
    private int totalPRBuilds;
    private int totalBuildInJava;
    private int totalJavaPassingBuilds;
    private int totalBuildInJavaFailing;
    private int totalBuildInJavaFailingWithFailingTests;
    private int totalNumberOfFailingAndPassingBuildPairs;
    private int totalNumberOfPassingAndPassingBuildPairs;

    private Collection<String> slugs;
    private Collection<Repository> repositories;

    private Date lookFromDate;
    private Date lookToDate;
    private String runId;

    private Date scannerRunningBeginDate;
    private Date scannerRunningEndDate;
    private JTravis jTravis;

    public ProjectScanner(Date lookFromDate, Date lookToDate, String runId) {
        this.lookFromDate = lookFromDate;
        this.lookToDate = lookToDate;

        this.slugs = new HashSet<String>();
        this.repositories = new HashSet<Repository>();
        this.runId = runId;
        this.jTravis = RepairnatorConfig.getInstance().getJTravis();

        this.logger.info("Look from " + Utils.formatCompleteDate(this.lookFromDate) + " to " + Utils.formatCompleteDate(this.lookToDate));
    }

    public String getRunId() {
        return runId;
    }

    public int getTotalRepoNumber() {
        return totalRepoNumber;
    }

    public int getTotalRepoUsingTravis() {
        return totalRepoUsingTravis;
    }

    public int getTotalScannedBuilds() {
        return totalScannedBuilds;
    }

    public int getTotalPRBuilds() {
        return totalPRBuilds;
    }

    public int getTotalBuildInJava() {
        return totalBuildInJava;
    }

    public int getTotalJavaPassingBuilds() {
        return totalJavaPassingBuilds;
    }

    public int getTotalBuildInJavaFailing() {
        return totalBuildInJavaFailing;
    }

    public int getTotalBuildInJavaFailingWithFailingTests() {
        return totalBuildInJavaFailingWithFailingTests;
    }

    public int getTotalNumberOfFailingAndPassingBuildPairs() {
        return totalNumberOfFailingAndPassingBuildPairs;
    }

    public int getTotalNumberOfPassingAndPassingBuildPairs() {
        return totalNumberOfPassingAndPassingBuildPairs;
    }

    public Date getLookFromDate() {
        return lookFromDate;
    }

    public Date getLookToDate() {
        return lookToDate;
    }

    public Date getScannerRunningBeginDate() {
        return scannerRunningBeginDate;
    }

    public Date getScannerRunningEndDate() {
        return scannerRunningEndDate;
    }

    public String getScannerDuration() {
        if (this.scannerRunningBeginDate == null || this.scannerRunningEndDate == null) {
            return "";
        }
        double diffInMilliseconds = this.scannerRunningEndDate.getTime() - this.scannerRunningBeginDate.getTime();
        int minutes = (int) (diffInMilliseconds / 1000) / 60;
        int seconds = (int) (diffInMilliseconds / 1000) % 60;
        int hours = minutes / 60;
        minutes = minutes % 60;
        return hours + ":" + minutes + ":" + seconds;
    }

    private List<String> getFileContent(String path) throws IOException {
        List<String> result = new ArrayList<String>();
        File file = new File(path);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        while (reader.ready()) {
            result.add(reader.readLine().trim());
        }
        return result;
    }

    /**
     * Take a filepath as input containing a list of projects to scan. Check
     * last build of each project. And finally returns the list of failing
     * builds.
     *
     * @param path
     *            A path to a file formatted to contain a slug name of project
     *            per line (ex: INRIA/spoon)
     * @return a list of failing builds
     * @throws IOException
     */
    public Map<ScannedBuildStatus, List<BuildToBeInspected>> getListOfBuildsToBeInspectedFromProjects(String path) throws IOException {
        this.scannerRunningBeginDate = new Date();

        List<String> slugs = getFileContent(path);
        this.totalRepoNumber = slugs.size();
        this.logger.info("# Repositories found: "+this.totalRepoNumber);

        List<Repository> repos = getListOfValidRepository(slugs);
        Map<ScannedBuildStatus, List<BuildToBeInspected>> builds = getListOfBuildsFromRepo(repos);

        this.scannerRunningEndDate = new Date();

        return builds;
    }

    private List<Repository> getListOfValidRepository(List<String> allSlugs) {
        List<Repository> result = new ArrayList<Repository>();

        this.logger.debug("---------------------------------------------------------------");
        this.logger.debug("Checking the "+this.totalRepoNumber+" repositories.");
        this.logger.debug("---------------------------------------------------------------");
        for (String slug : allSlugs) {
            this.logger.debug("Get repo " + slug);
            Optional<Repository> repositoryOptional = this.jTravis.repository().fromSlug(slug);
            if (repositoryOptional.isPresent()) {
                Repository repo = repositoryOptional.get();
                Optional<Build> lastBuild = repo.getLastBuild(false);
                if (lastBuild.isPresent()) {
                    result.add(repo);
                } else {
                    this.logger.info("It seems that the repo " + slug + " does not have any Travis build.");
                }

            } else {
                this.logger.warn("Can't examine repo : " + slug);
            }
        }

        this.totalRepoUsingTravis = result.size();
        this.logger.info("# Repositories using Travis: "+this.totalRepoUsingTravis);
        return result;
    }

    private Map<ScannedBuildStatus, List<BuildToBeInspected>> getListOfBuildsFromRepo(List<Repository> repos) {
        Map<ScannedBuildStatus, List<BuildToBeInspected>> results = new HashMap<>();
        for (ScannedBuildStatus status : ScannedBuildStatus.values()) {
            results.put(status, new ArrayList<>());
        }

        this.logger.debug("---------------------------------------------------------------");
        this.logger.debug("Scanning builds.");
        this.logger.debug("---------------------------------------------------------------");
        for (Repository repo : repos) {
            Optional<List<Build>> builds = this.jTravis.build().betweenDates(repo.getSlug(), this.lookFromDate, this.lookToDate);
            if (builds.isPresent()) {
                List<Build> repoBuilds = builds.get();
                for (Build build : repoBuilds) {
                    this.totalScannedBuilds++;
                    BuildToBeInspected buildToBeInspected = getBuildToBeInspected(build);
                    if (buildToBeInspected != null) {
                        results.get(buildToBeInspected.getStatus()).add(buildToBeInspected);
                    }
                }
            }
        }

        return results;
    }

    public BuildToBeInspected getBuildToBeInspected(Build build) {
        if (testBuild(build)) {
            if (RepairnatorConfig.getInstance().getLauncherMode() == LauncherMode.REPAIR) {
                this.logger.debug("Build "+build.getId()+" is interesting to be inspected.");
                return new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, this.runId);
            } else {
                this.logger.debug("Build "+build.getId()+" seems interesting to be inspected, thus get its previous build...");
                Optional<Build> optionalBeforeBuild = this.jTravis.build().getBefore(build, true);
                if (optionalBeforeBuild.isPresent()) {
                    Build previousBuild = optionalBeforeBuild.get();
                    this.logger.debug("Previous build: " + previousBuild.getId());

                    BearsMode mode = RepairnatorConfig.getInstance().getBearsMode();
                    if ((mode == BearsMode.BOTH || mode == BearsMode.FAILING_PASSING) && previousBuild.getState() == StateType.FAILED && thereIsDiffOnJavaFile(build, previousBuild)) {
                        this.totalNumberOfFailingAndPassingBuildPairs++;
                        this.logger.debug("The pair "+previousBuild.getId()+" ["+previousBuild.getState()+"], "+build.getId()+" ["+build.getState()+"] is interesting to be inspected.");
                        return new BuildToBeInspected(previousBuild, build, ScannedBuildStatus.FAILING_AND_PASSING, this.runId);
                    } else {
                        if ((mode == BearsMode.BOTH || mode == BearsMode.PASSING_PASSING) && previousBuild.getState() == StateType.PASSED && thereIsDiffOnJavaFile(build, previousBuild) && thereIsDiffOnTests(build, previousBuild)) {
                            this.totalNumberOfPassingAndPassingBuildPairs++;
                            this.logger.debug("The pair "+previousBuild.getId()+" ["+previousBuild.getState()+"], "+build.getId()+" ["+build.getState()+"] is interesting to be inspected.");
                            return new BuildToBeInspected(previousBuild, build, ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES, this.runId);
                        } else {
                            this.logger.debug("The pair "+previousBuild.getId()+" ["+previousBuild.getState()+"], "+build.getId()+" ["+build.getState()+"] is NOT interesting to be inspected.");
                        }
                    }
                } else {
                    this.logger.debug("The previous build from "+build.getId()+" was not retrieved.");
                }
            }
        } else {
            this.logger.debug("Build "+build.getId()+" is not interesting to be inspected.");
        }
        return null;
    }

    protected boolean testBuild(Build build) {
        if (build.isPullRequest()) {
            this.totalPRBuilds++;
        }

        Repository repo = build.getRepository();
        String language = build.getLanguage();
        if ("java".equals(language)) {
            this.totalBuildInJava++;

            this.logger.debug("Repo " + repo.getSlug() + " with java language - build " + build.getId() + " - Status : "
                    + build.getState().name());
            if (build.getState() == StateType.FAILED) {
                this.totalBuildInJavaFailing++;

                for (Job job : build.getJobs()) {
                    RepairnatorConfig.getInstance().getJTravis().refresh(job);
                    if (job.getState() == StateType.FAILED) {
                        Optional<Log> optionalLog = job.getLog();

                        if (optionalLog.isPresent()) {
                            Log jobLog = optionalLog.get();
                            if (jobLog.getBuildTool() == BuildTool.MAVEN) {
                                TestsInformation testInfo = jobLog.getTestsInformation();

                                // testInfo can be null if the build tool is unknown
                                if (testInfo != null && (testInfo.getFailing() > 0 || testInfo.getErrored() > 0)) {
                                    this.totalBuildInJavaFailingWithFailingTests++;
                                    if (RepairnatorConfig.getInstance().getLauncherMode() == LauncherMode.REPAIR) {
                                        this.slugs.add(repo.getSlug());
                                        this.repositories.add(repo);
                                        return true;
                                    } else {
                                        return false;
                                    }
                                } else {
                                    logger.debug("No failing or erroring test found in build " + build.getId());
                                }
                            } else {
                                logger.debug("Maven is not used in the build " + build.getId());
                            }
                        } else {
                            logger.error("Error while getting a job log: (jobId: " + job.getId() + ")");
                        }
                    }
                }
            } else if (build.getState() == StateType.PASSED) {
                this.totalJavaPassingBuilds++;
                if (RepairnatorConfig.getInstance().getLauncherMode() == LauncherMode.BEARS) {
                    for (Job job : build.getJobs()) {
                        RepairnatorConfig.getInstance().getJTravis().refresh(job);
                        if (job.getState() == StateType.PASSED) {
                            Optional<Log> optionalLog = job.getLog();

                            if (optionalLog.isPresent()) {
                                Log jobLog = optionalLog.get();
                                if (jobLog.getBuildTool() == BuildTool.MAVEN) {
                                    this.slugs.add(repo.getSlug());
                                    this.repositories.add(repo);
                                    return true;
                                } else {
                                    logger.debug("Maven is not used in the build " + build.getId());
                                }
                            }
                        }
                    }
                }
            }
        } else {
            this.logger.warn("Examine repo " + repo.getSlug() + " Careful the following build " + build.getId()
                    + " is not in java but language: " + language);
        }

        return false;
    }

    private boolean thereIsDiffOnJavaFile(Build build, Build previousBuild) {
        GHCompare compare = this.getCompare(build, previousBuild);
        if (compare != null) {
            GHCommit.File[] modifiedFiles = compare.getFiles();
            for (GHCommit.File file : modifiedFiles) {
                if (file.getFileName().endsWith(".java")) {
                    this.logger.debug("First java file found: " + file.getFileName());
                    return true;
                }
            }
        }
        return false;
    }

    private boolean thereIsDiffOnTests(Build build, Build previousBuild) {
        GHCompare compare = this.getCompare(build, previousBuild);
        if (compare != null) {
            GHCommit.File[] modifiedFiles = compare.getFiles();
            for (GHCommit.File file : modifiedFiles) {
                if (file.getFileName().toLowerCase().contains("test") && file.getFileName().endsWith(".java")) {
                    this.logger.debug("First probable test file found: " + file.getFileName());
                    return true;
                }
            }
        }
        return false;
    }

    private GHCompare getCompare(Build build, Build previousBuild) {
        try {
            GitHub gh = GitHubBuilder.fromEnvironment().build();

            GHRateLimit rateLimit = gh.getRateLimit();
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            this.logger.debug("GitHub rate limit: Limit: " + rateLimit.limit + " - Remaining: " + rateLimit.remaining + " - Reset hour: " + dateFormat.format(rateLimit.reset));

            if (rateLimit.remaining > 2) {
                GHRepository ghRepo = gh.getRepository(build.getRepository().getSlug());
                GHCommit buildCommit = ghRepo.getCommit(build.getCommit().getSha());
                GHCommit previousBuildCommit = ghRepo.getCommit(previousBuild.getCommit().getSha());
                GHCompare compare = ghRepo.getCompare(previousBuildCommit, buildCommit);
                return compare;
            } else {
                this.logger.warn("You reached your rate limit for GitHub. You have to wait until " + dateFormat.format(rateLimit.reset) + " to get data. PRInformation will be null for build "+build.getId()+".");
            }
        } catch (IOException e) {
            this.logger.warn("Error while getting commit from GitHub: " + e);
        }
        return null;
    }

}
