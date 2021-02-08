package fr.inria.spirals.repairnator.process.step.gatherinfo;

import exceptionparser.StackTrace;
import exceptionparser.StackTraceElement;
import exceptionparser.StackTraceParser;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.properties.tests.*;
import fr.inria.spirals.repairnator.process.inspectors.properties.Properties;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.process.testinformation.BugType;
import fr.inria.spirals.repairnator.process.testinformation.ErrorType;
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
    private int nbRunningTests;
    private int nbPassingTests;
    private int nbFailingTests;
    private int nbErroringTests;
    private int nbSkippingTests;

    private Set<FailureLocation> failureLocations;
    private Set<String> failureNames;
    private String failingModulePath;
    private boolean skipSettingStatusInformation;

    private ContractForGatherTestInformation contract;

    /**
     * This step intends to gather test information produced during {@link fr.inria.spirals.repairnator.process.step.TestProject} step.
     * A contract is asked to know if the step should find test failure or if everything must pass. Moreover, this step send by default
     * information to the JobStatus, but it can be skipping by setting the boolean to true.
     *
     * @param inspector                    The inspector responsible of the step pipeline
     * @param contract                     The contract to know in which case the step should stop the pipeline
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
        this.getLogger().debug("Gathering test information from surefire reports...");

        this.getLogger().debug("Contract: " + this.contract.getClass().getSimpleName());

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


        JobStatus jobStatus = this.getInspector().getJobStatus();
        for (File surefireDir : surefireDirs) {
            SurefireReportParser surefireReportParser = new SurefireReportParser(Arrays.asList(new File[]{surefireDir}),
                    Locale.ENGLISH, null);
            try {
                List<ReportTestSuite> testSuites = surefireReportParser.parseXMLReportFiles();
                for (ReportTestSuite testSuite : testSuites) {
                    this.nbTotalTests += testSuite.getNumberOfTests();
                    int runningTests = testSuite.getNumberOfTests() - testSuite.getNumberOfSkipped();
                    this.nbRunningTests += runningTests;
                    this.nbPassingTests += runningTests - testSuite.getNumberOfFailures() - testSuite.getNumberOfErrors();
                    this.nbFailingTests += testSuite.getNumberOfFailures();
                    this.nbErroringTests += testSuite.getNumberOfErrors();
                    this.nbSkippingTests += testSuite.getNumberOfSkipped();

                    if (testSuite.getNumberOfFailures() > 0 || testSuite.getNumberOfErrors() > 0) {
                        File failingModule = surefireDir.getParentFile().getParentFile();
                        this.failingModulePath = failingModule.getCanonicalPath();

                        if (!this.skipSettingStatusInformation) {
                            jobStatus.setFailingModulePath(this.failingModulePath);
                            getLogger().info("Get the following failing module path: " + failingModulePath);

                            Properties properties = jobStatus.getProperties();
                            FailingClass failingClass = properties.getTests().addFailingClass(testSuite.getFullClassName());
                            failingClass.setNumberRunning(testSuite.getNumberOfTests() - testSuite.getNumberOfSkipped());
                            failingClass.setNumberPassing(testSuite.getNumberOfTests() - testSuite.getNumberOfSkipped() - testSuite.getNumberOfFailures() - testSuite.getNumberOfErrors());
                            failingClass.setNumberFailing(testSuite.getNumberOfFailures());
                            failingClass.setNumberErroring(testSuite.getNumberOfErrors());
                            failingClass.setNumberSkipping(testSuite.getNumberOfSkipped());
                        }

                        for (ReportTestCase testCase : testSuite.getTestCases()) {
                            if (testCase.hasFailure() || testCase.hasError()) {

                                // sometimes surefire reports a failureType on the form:
                                // "java.lang.NullPointerException:" we should avoid this case
                                String failureType = testCase.getFailureType();

                                if (failureType.endsWith(":")) {
                                    failureType = failureType.substring(0, failureType.length() - 1);
                                }

                                // Find FailureLocation or create a new one
                                this.failureNames.add(failureType);
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

                                if (testCase.hasError()) {
                                    ErrorType typeTof = new ErrorType(failureType, testCase.getFailureDetail());

                                    try {
                                        // Descend stack trace
                                        StackTrace stackTrace = StackTraceParser.parse(testCase.getFailureDetail());
                                        StackTrace causedBy = stackTrace.getCausedBy();
                                        while (causedBy != null) {
                                            stackTrace = causedBy;
                                            causedBy = stackTrace.getCausedBy();
                                        }

                                        // Get source files where error has occurred
                                        Set<File> classFiles = new HashSet<>();
                                        Set<File> packageFiles = new HashSet<>();
                                        Set<File> stackFiles = new HashSet<>();
                                        boolean onePass = false;
                                        for (StackTraceElement stackTraceElement : stackTrace.getElements()) {
                                            String path = stackTraceElement.getMethod().substring(0, stackTraceElement.getMethod().lastIndexOf(".")).replace(".", "/") + ".java";
                                            if (path.contains("$")) {
                                                path = path.substring(0, path.indexOf("$")) + ".java";
                                            }
                                            String packagePath = ".";
                                            if (path.lastIndexOf("/") != -1) {
                                                packagePath = path.substring(0, path.lastIndexOf("/"));
                                            }
                                            for (File sourcedir : this.getInspector().getJobStatus().getRepairSourceDir()) {
                                                File file = new File(sourcedir.getAbsolutePath() + "/" + path);
                                                File packageFile = new File(sourcedir.getAbsolutePath() + "/" + packagePath);
                                                if (file.exists()) {
                                                    if (!onePass) {
                                                        this.getLogger().debug("classFiles: " + file.getAbsolutePath());
                                                        classFiles.add(file);
                                                    }
                                                    this.getLogger().debug("stackFiles: " + file.getAbsolutePath());
                                                    stackFiles.add(file);
                                                }
                                                if (packageFile.exists()) {
                                                    this.getLogger().debug("packageFiles: " + packageFile.getAbsolutePath());
                                                    packageFiles.add(packageFile);
                                                }
                                                if (file.exists() || packageFile.exists())
                                                    break;
                                            }
                                            onePass = true;
                                        }

                                        typeTof.addClassFiles(classFiles);
                                        typeTof.addPackageFiles(packageFiles);
                                        typeTof.addStackFiles(stackFiles);
                                    } catch (StackTraceParser.ParseException e) {
                                        this.addStepError("Error while parsing stack trace.", e);
                                    }

                                    failureLocation.addErroringMethod(testCase.getName(), typeTof);
                                } else {
                                    FailureType typeTof = new FailureType(failureType, testCase.getFailureDetail());
                                    failureLocation.addFailingMethod(testCase.getName(), typeTof);
                                }

                                if (!this.skipSettingStatusInformation) {
                                    Properties properties = this.getInspector().getJobStatus().getProperties();
                                    properties.getTests().getOverallMetrics().addFailure(failureType, testCase.hasError());

                                    FailureDetail failureDetail = new FailureDetail();
                                    failureDetail.setTestClass(failureLocation.getClassName());
                                    failureDetail.setTestMethod(testCase.getName());
                                    failureDetail.setFailureName(failureType);
                                    failureDetail.setDetail(testCase.getFailureDetail());
                                    failureDetail.setError(testCase.hasError());
                                    properties.getTests().addFailureDetail(failureDetail);
                                }
                            }
                        }
                    }
                }
            } catch (MavenReportException e) {
                this.addStepError("Error while parsing files to get test information.", e);
            } catch (IOException e) {
                this.addStepError("Error while getting the failing module path.", e);
            }
        }

        if (!this.skipSettingStatusInformation) {
            jobStatus.setFailureLocations(this.failureLocations);

            Properties properties = jobStatus.getProperties();
            Tests tests = properties.getTests();
            OverallMetrics overallMetrics = tests.getOverallMetrics();
            overallMetrics.setNumberRunning(this.nbRunningTests);
            overallMetrics.setNumberPassing(this.nbPassingTests);
            overallMetrics.setNumberFailing(this.nbFailingTests);
            overallMetrics.setNumberErroring(this.nbErroringTests);
            overallMetrics.setNumberSkipping(this.nbSkippingTests);
        }

        this.getLogger().info("---Test results---");
        this.getLogger().info("   Total tests: " + this.nbTotalTests);
        this.getLogger().info("   Tests run: " + this.nbRunningTests);
        this.getLogger().info("   Tests passing: " + this.nbPassingTests);
        this.getLogger().info("   Failures: " + this.nbFailingTests);
        this.getLogger().info("   Errors: " + this.nbErroringTests);
        this.getLogger().info("   Skipped: " + this.nbSkippingTests);

        return contract.shouldBeStopped(this);
    }

}
