package fr.inria.spirals.repairnator.scanner;

import fr.inria.spirals.jtravis.entities.*;
import fr.inria.spirals.jtravis.helpers.BuildHelper;
import fr.inria.spirals.jtravis.helpers.RepositoryHelper;
import fr.inria.spirals.repairnator.RepairMode;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
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

    public int getTotalPRBuilds() {
        return totalPRBuilds;
    }

    public int getTotalBuildInJava() {
        return totalBuildInJava;
    }

    public int getTotalBuildInJavaFailing() {
        return totalBuildInJavaFailing;
    }

    public Date getLimitDate() {
        return limitDate;
    }

    public int getTotalJavaPassingBuilds() {
        return totalJavaPassingBuilds;
    }

    public int getTotalRepoNumber() {
        return totalRepoNumber;
    }

    public int getTotalRepoUsingTravis() {
        return totalRepoUsingTravis;
    }

    public int getTotalBuildInJavaFailingWithFailingTests() {
        return totalBuildInJavaFailingWithFailingTests;
    }

    public int getTotalScannedBuilds() {
        return totalScannedBuilds;
    }

    public Collection<String> getSlugs() {
        return slugs;
    }

    public Collection<Repository> getRepositories() {
        return repositories;
    }

    public List<BuildToBeInspected> getListOfFailingBuildFromGivenBuildIds(String path) throws IOException {
        this.dateStart = new Date();
        List<String> buildsIds = getFileContent(path);
        this.totalScannedBuilds = buildsIds.size();

        List<BuildToBeInspected> buildsToBeInspected = new ArrayList<BuildToBeInspected>();

        for (String s : buildsIds) {
            int buildId;
            try {
                buildId = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                this.logger.error("Error while reading build ids from input: " + e.getMessage());
                continue;
            }

            Build build = BuildHelper.getBuildFromId(buildId, null);
            if (build == null) {
                this.logger.warn("The following build cannot be retrieved: " + buildId);
                continue;
            }
            if (testBuild(build, true)) {
                BuildToBeInspected buildToBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL);
                buildsToBeInspected.add(buildToBeInspected);
            }
        }

        this.totalRepoNumber = this.repositories.size();
        this.totalRepoUsingTravis = this.repositories.size();
        this.totalBuildInJavaFailingWithFailingTests = buildsToBeInspected.size();
        this.dateFinish = new Date();

        return buildsToBeInspected;
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
    public List<BuildToBeInspected> getListOfFailingBuildFromProjects(String path, RepairMode mode) throws IOException {
        return getListOfBuildsFromProjectsByBuildStatus(path, mode, true);
    }

    public List<BuildToBeInspected> getListOfPassingBuildsFromProjects(String path, RepairMode mode) throws IOException {
        return getListOfBuildsFromProjectsByBuildStatus(path, mode, false);
    }

    private List<BuildToBeInspected> getListOfBuildsFromProjectsByBuildStatus(String path, RepairMode mode, boolean targetFailing)
            throws IOException {
        this.dateStart = new Date();
        List<String> slugs = getFileContent(path);
        this.totalRepoNumber = slugs.size();

        List<Repository> repos = getListOfValidRepository(slugs);
        List<BuildToBeInspected> builds = getListOfBuildsFromRepo(repos, mode, targetFailing);

        this.dateFinish = new Date();
        return builds;
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

    private List<BuildToBeInspected> getListOfBuildsFromRepo(List<Repository> repos, RepairMode mode, boolean targetFailing) {
        List<BuildToBeInspected> buildsToBeInspected = new ArrayList<BuildToBeInspected>();

        for (Repository repo : repos) {
            List<Build> repoBuilds = BuildHelper.getBuildsFromRepositoryWithLimitDate(repo, this.limitDate);

            for (Build build : repoBuilds) {
                this.totalScannedBuilds++;
                if (testBuild(build, targetFailing)) {
                    if (mode == RepairMode.FORBEARS) {
                        // First of all, here it is checked if the current passing build has a
                        // previous build---if doesn't, nothing is made as this build is not
                        // useful for Bears
                        Build previousBuild = BuildHelper.getLastBuildOfSameBranchOfStatusBeforeBuild(build, null);
                        if (previousBuild != null) {
                            this.logger.debug("Build: " + build.getId());
                            this.logger.debug("Previous build: " + previousBuild.getId());

                            if (previousBuild.getBuildStatus() == BuildStatus.FAILED) {
                                BuildToBeInspected buildToBeInspected = new BuildToBeInspected(build, previousBuild, ScannedBuildStatus.PASSING_AND_FAIL);
                                buildsToBeInspected.add(buildToBeInspected);
                            } else {
                                if (previousBuild.getBuildStatus() == BuildStatus.PASSED && thereIsDiffOnTests(build, previousBuild)) {
                                    BuildToBeInspected buildToBeInspected = new BuildToBeInspected(build, previousBuild, ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES);
                                    buildsToBeInspected.add(buildToBeInspected);
                                } else {
                                    this.logger.debug("The pair of builds is not interesting.");
                                }
                            }
                        }
                    } else {
                        BuildToBeInspected buildToBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL);
                        buildsToBeInspected.add(buildToBeInspected);
                    }
                }
            }
        }

        return buildsToBeInspected;
    }

    public static Properties getPropertiesFromFile(String propertyFile) throws IOException {
        InputStream inputStream = new FileInputStream(propertyFile);
        Properties properties = new Properties();
        properties.load(inputStream);
        return properties;
    }

    private Properties getPropertiesFromInput(String input) throws IOException {
        List<String> content = getFileContent(input);

        if (content.isEmpty()) {
            throw new IOException("File " + input + " is empty.");
        }

        String propertyFileDir = content.get(0);
        String propFilePath = propertyFileDir + File.separator + AbstractStep.PROPERTY_FILENAME;
        return getPropertiesFromFile(propFilePath);
    }

    public String readWorkspaceFromInput(String input) throws IOException {
        Properties properties = getPropertiesFromInput(input);
        return properties.getProperty("workspace");
    }

    public List<BuildToBeInspected> readBuildFromInput(String input) throws IOException {
        List<BuildToBeInspected> buildsToBeInspected = new ArrayList<BuildToBeInspected>();

        Properties properties = getPropertiesFromInput(input);
        String buildId = properties.getProperty("buildid");
        if (buildId != null) {
            Build build = BuildHelper.getBuildFromId(Integer.parseInt(buildId), null);
            if (build != null && build.getBuildStatus() == BuildStatus.FAILED) {
                BuildToBeInspected buildToBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL);
                buildsToBeInspected.add(buildToBeInspected);
            }
        }

        return buildsToBeInspected;
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
                    return true;
                }
            }

        } catch (IOException e) {
            this.logger.warn("Error while getting commit from Github: " + e);
        }
        return false;
    }

}
