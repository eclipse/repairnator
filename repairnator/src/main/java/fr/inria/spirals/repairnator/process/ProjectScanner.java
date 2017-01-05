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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * This class aims to provide utility methods to scan the projects and get failing builds
 *
 * @author Simon Urli
 */
public class ProjectScanner {

    private int totalRepoNumber;
    private int totalScannedRepo;
    private int totalScannedBuilds;
    private int totalBuildInJava;
    private int totalBuildInJavaFailing;
    private int totalBuildInJavaWithFailingTests;

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

    public int getTotalScannedRepo() {
        return totalScannedRepo;
    }

    public int getTotalBuildInJavaWithFailingTests() {
        return totalBuildInJavaWithFailingTests;
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

    /**
     * Take a filepath as input containing a list of projects to scan. Check last build of each project. And finally returns the list of failing builds.
     *
     * @param path A path to a file formatted to contain a slug name of project per line (ex: INRIA/spoon)
     * @return a list of failing builds
     * @throws IOException
     */
    public List<Build> getListOfFailingBuildFromProjects(String path) throws IOException {
        List<String> slugs = readSlugProjectFromFilepath(path);
        List<Repository> repos = getListOfValidRepository(slugs);
        List<Build> builds = getListOfBuildsFromRepo(repos);

        return builds;
    }

    /**
     * Utility method to read the file and return the list of slug name
     * @param path A path to a file formatted to contain a slug name of project per line (ex: INRIA/spoon)
     * @return the list of slug name as strings
     * @throws IOException
     */
    private List<String> readSlugProjectFromFilepath(String path) throws IOException {
        List<String> result = new ArrayList<String>();
        File file = new File(path);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        while (reader.ready()) {
            result.add(reader.readLine().trim());
        }

        this.totalRepoNumber = result.size();

        return result;
    }

    private List<Repository> getListOfValidRepository(List<String> allSlugs) {
        List<Repository> result = new ArrayList<Repository>();

        for (String slug : allSlugs) {
            Launcher.LOGGER.debug("Get repo "+slug);
            Repository repo = RepositoryHelper.getRepositoryFromSlug(slug);
            if (repo != null) {
                Build lastBuild = repo.getLastBuild();
                if (lastBuild != null) {
                    result.add(repo);
                } else {
                    Launcher.LOGGER.info("It seems that the repo "+slug+" does not have any Travis build.");
                }

            } else {
                Launcher.LOGGER.warn("Can't examine repo : "+slug);
            }
        }

        this.totalScannedRepo = result.size();
        return result;
    }

    private List<Build> getListOfBuildsFromRepo(List<Repository> repos) {
        List<Build> result = new ArrayList<Build>();

        for (Repository repo : repos) {
            List<Build> repoBuilds = BuildHelper.getBuildsFromRepositoryWithLimitDate(repo, this.limitDate);

            for (Build build : repoBuilds) {
                this.totalScannedBuilds++;
                if (build.getConfig().getLanguage().equals("java")) {
                    this.totalBuildInJava++;
                    Launcher.LOGGER.debug("Repo "+repo.getSlug()+" with java language - build "+build.getId()+" - Status : "+build.getBuildStatus().name());
                    if (build.getBuildStatus() == BuildStatus.FAILED) {
                        this.totalBuildInJavaFailing++;
                        for (Job job : build.getJobs()) {
                            Log jobLog = job.getLog();
                            TestsInformation testInfo = jobLog.getTestsInformation();

                            if (testInfo.getFailing() > 0) {
                                result.add(build);
                                this.slugs.add(repo.getSlug());
                                this.repositories.add(repo);
                                break;
                            }
                        }
                    }
                } else {
                    Launcher.LOGGER.warn("Examine repo "+repo.getSlug()+" Careful the following build "+build.getId()+" is not in java but language: "+build.getConfig().getLanguage());
                }
            }
        }

        this.totalBuildInJavaWithFailingTests = result.size();
        return result;
    }
}
