package fr.inria.spirals.repairnator.process.step.gatherinfo;

import fr.inria.spirals.repairnator.process.inspectors.Metrics;
import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
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
    private int nbRunningTests;

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
    public GatherTestInformation(ProjectInspector inspector, boolean blockingStep, ContractForGatherTestInformation contract, boolean skipSettingStatusInformation, String stepName) {
        super(inspector, blockingStep, stepName);
        this.failureLocations = new HashSet<>();
        this.failureNames = new HashSet<>();
        this.contract = contract;
        this.skipSettingStatusInformation = skipSettingStatusInformation;
    }

    public GatherTestInformation(ProjectInspector inspector, boolean blockingStep, ContractForGatherTestInformation contract, boolean skipSettingStatusInformation) {
        this(inspector, blockingStep, contract, skipSettingStatusInformation, GatherTestInformation.class.getSimpleName());
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

    public int getNbRunningTests() {
        return nbRunningTests;
    }

    @Override
    protected StepStatus businessExecute() {
        this.getLogger().debug("Gathering test information...");

        File rootRepo = new File(this.getInspector().getJobStatus().getPomDirPath());
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
                    this.nbTotalTests += testSuite.getNumberOfTests();
                    this.nbSkippingTests += testSuite.getNumberOfSkipped();
                    this.nbRunningTests += testSuite.getNumberOfTests() - testSuite.getNumberOfSkipped();
                    this.nbErroringTests += testSuite.getNumberOfErrors();
                    this.nbFailingTests += testSuite.getNumberOfFailures();

                    if (testSuite.getNumberOfFailures() > 0 || testSuite.getNumberOfErrors() > 0) {
                        File failingModule = surefireDir.getParentFile().getParentFile();
                        this.failingModulePath = failingModule.getCanonicalPath();

                        if (!this.skipSettingStatusInformation) {
                            this.getInspector().getJobStatus().setFailingModulePath(this.failingModulePath);
                            this.writeProperty("failingModule", this.failingModulePath);
                            getLogger().info("Get the following failing module path: " + failingModulePath);
                        }

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
                                    failureLocation.addErroringMethod(testCase.getName());
                                } else {
                                    failureLocation.addFailingMethod(testCase.getName());
                                }
                            }
                        }
                    }
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
            this.getInspector().getJobStatus().setFailureLocations(this.failureLocations);

            Metrics metrics = this.getInspector().getJobStatus().getMetrics();
            metrics.setFailureNames(this.failureNames);
            metrics.setNbFailingTests(this.nbFailingTests);
            metrics.setNbRunningTests(this.nbTotalTests);
            metrics.setNbSkippingTests(this.nbSkippingTests);
            metrics.setNbErroringTests(this.nbErroringTests);
            metrics.setNbSucceedingTests(this.nbTotalTests - this.nbErroringTests - this.nbFailingTests);
        }

        return contract.shouldBeStopped(this);
    }

}
