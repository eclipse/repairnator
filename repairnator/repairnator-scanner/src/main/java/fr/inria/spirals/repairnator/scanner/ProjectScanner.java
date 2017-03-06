package fr.inria.spirals.repairnator.scanner;

import fr.inria.spirals.jtravis.entities.*;
import fr.inria.spirals.jtravis.helpers.BuildHelper;
import fr.inria.spirals.jtravis.helpers.RepositoryHelper;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.LauncherMode;
import fr.inria.spirals.repairnator.ScannedBuildStatus;
import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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
    private Date dateStart;
    private Date dateFinish;

    private Collection<String> slugs;
    private Collection<Repository> repositories;
    private Date limitDate;

    public ProjectScanner(int lookupHours) {
        this.slugs = new HashSet<String>();
        this.repositories = new HashSet<Repository>();

        Calendar limitCal = Calendar.getInstance();
        limitCal.add(Calendar.HOUR_OF_DAY, -lookupHours);
        this.limitDate = limitCal.getTime();
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

    public Collection<String> getSlugs() {
        return slugs;
    }

    public Collection<Repository> getRepositories() {
        return repositories;
    }

    public Date getLimitDate() {
        return limitDate;
    }

    public List<BuildToBeInspected> getListOfFailingBuildsFromGivenBuildIds(String path, LauncherMode mode) throws IOException {
        return getListOfBuildsFromGivenBuildIds(path, mode, true);
    }

    public List<BuildToBeInspected> getListOfPassingBuildsFromGivenBuildIds(String path, LauncherMode mode) throws IOException {
        return getListOfBuildsFromGivenBuildIds(path, mode, false);
    }

    public List<BuildToBeInspected> getListOfBuildsFromGivenBuildIds(String path, LauncherMode mode, boolean targetFailing) throws IOException {
        this.dateStart = new Date();
        List<String> buildsIds = getFileContent(path);
        this.totalScannedBuilds = buildsIds.size();

        List<BuildToBeInspected> buildsToBeInspected = new ArrayList<BuildToBeInspected>();

        for (String buildIdStr : buildsIds) {
            int buildId;
            try {
                buildId = Integer.parseInt(buildIdStr);
            } catch (NumberFormatException e) {
                this.logger.error("Error while reading build ids from input: " + e.getMessage());
                continue;
            }

            Build build = BuildHelper.getBuildFromId(buildId, null);
            if (build == null) {
                this.logger.warn("The following build cannot be retrieved: " + buildId);
                continue;
            }

            BuildToBeInspected buildToBeInspected = getBuildToBeInspected(build, mode, targetFailing);
            if (buildToBeInspected != null) {
                buildsToBeInspected.add(buildToBeInspected);
            }
        }

        this.totalRepoNumber = this.repositories.size();
        this.totalRepoUsingTravis = this.repositories.size();
        this.dateFinish = new Date();

        return buildsToBeInspected;
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
    public List<BuildToBeInspected> getListOfFailingBuildsFromProjects(String path, LauncherMode mode) throws IOException {
        return getListOfBuildsFromProjectsByBuildStatus(path, mode, true);
    }

    public List<BuildToBeInspected> getListOfPassingBuildsFromProjects(String path, LauncherMode mode) throws IOException {
        return getListOfBuildsFromProjectsByBuildStatus(path, mode, false);
    }

    private List<BuildToBeInspected> getListOfBuildsFromProjectsByBuildStatus(String path, LauncherMode mode, boolean targetFailing)
            throws IOException {
        this.dateStart = new Date();
        List<String> slugs = getFileContent(path);
        this.totalRepoNumber = slugs.size();

        List<Repository> repos = getListOfValidRepository(slugs);
        List<BuildToBeInspected> builds = getListOfBuildsFromRepo(repos, mode, targetFailing);

        this.dateFinish = new Date();
        return builds;
    }

    private List<Repository> getListOfValidRepository(List<String> allSlugs) {
        List<Repository> result = new ArrayList<Repository>();

        for (String slug : allSlugs) {
            this.logger.debug("Get repo " + slug);
            Repository repo = RepositoryHelper.getRepositoryFromSlug(slug);
            if (repo != null) {
                Build lastBuild = repo.getLastBuild(false);
                if (lastBuild != null) {
                    result.add(repo);
                } else {
                    this.logger.info("It seems that the repo " + slug + " does not have any Travis build.");
                }

            } else {
                this.logger.warn("Can't examine repo : " + slug);
            }
        }

        this.totalRepoUsingTravis = result.size();
        return result;
    }

    private List<BuildToBeInspected> getListOfBuildsFromRepo(List<Repository> repos, LauncherMode mode, boolean targetFailing) {
        List<BuildToBeInspected> buildsToBeInspected = new ArrayList<BuildToBeInspected>();

        for (Repository repo : repos) {
            List<Build> repoBuilds = BuildHelper.getBuildsFromRepositoryWithLimitDate(repo, this.limitDate);

            for (Build build : repoBuilds) {
                this.totalScannedBuilds++;
                BuildToBeInspected buildToBeInspected = getBuildToBeInspected(build, mode, targetFailing);
                if (buildToBeInspected != null) {
                    buildsToBeInspected.add(buildToBeInspected);
                }
            }
        }

        return buildsToBeInspected;
    }

    public BuildToBeInspected getBuildToBeInspected(Build build, LauncherMode mode, boolean targetFailing) {
        if (testBuild(build, targetFailing)) {
            if (mode == LauncherMode.SLUGFORBEARS || mode == LauncherMode.BUILDFORBEARS) {
                Build previousBuild = BuildHelper.getLastBuildOfSameBranchOfStatusBeforeBuild(build, null);
                if (previousBuild != null) {
                    this.logger.debug("Build: " + build.getId());
                    this.logger.debug("Previous build: " + previousBuild.getId());

                    if (previousBuild.getBuildStatus() == BuildStatus.FAILED && thereIsDiffOnJavaSourceCode(build, previousBuild)) {
                        this.totalNumberOfFailingAndPassingBuildPairs++;
                        return new BuildToBeInspected(build, previousBuild, ScannedBuildStatus.FAILING_AND_PASSING);
                    } else {
                        if (previousBuild.getBuildStatus() == BuildStatus.PASSED && thereIsDiffOnJavaSourceCode(build, previousBuild) && thereIsDiffOnTests(build, previousBuild)) {
                            this.totalNumberOfPassingAndPassingBuildPairs++;
                            return new BuildToBeInspected(build, previousBuild, ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES);
                        } else {
                            this.logger.debug("The pair of builds is not interesting.");
                        }
                    }
                }
            } else {
                return new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL);
            }
        }
        return null;
    }

    private boolean testBuild(Build build, boolean targetFailing) {
        if (build.isPullRequest()) {
            this.totalPRBuilds++;
        }

        Repository repo = build.getRepository();
        if (build.getConfig().getLanguage().equals("java")) {
            this.totalBuildInJava++;

            this.logger.debug("Repo " + repo.getSlug() + " with java language - build " + build.getId() + " - Status : "
                    + build.getBuildStatus().name());
            if (build.getBuildStatus() == BuildStatus.FAILED) {
                this.totalBuildInJavaFailing++;

                for (Job job : build.getJobs()) {
                    Log jobLog = job.getLog();

                    if (jobLog != null) {
                        TestsInformation testInfo = jobLog.getTestsInformation();

                        // testInfo can be null if the build tool is unknown
                        if (testInfo != null && (testInfo.getFailing() > 0 || testInfo.getErrored() > 0)) {
                            this.totalBuildInJavaFailingWithFailingTests++;
                            if (targetFailing) {
                                this.slugs.add(repo.getSlug());
                                this.repositories.add(repo);
                                return true;
                            }
                        }
                    } else {
                        logger.error("Error while getting a job log: (jobId: " + job.getId() + ")");
                    }
                }
            } else if (build.getBuildStatus() == BuildStatus.PASSED) {
                this.totalJavaPassingBuilds++;
                if (!targetFailing) {
                    this.slugs.add(repo.getSlug());
                    this.repositories.add(repo);
                    return true;
                }
            }
        } else {
            this.logger.warn("Examine repo " + repo.getSlug() + " Careful the following build " + build.getId()
                    + " is not in java but language: " + build.getConfig().getLanguage());
        }
        return false;
    }

    public boolean thereIsDiffOnJavaSourceCode(Build build, Build previousBuild) {
        try {
            GitHub gh = GitHubBuilder.fromEnvironment().build();
            GHRepository ghRepo = gh.getRepository(build.getRepository().getSlug());
            GHCommit buildCommit = ghRepo.getCommit(build.getCommit().getSha());
            GHCommit previousBuildCommit = ghRepo.getCommit(previousBuild.getCommit().getSha());
            GHCompare compare = ghRepo.getCompare(previousBuildCommit, buildCommit);
            GHCommit.File[] modifiedFiles = compare.getFiles();
            for (GHCommit.File file : modifiedFiles) {
                if (file.getFileName().endsWith(".java")) {
                    logger.error("SC File: " + file.getFileName());
                    return true;
                }
            }

        } catch (IOException e) {
            this.logger.warn("Error while getting commit from Github: " + e);
        }
        return false;
    }

    public boolean thereIsDiffOnTests(Build build, Build previousBuild) {
        try {
            GitHub gh = GitHubBuilder.fromEnvironment().build();
            GHRepository ghRepo = gh.getRepository(build.getRepository().getSlug());
            GHCommit buildCommit = ghRepo.getCommit(build.getCommit().getSha());
            GHCommit previousBuildCommit = ghRepo.getCommit(previousBuild.getCommit().getSha());
            GHCompare compare = ghRepo.getCompare(previousBuildCommit, buildCommit);
            GHCommit.File[] modifiedFiles = compare.getFiles();
            for (GHCommit.File file : modifiedFiles) {
                if (file.getFileName().contains("src/test/java")) {
                    logger.error("TC File: " + file.getFileName());
                    return true;
                }
            }

        } catch (IOException e) {
            this.logger.warn("Error while getting commit from Github: " + e);
        }
        return false;
    }

}
