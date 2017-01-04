package fr.inria.spirals.repairnator.step;

import fr.inria.spirals.repairnator.Launcher;
import fr.inria.spirals.repairnator.ProjectInspector;
import fr.inria.spirals.repairnator.ProjectState;
import org.apache.maven.plugins.surefire.report.ReportTestCase;
import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.plugins.surefire.report.SurefireReportParser;
import org.apache.maven.reporting.MavenReportException;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by urli on 03/01/2017.
 */
public class TestProject extends BuildProject {

    private static final String SUREFIREREPORT_PATH = "/target/surefire-reports";

    public TestProject(ProjectInspector inspector) {
        super(inspector);
    }

    protected void businessExecute() {
        Launcher.LOGGER.debug("Start launching tests with maven.");

        System.setProperty("maven.surefire.timeout","1");
        int result = this.mavenBuild(true);
        System.clearProperty("maven.surefire.timeout");

        File rootRepo = new File(this.inspector.getRepoLocalPath());
        final List<File> surefireDirs = new ArrayList<File>();

        try {
            Files.walkFileTree(rootRepo.toPath(), new SimpleFileVisitor<Path>() {
                public FileVisitResult preVisitDirectory(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith(SUREFIREREPORT_PATH)) {
                        surefireDirs.add(file.toFile());
                        return FileVisitResult.SKIP_SUBTREE;
                    } else {
                        return FileVisitResult.CONTINUE;
                    }
                }
            });
        } catch (IOException e) {
            Launcher.LOGGER.warn("Error while traversing files to get surefire reports: "+e);
        }

        SurefireReportParser parser = new SurefireReportParser(surefireDirs, Locale.ENGLISH);
        try {
            List<ReportTestSuite> testSuites = parser.parseXMLReportFiles();
            for (ReportTestSuite testSuite : testSuites) {
                for (ReportTestCase testCase : testSuite.getTestCases()) {
                    if (testCase.hasFailure()) {
                        System.out.println(testCase.getClassName()+":"+testCase.getFailureType());
                    }
                }
            }
        } catch (MavenReportException e) {
            e.printStackTrace();
        }

        if (result == 0) {
            Launcher.LOGGER.info("Repository "+this.inspector.getRepoSlug()+" has passing all tests: then no test can be fixed...");
            this.shouldStop = true;
        } else {
            this.state = ProjectState.HASTESTFAILURE;
        }
    }
}
