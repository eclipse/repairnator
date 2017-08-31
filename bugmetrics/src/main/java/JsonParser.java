import model.Benchmark;
import model.Bug;
import model.ExceptionType;
import model.Project;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by fermadeiral on 02/05/17.
 */
public class JsonParser {

    private String jsonFileFolderPath;
    private String outputPath;
    private LauncherMode launcherMode;
    private OutputType outputType;
    private Date lookFromDate;
    private Date lookToDate;

    private Benchmark benchmark;

    private List<String> listOfProjectsA;
    private List<String> listOfProjectsB;
    private List<String> listOfProjectsG;

    private Pattern patternToGetDateFromBranchName = Pattern.compile("(.*)[-]\\d*[-](\\d*)[-]\\d*");

    JsonParser(String jsonFileFolderPath, String outputPath, LauncherMode launcherMode, OutputType outputType, Date lookFromDate, Date lookToDate) {
        this.jsonFileFolderPath = jsonFileFolderPath;
        this.outputPath = outputPath;
        this.launcherMode = launcherMode;
        this.outputType = outputType;
        this.lookFromDate = lookFromDate;
        this.lookToDate = lookToDate;

        this.listOfProjectsA = this.getListOfProjects("../librepair/bearsData/list_of_projectsA.txt");
        this.listOfProjectsB = this.getListOfProjects("../librepair/bearsData/list_of_projectsB.txt");
        this.listOfProjectsG = this.getListOfProjects("../librepair/bearsData/list_of_projectsG.txt");
    }

    public void run() {
        FileFilter filter = new FileFilter() {
            public boolean accept(File file) {
                return file.getName().endsWith(".json");
            }
        };
        File dir = new File(this.jsonFileFolderPath);
        File[] files = dir.listFiles(filter);

        System.out.println("# Files found: " + files.length);

        if (outputType == OutputType.METRICS) {
            calculateMetricsFromRepairnatorJsonFile(files);
        } else {
            getBranchNames(files);
        }
    }

    private boolean isInterestingBranch(String branchName) {
        if (launcherMode == LauncherMode.ALL_BRANCHES) {
            return true;
        }
        Matcher matcher = patternToGetDateFromBranchName.matcher(branchName);
        if (matcher.find()) {
            String dateStr = matcher.group(2);
            DateFormat formatter = new SimpleDateFormat("yyyyMMdd");
            Date date;
            try {
                date = formatter.parse(dateStr);
                if (date.after(lookFromDate) && date.before(lookToDate)) {
                    return true;
                }
                if (date.equals(lookFromDate)) {
                    return true;
                }
            } catch (java.text.ParseException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private void calculateMetricsFromRepairnatorJsonFile(File[] files) {
        this.benchmark = new Benchmark();
        try {
            for (int i = 0; i < files.length; i++) {
                String branchName = files[i].getName().replace("_repairnator.json", "");

                if (!isInterestingBranch(branchName)) {
                    continue;
                }

                JSONParser parser = new JSONParser();

                JSONObject jsonBug = (JSONObject) parser.parse(new FileReader(files[i]));

                String projectName = (String) jsonBug.get("repo");

                if (listOfProjectsA.contains(projectName) || listOfProjectsB.contains(projectName) || listOfProjectsG.contains(projectName)) {
                    Project project = this.benchmark.addProject(projectName);

                    Bug bug = this.benchmark.addBug(branchName, project);

                    String bugTypeDescription = (String) jsonBug.get("bugType");
                    this.benchmark.addBugType(bugTypeDescription);

                    JSONArray failingTestCases = (JSONArray) jsonBug.get("failing-test-cases");
                    Iterator failingTestCasesIterator = failingTestCases.iterator();
                    while (failingTestCasesIterator.hasNext()) {
                        JSONArray failures = (JSONArray) ((JSONObject) failingTestCasesIterator.next()).get("failures");
                        Iterator failuresIterator = failures.iterator();
                        while (failuresIterator.hasNext()) {
                            String exceptionTypeDescription = ((JSONObject) failuresIterator.next()).get("failureName").toString().replace(":", "");
                            ExceptionType exceptionType = this.benchmark.addExceptionType(exceptionTypeDescription);
                            bug.addExceptionType(exceptionType);
                        }
                    }

                    JSONObject metrics = (JSONObject) jsonBug.get("metrics");
                    JSONObject stepDurations = (JSONObject) metrics.get("StepsDurationsInSeconds");

                    int cloneRepositoryDuration = Integer.parseInt(stepDurations.get("CloneRepository").toString());
                    int checkoutBuggyBuildDuration;
                    int checkoutPatchedBuildDuration = Integer.parseInt(stepDurations.get("CheckoutPatchedBuild").toString());
                    int computeClasspathDuration = Integer.parseInt(stepDurations.get("ComputeClasspath").toString());
                    int computeSourceDirDuration = Integer.parseInt(stepDurations.get("ComputeSourceDir").toString());
                    int computeTestDirDuration = Integer.parseInt(stepDurations.get("ComputeTestDir").toString());
                    int resolveDependencyDuration = Integer.parseInt(stepDurations.get("ResolveDependency").toString());
                    int buildProjectBuildDuration = Integer.parseInt(stepDurations.get("BuildProjectBuild").toString());
                    int buildProjectPreviousBuildDuration;
                    int testProjectBuildDuration = Integer.parseInt(stepDurations.get("TestProjectBuild").toString());
                    int testProjectPreviousBuildDuration;
                    int gatherTestInformationBuildDuration = Integer.parseInt(stepDurations.get("GatherTestInformationBuild").toString());
                    int gatherTestInformationPreviousBuildDuration;
                    int nopolRepairDuration = Integer.parseInt(stepDurations.get("NopolRepair").toString());
                    int initRepoToPushDuration = Integer.parseInt(stepDurations.get("InitRepoToPush").toString());
                    int pushIncriminatedBuildDuration = Integer.parseInt(stepDurations.get("PushIncriminatedBuild").toString());
                    int commitPatchDuration = Integer.parseInt(stepDurations.get("CommitPatch").toString());

                    if (bugTypeDescription.equals("failing_passing")) {
                        checkoutBuggyBuildDuration = Integer.parseInt(stepDurations.get("CheckoutBuggyBuild").toString());
                        buildProjectPreviousBuildDuration = Integer.parseInt(stepDurations.get("BuildProjectPreviousBuild").toString());
                        testProjectPreviousBuildDuration = Integer.parseInt(stepDurations.get("TestProjectPreviousBuild").toString());
                        gatherTestInformationPreviousBuildDuration = Integer.parseInt(stepDurations.get("GatherTestInformationPreviousBuild").toString());
                    } else {
                        checkoutBuggyBuildDuration = Integer.parseInt(stepDurations.get("CheckoutBuggyBuildSourceCode").toString());
                        buildProjectPreviousBuildDuration = Integer.parseInt(stepDurations.get("BuildProjectPreviousBuildSourceCode").toString());
                        testProjectPreviousBuildDuration = Integer.parseInt(stepDurations.get("TestProjectPreviousBuildSourceCode").toString());
                        gatherTestInformationPreviousBuildDuration = Integer.parseInt(stepDurations.get("GatherTestInformationPreviousBuildSourceCode").toString());
                    }

                    int totalDuration = cloneRepositoryDuration + checkoutBuggyBuildDuration + checkoutPatchedBuildDuration
                            + computeClasspathDuration + computeSourceDirDuration + computeTestDirDuration
                            + resolveDependencyDuration + buildProjectBuildDuration + buildProjectPreviousBuildDuration
                            + testProjectBuildDuration + testProjectPreviousBuildDuration + gatherTestInformationBuildDuration
                            + gatherTestInformationPreviousBuildDuration + nopolRepairDuration + initRepoToPushDuration
                            + pushIncriminatedBuildDuration + commitPatchDuration;

                    bug.addStepToDuration("cloneRepository", cloneRepositoryDuration);
                    bug.addStepToDuration("checkoutBuggyBuild", checkoutBuggyBuildDuration);
                    bug.addStepToDuration("checkoutPatchedBuild", checkoutPatchedBuildDuration);
                    bug.addStepToDuration("computeClasspath", computeClasspathDuration);
                    bug.addStepToDuration("computeSourceDir", computeSourceDirDuration);
                    bug.addStepToDuration("computeTestDir", computeTestDirDuration);
                    bug.addStepToDuration("resolveDependency", resolveDependencyDuration);
                    bug.addStepToDuration("buildProjectBuild", buildProjectBuildDuration);
                    bug.addStepToDuration("buildProjectPreviousBuild", buildProjectPreviousBuildDuration);
                    bug.addStepToDuration("testProjectBuild", testProjectBuildDuration);
                    bug.addStepToDuration("testProjectPreviousBuild", testProjectPreviousBuildDuration);
                    bug.addStepToDuration("gatherTestInformationBuild", gatherTestInformationBuildDuration);
                    bug.addStepToDuration("gatherTestInformationPreviousBuild", gatherTestInformationPreviousBuildDuration);
                    bug.addStepToDuration("nopolRepair", nopolRepairDuration);
                    bug.addStepToDuration("initRepoToPush", initRepoToPushDuration);
                    bug.addStepToDuration("pushIncriminatedBuild", pushIncriminatedBuildDuration);
                    bug.addStepToDuration("commitPatch", commitPatchDuration);
                    bug.addStepToDuration("totalDuration", totalDuration);
                }
            }

            System.out.println("# Bugs: " + this.benchmark.getBugs().size());

            System.out.println("# Projects: " + this.benchmark.getProjects().size());

            System.out.println("\nBug types: ");
            for (Entry entry : this.benchmark.getBugTypeToCounterMap().entrySet()) {
                System.out.println("# " + entry.getKey() + ": " + entry.getValue());
            }

            System.out.println("\n# Distinct exception types: " + this.benchmark.getExceptionTypes().size());

            System.out.println("\nException types out:");
            for (Entry<String, Integer> exceptionTypeOutToCounter : this.benchmark.getExceptionTypesOutToCounterMap().entrySet()) {
                System.out.println(exceptionTypeOutToCounter.getKey() + ": " + exceptionTypeOutToCounter.getValue());
            }

            createNumberOfBugsByProjectsCsvFile();
            createDistributionExceptionTypeOccurrencesByProjectsCsvFile();
            createStepTotalDurationByProjectsCsvFile();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void getBranchNames(File[] files) {
        int totalBranchOutput = 0;
        for (int i = 0; i < files.length; i++) {
            String branchName = files[i].getName().replace("_repairnator.json", "");

            if (!isInterestingBranch(branchName)) {
                continue;
            }

            System.out.println(branchName);
            totalBranchOutput++;
        }

        System.out.println("# Total branches output: "+totalBranchOutput);
    }

    private void createNumberOfBugsByProjectsCsvFile() {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(outputPath + "/number-of-bugs-by-projects.csv");

            Collections.sort(this.benchmark.getProjects());

            String fileHeader = "project,number of bugs\n";
            fileWriter.append(fileHeader);

            String line;
            for (Project project : this.benchmark.getProjects()) {
                line = project.getName() + "," + project.getNumberOfBugs() + "\n";
                fileWriter.append(line);
            }

            System.out.println("\nNumber of bugs by projects CSV file was created successfully");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createDistributionExceptionTypeOccurrencesByProjectsCsvFile() {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(outputPath + "/distribution-exception-type-occurrences-by-projects.csv");

            Collections.sort(this.benchmark.getProjects());

            String fileHeader = "exception type";
            for (Project project : this.benchmark.getProjects()) {
                fileHeader += "," + project.getName();
            }
            fileHeader += ",sum\n";
            fileWriter.append(fileHeader);

            String line;
            for (ExceptionType exceptionType : this.benchmark.getExceptionTypes()) {
                String exceptionTypeDescription = exceptionType.getDescription();
                line = exceptionTypeDescription + ",";
                int sumForExceptionType = 0;
                for (Project project : this.benchmark.getProjects()) {
                    if (project.containsExceptionType(exceptionTypeDescription)) {
                        int number = project.getOccurrenceCounterOfExceptionType(exceptionTypeDescription);
                        line += number + ",";
                        sumForExceptionType += number;
                    } else {
                        line += "0,";
                    }
                }
                line += sumForExceptionType + "\n";
                fileWriter.append(line);
            }
            line = "sum";
            for (Project project : this.benchmark.getProjects()) {
                line += "," + project.getOccurrenceCounterOfAllExceptionTypes();
            }
            fileWriter.append(line);

            System.out.println("\nDistribution exception type occurrences by projects CSV file was created successfully");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createStepTotalDurationByProjectsCsvFile() {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(outputPath + "/step-total-duration-by-projects.csv");

            Collections.sort(this.benchmark.getProjects());

            String fileHeader = "project,average,min,max\n";
            fileWriter.append(fileHeader);

            String line;
            for (Project project : this.benchmark.getProjects()) {
                line = project.getName() + ",";
                int sum = 0, min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
                for (Bug bug : project.getBugs()) {
                    int totalDuration = bug.getStepDuration("totalDuration");
                    sum += totalDuration;
                    min = (totalDuration < min) ? totalDuration : min;
                    max = (totalDuration > max) ? totalDuration : max;
                }
                line += sum / project.getNumberOfBugs() + "," + min + "," + max + "\n";
                fileWriter.append(line);
            }

            System.out.println("\nStep total duration by projects CSV file was created successfully");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private List<String> getListOfProjects(String filePath) {
        List<String> listOfProjects = new ArrayList<String>();

        BufferedReader in;
        try {
            in = new BufferedReader(new FileReader(filePath));
            String line;
            while((line = in.readLine()) != null) {
                listOfProjects.add(line);
            }
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return listOfProjects;
    }

}
