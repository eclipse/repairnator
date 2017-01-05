package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.Launcher;
import fr.inria.spirals.repairnator.process.ProjectInspector;
import org.apache.maven.plugins.surefire.report.ReportTestCase;
import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.plugins.surefire.report.SurefireReportParser;
import org.apache.maven.reporting.MavenReportException;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Created by urli on 05/01/2017.
 */
public class GatherTestInformation extends AbstractStep {

    private static final String SUREFIREREPORT_PATH = "/target/surefire-reports";

    private int nbTotalTests;
    private int nbSkippingTests;
    private int nbFailingTests;
    private Map<String, Set<String>> typeOfFailures;

    public GatherTestInformation(ProjectInspector inspector) {
        super(inspector);
        this.typeOfFailures = new HashMap<>();
    }

    public int getNbFailingTests() {
        return nbFailingTests;
    }

    public int getNbTotalTests() {
        return nbTotalTests;
    }

    public int getNbSkippingTests() {
        return nbSkippingTests;
    }

    public Map<String, Set<String>> getTypeOfFailures() {
        return typeOfFailures;
    }

    @Override
    protected void businessExecute() {
        Launcher.LOGGER.debug("Start gathering test information...");
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
                this.nbTotalTests += testSuite.getNumberOfTests();
                this.nbSkippingTests += testSuite.getNumberOfSkipped();
                if (testSuite.getNumberOfFailures() > 0) {
                    for (ReportTestCase testCase : testSuite.getTestCases()) {
                        if (testCase.hasFailure()) {
                            this.nbFailingTests++;
                            String tof = testCase.getFailureType();
                            if (!this.typeOfFailures.containsKey(tof)) {
                                this.typeOfFailures.put(tof, new HashSet<String>());
                            }
                            Set<String> failingClasses = this.typeOfFailures.get(tof);
                            failingClasses.add(testCase.getFullClassName());
                        }
                    }
                }
            }

            if (this.nbFailingTests > 0) {
                this.setState(ProjectState.HASTESTFAILURE);
                this.shouldStop = false;
            } else {
                this.setState(ProjectState.NOTFAILING);
                this.shouldStop = true;
            }
        } catch (MavenReportException e) {
            Launcher.LOGGER.warn("Error while parsing files to get test information: "+e);
        }
    }
}
