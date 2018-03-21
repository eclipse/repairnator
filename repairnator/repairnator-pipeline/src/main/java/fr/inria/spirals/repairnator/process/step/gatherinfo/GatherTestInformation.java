package fr.inria.spirals.repairnator.process.step.gatherinfo;

import fr.inria.spirals.repairnator.process.inspectors.Metrics;
import fr.inria.spirals.repairnator.states.PipelineState;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.process.testinformation.FailureLocation;
import fr.inria.spirals.repairnator.process.testinformation.FailureType;
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

    /**
     * This step intends to gather test information produced during {@link fr.inria.spirals.repairnator.process.step.TestProject} step.
     * A contract is asked to know if the step should find test failure or if everything must pass. Moreover, this step send by default
     * information to the JobStatus, but it can be skipping by setting the boolean to true.
     * @param inspector The inspector responsible of the step pipeline
     * @param contract The contract to know in which case the step should stop the pipeline
     * @param skipSettingStatusInformation If set to true, the step won't push any information to the JobStatus of the inspector.
     */
    public GatherTestInformation(ProjectInspector inspector, ContractForGatherTestInformation contract, boolean skipSettingStatusInformation, String stepName) {
        super(inspector, stepName);
        this.failureLocations = new HashSet<>();
        this.failureNames = new HashSet<>();
        this.contract = contract;
        this.skipSettingStatusInformation = skipSettingStatusInformation;
    }

    public GatherTestInformation(ProjectInspector inspector, ContractForGatherTestInformation contract, boolean skipSettingStatusInformation) {
        this(inspector, contract, skipSettingStatusInformation, GatherTestInformation.class.getSimpleName());
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
                        this.nbTotalTests += testSuite.getNumberOfTests();
                        this.nbSkippingTests += testSuite.getNumberOfSkipped();

                        if (testSuite.getNumberOfFailures() > 0 || testSuite.getNumberOfErrors() > 0) {
                            File failingModule = surefireDir.getParentFile().getParentFile();
                            this.failingModulePath = failingModule.getCanonicalPath();
                            this.getInspector().getJobStatus().setFailingModulePath(this.failingModulePath);
                            this.writeProperty("failingModule", this.failingModulePath);
                            getLogger().info("Get the following failing module path: "+failingModulePath);

                            for (ReportTestCase testCase : testSuite.getTestCases()) {
                                if (testCase.hasFailure() || testCase.hasError()) {

                                    // sometimes surefire reports a failureType on the form:
                                    // "java.lang.NullPointerException:" we should avoid this case
                                    String failureType = testCase.getFailureType();

                                    if (failureType.endsWith(":")) {
                                        failureType = failureType.substring(0, failureType.length() - 1);
                                    }

                                    this.failureNames.add(failureType);
                                    FailureType typeTof = new FailureType(testCase.getFailureType(), testCase.getFailureMessage(), testCase.hasError());
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

                                    if (testCase.hasError()) {
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
                    this.setPipelineState(PipelineState.HASTESTFAILURE);
                } else if (this.nbErroringTests > 0) {
                    this.setPipelineState(PipelineState.HASTESTERRORS);
                } else {
                    this.setPipelineState(PipelineState.NOTFAILING);
                }
            } catch (MavenReportException e) {
                this.addStepError("Error while parsing files to get test information:",e);
            } catch (IOException e) {
                this.addStepError("Error while getting the failing module path: ",e);
            }
        }

        if (!this.skipSettingStatusInformation) {
            this.writeProperty("error-types", this.failureNames);
            this.writeProperty("failing-test-cases", this.failureLocations);
            this.writeProperty("totalNumberFailingTests", this.nbFailingTests);
            this.writeProperty("totalNumberErroringTests", this.nbErroringTests);
            this.writeProperty("totalNumberSkippingTests", this.nbSkippingTests);
            this.writeProperty("totalNumberRunningTests", this.nbTotalTests);
            this.inspector.getJobStatus().setFailureLocations(this.failureLocations);

            Metrics metrics = this.inspector.getJobStatus().getMetrics();
            metrics.setFailureNames(this.failureNames);
            metrics.setNbFailingTests(this.nbErroringTests+this.nbFailingTests);
            metrics.setNbRunningTests(this.nbTotalTests);
        }


        this.shouldStop = contract.shouldBeStopped(this);
    }

}
