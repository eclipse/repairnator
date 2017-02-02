package fr.inria.spirals.repairnator.process;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.BuildStatus;
import fr.inria.spirals.jtravis.entities.Job;
import fr.inria.spirals.jtravis.entities.Log;
import fr.inria.spirals.jtravis.entities.Repository;
import fr.inria.spirals.jtravis.entities.TestsInformation;
import fr.inria.spirals.jtravis.helpers.BuildHelper;
import fr.inria.spirals.jtravis.helpers.RepositoryHelper;
import fr.inria.spirals.repairnator.Launcher;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * This class aims to provide utility methods to scan the projects and get failing builds
 *
 * @author Simon Urli
 */
public class ProjectScanner {
    private final Logger logger = LoggerFactory.getLogger(ProjectScanner.class);
    private int totalRepoNumber;
    private int totalRepoUsingTravis;
    private int totalScannedBuilds;
    private int totalPassingBuilds;
    private int totalBuildInJava;
    private int totalBuildInJavaFailing;
    private int totalBuildInJavaFailingWithFailingTests;
    private Date dateStart;
    private Date dateFinish;

    private Collection<String> slugs;
    private Collection<Repository> repositories;
    private Collection<Integer> buildsId;
    private Date limitDate;

    public ProjectScanner(int lookupHours) {
        this.slugs = new HashSet<String>();
        this.repositories = new HashSet<Repository>();

        Calendar limitCal = Calendar.getInstance();
        limitCal.add(Calendar.HOUR_OF_DAY, -lookupHours);
        this.limitDate = limitCal.getTime();
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

    public int getTotalPassingBuilds() {
        return totalPassingBuilds;
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

    public List<Build> getListOfFailingBuildFromGivenBuildIds(String path) throws IOException {
        this.dateStart = new Date();
        List<String> buildsIds = getFileContent(path);
        this.totalScannedBuilds = buildsIds.size();

        this.buildsId = new ArrayList<Integer>();

        List<Build> result = new ArrayList<Build>();

        for (String s : buildsIds) {
            int buildId;
            try {
                buildId = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                this.logger.error("Error while reading build ids from input: "+e.getMessage());
                continue;
            }

            Build build = BuildHelper.getBuildFromId(buildId, null);
            if (build == null) {
                this.logger.warn("The following build cannot be retrieved: "+buildId);
                continue;
            }
            if (testBuild(build)) {
                result.add(build);
                this.buildsId.add(build.getId());
            }
        }

        this.totalRepoNumber = this.repositories.size();
        this.totalRepoUsingTravis = this.repositories.size();
        this.totalBuildInJavaFailingWithFailingTests = result.size();
        this.dateFinish = new Date();

        return result;
    }

    /**
     * Take a filepath as input containing a list of projects to scan. Check last build of each project. And finally returns the list of failing builds.
     *
     * @param path A path to a file formatted to contain a slug name of project per line (ex: INRIA/spoon)
     * @return a list of failing builds
     * @throws IOException
     */
    public List<Build> getListOfFailingBuildFromProjects(String path) throws IOException {
        this.dateStart = new Date();
        List<String> slugs = getFileContent(path);
        this.totalRepoNumber = slugs.size();

        List<Repository> repos = getListOfValidRepository(slugs);
        List<Build> builds = getListOfBuildsFromRepo(repos);

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
            this.logger.debug("Get repo "+slug);
            Repository repo = RepositoryHelper.getRepositoryFromSlug(slug);
            if (repo != null) {
                Build lastBuild = repo.getLastBuild();
                if (lastBuild != null) {
                    result.add(repo);
                } else {
                    this.logger.info("It seems that the repo "+slug+" does not have any Travis build.");
                }

            } else {
                this.logger.warn("Can't examine repo : "+slug);
            }
        }

        this.totalRepoUsingTravis = result.size();
        return result;
    }

    private boolean testBuild(Build build) {
        Repository repo = build.getRepository();
        if (build.getConfig().getLanguage().equals("java")) {
            this.totalBuildInJava++;

            // TODO: get number of build due to PR by project by day
            this.logger.debug("Repo "+repo.getSlug()+" with java language - build "+build.getId()+" - Status : "+build.getBuildStatus().name());
            if (build.getBuildStatus() == BuildStatus.FAILED) {
                this.totalBuildInJavaFailing++;
                for (Job job : build.getJobs()) {
                    Log jobLog = job.getLog();
                    TestsInformation testInfo = jobLog.getTestsInformation();

                    if (testInfo.getFailing() > 0) {
                        this.slugs.add(repo.getSlug());
                        this.repositories.add(repo);
                        return true;
                    }
                }
            } else if (build.getBuildStatus() == BuildStatus.PASSED) {
                this.totalPassingBuilds++;
            }
        } else {
            this.logger.warn("Examine repo "+repo.getSlug()+" Careful the following build "+build.getId()+" is not in java but language: "+build.getConfig().getLanguage());
        }
        return false;
    }

    private List<Build> getListOfBuildsFromRepo(List<Repository> repos) {
        List<Build> result = new ArrayList<Build>();

        this.buildsId = new ArrayList<Integer>();
        for (Repository repo : repos) {
            List<Build> repoBuilds = BuildHelper.getBuildsFromRepositoryWithLimitDate(repo, this.limitDate);

            for (Build build : repoBuilds) {
                this.totalScannedBuilds++;
                if (testBuild(build)) {
                    result.add(build);
                    this.buildsId.add(build.getId());
                }
            }
        }

        this.totalBuildInJavaFailingWithFailingTests = result.size();
        return result;
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
            throw new IOException("File "+input+" is empty.");
        }

        String propertyFileDir = content.get(0);
        String propFilePath = propertyFileDir+File.separator+AbstractStep.PROPERTY_FILENAME;
        return getPropertiesFromFile(propFilePath);
    }

    public String readWorkspaceFromInput(String input) throws IOException {
        Properties properties = getPropertiesFromInput(input);
        return properties.getProperty("workspace");
    }

    public List<Build> readBuildFromInput(String input) throws IOException {
        List<Build> result = new ArrayList<Build>();

        Properties properties = getPropertiesFromInput(input);
        String buildId = properties.getProperty("buildid");
        if (buildId != null) {
            Build build = BuildHelper.getBuildFromId(Integer.parseInt(buildId), null);
            if (build != null) {
                result.add(build);
            }
        }


        return result;
    }
}
