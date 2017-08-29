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

    private List<String> listOfProjectsA;
    private List<String> listOfProjectsB;
    private List<String> listOfProjectsG;

    // Metrics
    private int numberOfBugs;
    private Map<String, Integer> bugTypesToCounterMap = new HashMap<String, Integer>();
    private Map<String, Integer> projectsToBugsMap = new HashMap<String, Integer>();
    private Map<String, Map<String, Integer>> projectsToExceptionTypesToCounterMap = new HashMap<String, Map<String, Integer>>();
    private Map<String, Map<String, Integer>> exceptionTypesToProjectsToCounterMap = new HashMap<String, Map<String, Integer>>();
    private Map<String, List<Map<String, Integer>>> projectsToStepsToDurationsMap = new HashMap<String, List<Map<String, Integer>>>();

    private Pattern patternToGetDateFromBranchName = Pattern.compile("(.*)[-]\\d*[-](\\d*)[-]\\d*");

    private Map<String, Integer> exceptionTypesOut = new HashMap<String, Integer>();

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

        this.exceptionTypesOut.put("skipped", 0);
        this.exceptionTypesOut.put("Wanted but not invoked", 0);
        this.exceptionTypesOut.put("Condition not satisfied", 0);
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
        try {
            for (int i = 0; i < files.length; i++) {
                String branchName = files[i].getName().replace("_repairnator.json", "");

                if (!isInterestingBranch(branchName)) {
                    continue;
                }

                JSONParser parser = new JSONParser();

                JSONObject bug = (JSONObject) parser.parse(new FileReader(files[i]));

                String projectName = (String) bug.get("repo");

                if (listOfProjectsA.contains(projectName) || listOfProjectsB.contains(projectName) || listOfProjectsG.contains(projectName)) {
                    this.numberOfBugs++;

                    if (!this.projectsToBugsMap.keySet().contains(projectName)) {
                        this.projectsToBugsMap.put(projectName, 0);
                    }
                    this.projectsToBugsMap.put(projectName, this.projectsToBugsMap.get(projectName) + 1);

                    String bugType = (String) bug.get("bugType");
                    if (!this.bugTypesToCounterMap.keySet().contains(bugType)) {
                        this.bugTypesToCounterMap.put(bugType, 0);
                    }
                    this.bugTypesToCounterMap.put(bugType, this.bugTypesToCounterMap.get(bugType) + 1);

                    JSONArray failingTestCases = (JSONArray) bug.get("failing-test-cases");
                    Iterator failingTestCasesIterator = failingTestCases.iterator();
                    while (failingTestCasesIterator.hasNext()) {
                        JSONArray failures = (JSONArray) ((JSONObject) failingTestCasesIterator.next()).get("failures");
                        Iterator failuresIterator = failures.iterator();
                        while (failuresIterator.hasNext()) {
                            String exceptionType = ((JSONObject) failuresIterator.next()).get("failureName").toString().replace(":", "");
                            if (exceptionTypesOut.keySet().contains(exceptionType)) {
                                exceptionTypesOut.put(exceptionType, exceptionTypesOut.get(exceptionType) + 1);
                            } else {
                                if (!projectsToExceptionTypesToCounterMap.containsKey(projectName)) {
                                    projectsToExceptionTypesToCounterMap.put(projectName, new HashMap<String, Integer>());
                                }
                                if (!projectsToExceptionTypesToCounterMap.get(projectName).containsKey(exceptionType)) {
                                    projectsToExceptionTypesToCounterMap.get(projectName).put(exceptionType, 0);
                                } else {
                                    this.projectsToExceptionTypesToCounterMap.get(projectName).put(exceptionType, this.projectsToExceptionTypesToCounterMap.get(projectName).get(exceptionType) + 1);
                                }

                                if (!this.exceptionTypesToProjectsToCounterMap.containsKey(exceptionType)) {
                                    this.exceptionTypesToProjectsToCounterMap.put(exceptionType, new HashMap<String, Integer>());
                                }
                                if (!this.exceptionTypesToProjectsToCounterMap.get(exceptionType).containsKey(projectName)) {
                                    this.exceptionTypesToProjectsToCounterMap.get(exceptionType).put(projectName, 0);
                                }
                                this.exceptionTypesToProjectsToCounterMap.get(exceptionType).put(projectName, this.exceptionTypesToProjectsToCounterMap.get(exceptionType).get(projectName) + 1);
                            }
                        }
                    }

                    Map<String, Integer> stepsToDurationsMap = new HashMap<String, Integer>();

                    JSONObject metrics = (JSONObject) bug.get("metrics");
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

                    if (bugType.equals("failing_passing")) {
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

                    stepsToDurationsMap.put("cloneRepository", cloneRepositoryDuration);
                    stepsToDurationsMap.put("checkoutBuggyBuild", checkoutBuggyBuildDuration);
                    stepsToDurationsMap.put("checkoutPatchedBuild", checkoutPatchedBuildDuration);
                    stepsToDurationsMap.put("computeClasspath", computeClasspathDuration);
                    stepsToDurationsMap.put("computeSourceDir", computeSourceDirDuration);
                    stepsToDurationsMap.put("computeTestDir", computeTestDirDuration);
                    stepsToDurationsMap.put("resolveDependency", resolveDependencyDuration);
                    stepsToDurationsMap.put("buildProjectBuild", buildProjectBuildDuration);
                    stepsToDurationsMap.put("buildProjectPreviousBuild", buildProjectPreviousBuildDuration);
                    stepsToDurationsMap.put("testProjectBuild", testProjectBuildDuration);
                    stepsToDurationsMap.put("testProjectPreviousBuild", testProjectPreviousBuildDuration);
                    stepsToDurationsMap.put("gatherTestInformationBuild", gatherTestInformationBuildDuration);
                    stepsToDurationsMap.put("gatherTestInformationPreviousBuild", gatherTestInformationPreviousBuildDuration);
                    stepsToDurationsMap.put("nopolRepair", nopolRepairDuration);
                    stepsToDurationsMap.put("initRepoToPush", initRepoToPushDuration);
                    stepsToDurationsMap.put("pushIncriminatedBuild", pushIncriminatedBuildDuration);
                    stepsToDurationsMap.put("commitPatch", commitPatchDuration);
                    stepsToDurationsMap.put("totalDuration", totalDuration);

                    if (!this.projectsToStepsToDurationsMap.containsKey(projectName)) {
                        this.projectsToStepsToDurationsMap.put(projectName, new ArrayList<>());
                    }
                    this.projectsToStepsToDurationsMap.get(projectName).add(stepsToDurationsMap);
                }
            }

            System.out.println("# Bugs: " + this.numberOfBugs);

            System.out.println("# Projects: " + this.projectsToBugsMap.keySet().size());

            System.out.println("\nBug types: ");
            for (Entry entry : this.bugTypesToCounterMap.entrySet()) {
                System.out.println("# " + entry.getKey() + ": " + entry.getValue());
            }

            System.out.println("\n# Distinct exception types: " + this.exceptionTypesToProjectsToCounterMap.keySet().size());

            System.out.println("\nException types out:");
            for (String exceptionTypeOut : exceptionTypesOut.keySet()) {
                System.out.println(exceptionTypeOut + ": " + exceptionTypesOut.get(exceptionTypeOut));
            }

            createDistributionExceptionTypesByProjectsCsvFile();
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

    private void createDistributionExceptionTypesByProjectsCsvFile() {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(outputPath + "/distribution-exception-types-by-projects.csv");

            Map<String, Map<String, Integer>> sortedProjectsToExceptionTypesToCounterMap = sortMap(this.projectsToExceptionTypesToCounterMap);

            String fileHeader = "exception type";
            for (String projectName : sortedProjectsToExceptionTypesToCounterMap.keySet()) {
                fileHeader += "," + projectName;
            }
            fileHeader += ",sum";
            fileWriter.append(fileHeader);
            fileWriter.append("\n");

            String line;
            for (String exceptionType : this.exceptionTypesToProjectsToCounterMap.keySet()) {
                line = exceptionType + ",";
                int sumForExceptionType = 0;
                for (String projectName : sortedProjectsToExceptionTypesToCounterMap.keySet()) {
                    if (this.exceptionTypesToProjectsToCounterMap.get(exceptionType).containsKey(projectName)) {
                        int number = this.exceptionTypesToProjectsToCounterMap.get(exceptionType).get(projectName);
                        line += number + ",";
                        sumForExceptionType += number;
                    } else {
                        line += "0,";
                    }
                }
                line += sumForExceptionType;
                fileWriter.append(line);
                fileWriter.append("\n");
            }
            line = "sum";
            for (String projectName : sortedProjectsToExceptionTypesToCounterMap.keySet()) {
                int sumForProject = 0;
                Map<String, Integer> exceptionTypesToCounter = projectsToExceptionTypesToCounterMap.get(projectName);
                for (Integer counter : exceptionTypesToCounter.values()) {
                    sumForProject += counter;
                }
                line += "," + sumForProject;
            }
            fileWriter.append(line);

            System.out.println("\nDistribution exception types by projects CSV file was created successfully");
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

            Map<String, Map<String, Integer>> sortedProjectsToExceptionTypesToCounterMap = sortMap(this.projectsToExceptionTypesToCounterMap);

            String fileHeader = "project,average,min,max\n";
            fileWriter.append(fileHeader);

            String line;
            for (String projectName : sortedProjectsToExceptionTypesToCounterMap.keySet()) {
                line = projectName + ",";
                int sum = 0, min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
                for (Map<String, Integer> stepsToDurationsMap : projectsToStepsToDurationsMap.get(projectName)) {
                    int totalDuration = stepsToDurationsMap.get("totalDuration");
                    sum += totalDuration;
                    min = (totalDuration < min) ? totalDuration : min;
                    max = (totalDuration > max) ? totalDuration : max;
                }
                line += sum / projectsToStepsToDurationsMap.get(projectName).size() + "," + min + "," + max + "\n";
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

    private static Map<String, Map<String, Integer>> sortMap(Map<String, Map<String, Integer>> unsortMap) {
        List<Entry<String, Map<String, Integer>>> list = new LinkedList<Entry<String, Map<String, Integer>>>(unsortMap.entrySet());

        Collections.sort(list, (o1, o2) -> o1.getKey().compareTo(o2.getKey()));

        Map<String, Map<String, Integer>> sortedMap = new LinkedHashMap<String, Map<String, Integer>>();
        for (Entry<String, Map<String, Integer>> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

}
