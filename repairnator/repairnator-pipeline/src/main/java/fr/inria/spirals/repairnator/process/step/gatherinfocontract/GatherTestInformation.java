package fr.inria.spirals.repairnator.process.step.gatherinfocontract;

import fr.inria.spirals.repairnator.ProjectState;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.process.step.gatherinfocontract.ContractForGatherTestInformation;
import fr.inria.spirals.repairnator.process.testinformation.FailureLocation;
import fr.inria.spirals.repairnator.process.testinformation.FailureType;
import org.apache.commons.lang3.StringUtils;
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
import java.util.*;

/**
 * Created by urli on 05/01/2017.
 */
public class GatherTestInformation extends AbstractStep {

    private static final String SUREFIREREPORT_PATH = "/target/surefire-reports";

    private int nbTotalTests;
    private int nbSkippingTests;
    private int nbFailingTests;
    private int nbErroringTests;
    private Set<FailureLocation> failureLocations;
    private Set<String> failureNames;
    private String failingModulePath;
    private boolean skipSettingStatusInformation;

    private ContractForGatherTestInformation contract;

    public GatherTestInformation(ProjectInspector inspector, ContractForGatherTestInformation contract, boolean skipSettingStatusInformation) {
        super(inspector);
        this.failureLocations = new HashSet<>();
        this.failureNames = new HashSet<>();
        this.contract = contract;
        this.skipSettingStatusInformation = skipSettingStatusInformation;
    }

    public int getNbFailingTests() {
        return nbFailingTests;
    }

    public int getNbErroringTests() {
        return nbErroringTests;
    }

    public int getNbTotalTests() {
        return nbTotalTests;
    }

    public int getNbSkippingTests() {
        return nbSkippingTests;
    }

    @Override
    protected void businessExecute() {
        this.getLogger().debug("Gathering test information...");

        File rootRepo = new File(this.inspector.getJobStatus().getPomDirPath());
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
            this.getLogger().warn("Error while traversing files to get surefire reports: " + e);
            this.addStepError(e.getMessage());
        }

        for (File surefireDir : surefireDirs) {
            SurefireReportParser parser = new SurefireReportParser(Arrays.asList(new File[] { surefireDir }),
                    Locale.ENGLISH, null);
            try {
                List<ReportTestSuite> testSuites = parser.parseXMLReportFiles();
                for (ReportTestSuite testSuite : testSuites) {
                    if (!skipSettingStatusInformation) {
                        if (testSuite.getNumberOfFailures() > 0 || testSuite.getNumberOfErrors() > 0) {
                            File failingModule = surefireDir.getParentFile().getParentFile();
                            this.failingModulePath = failingModule.getCanonicalPath();
                            this.getInspector().getJobStatus().setFailingModulePath(this.failingModulePath);
                            this.writeProperty("failingModule", this.failingModulePath);
                        }

                        this.nbTotalTests += testSuite.getNumberOfTests();
                        this.nbSkippingTests += testSuite.getNumberOfSkipped();
                        if (testSuite.getNumberOfFailures() > 0 || testSuite.getNumberOfErrors() > 0) {
                            for (ReportTestCase testCase : testSuite.getTestCases()) {
                                if (testCase.hasFailure()) {
                                    this.failureNames.add(testCase.getFailureType());
                                    FailureType typeTof = new FailureType(testCase.getFailureType(), testCase.getFailureMessage(), testCase.isError());
                                    FailureLocation failureLocation = null;

                                    for (FailureLocation location : this.failureLocations) {
                                        if (location.getClassName().equals(testCase.getFullClassName())) {
                                            failureLocation = location;
                                            break;
                                        }
                                    }

                                    if (failureLocation == null) {
                                        failureLocation = new FailureLocation(testCase.getFullClassName());
                                        this.failureLocations.add(failureLocation);
                                    }
                                    failureLocation.addFailure(typeTof);

                                    if (testCase.isError()) {
                                        failureLocation.addErroringMethod(testCase.getFullClassName()+"#"+testCase.getName());
                                    } else {
                                        failureLocation.addFailingMethod(testCase.getFullClassName()+"#"+testCase.getName());
                                    }
                                }
                            }
                        }
                    }
                    this.nbErroringTests += testSuite.getNumberOfErrors();
                    this.nbFailingTests += testSuite.getNumberOfFailures();
                }

                if (this.nbFailingTests > 0) {
                    this.setState(ProjectState.HASTESTFAILURE);
                } else if (this.nbErroringTests > 0) {
                    this.setState(ProjectState.HASTESTERRORS);
                } else {
                    this.setState(ProjectState.NOTFAILING);
                }
            } catch (MavenReportException e) {
                this.addStepError("Error while parsing files to get test information:",e);
            } catch (IOException e) {
                this.addStepError("Error while getting the failing module path: ",e);
            }
        }

        if (!this.skipSettingStatusInformation) {
            this.writeProperty("error-types", StringUtils.join(this.failureNames, ","));
            this.writeProperty("failing-test-cases", StringUtils.join(this.failureLocations, ","));
            this.inspector.getJobStatus().setFailureLocations(this.failureLocations);
            this.inspector.getJobStatus().setFailureNames(this.failureNames);
        }


        this.shouldStop = contract.shouldBeStopped(this);
    }

}
