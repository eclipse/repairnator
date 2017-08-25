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

    private Pattern patternToGetDateFromBranchName = Pattern.compile("(.*)[-]\\d*[-](\\d*)[-]\\d*");

    private Map<String, Integer> errorTypesOut = new HashMap<String, Integer>();

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

        this.errorTypesOut.put("skipped", 0);
        this.errorTypesOut.put("Wanted but not invoked", 0);
        this.errorTypesOut.put("Condition not satisfied", 0);
    }

    public void run() {
        FileFilter filter = new FileFilter() {
            public boolean accept(File file) {
                return file.getName().endsWith(".json");
            }
        };
        File dir = new File(this.jsonFileFolderPath);
        File[] files = dir.listFiles(filter);

        System.out.println("#Files found: " + files.length);

        if (outputType == OutputType.METRICS) {
            calculateMetrics(files);
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

    private void calculateMetrics(File[] files) {
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
                            String errorType = ((JSONObject) failuresIterator.next()).get("failureName").toString().replace(":", "");
                            if (errorTypesOut.keySet().contains(errorType)) {
                                errorTypesOut.put(errorType, errorTypesOut.get(errorType) + 1);
                            } else {
                                if (!this.exceptionTypesToProjectsToCounterMap.containsKey(errorType)) {
                                    this.exceptionTypesToProjectsToCounterMap.put(errorType, new HashMap<String, Integer>());
                                }
                                if (!this.exceptionTypesToProjectsToCounterMap.get(errorType).containsKey(projectName)) {
                                    this.exceptionTypesToProjectsToCounterMap.get(errorType).put(projectName, 0);
                                }
                                this.exceptionTypesToProjectsToCounterMap.get(errorType).put(projectName, this.exceptionTypesToProjectsToCounterMap.get(errorType).get(projectName) + 1);
                            }
                            if (!projectsToExceptionTypesToCounterMap.containsKey(projectName)) {
                                projectsToExceptionTypesToCounterMap.put(projectName, new HashMap<String, Integer>());
                            }
                            if (!projectsToExceptionTypesToCounterMap.get(projectName).containsKey(errorType)) {
                                projectsToExceptionTypesToCounterMap.get(projectName).put(errorType, 0);
                            } else {
                                this.projectsToExceptionTypesToCounterMap.get(projectName).put(errorType, this.projectsToExceptionTypesToCounterMap.get(projectName).get(errorType) + 1);
                            }
                        }
                    }
                }
            }

            System.out.println("#Bugs: " + this.numberOfBugs);

            System.out.println("#Projects: " + this.projectsToBugsMap.keySet().size());

            System.out.println("Bug types: ");
            for (Entry entry : this.bugTypesToCounterMap.entrySet()) {
                System.out.println("#" + entry.getKey() + ": " + entry.getValue());
            }

            System.out.println("#Distinct error types: " + this.exceptionTypesToProjectsToCounterMap.keySet().size());

            System.out.println();
            for (String errorTypeOut : errorTypesOut.keySet()) {
                System.out.println(errorTypeOut + ": " + errorTypesOut.get(errorTypeOut));
            }

            writeErrorTypeDistributionCsvFile();
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

        System.out.println("#Total branches output: "+totalBranchOutput);
    }

    private void writeErrorTypeDistributionCsvFile() {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(outputPath + "/distribution-error-types-by-projects.csv");

            Map<String, Integer> sortedProjectsToBugsMap = sortByComparator(this.projectsToBugsMap);

            String fileHeader = "error type";
            for (String projectName : sortedProjectsToBugsMap.keySet()) {
                fileHeader += "," + projectName;
            }
            fileHeader += ",sum";
            fileWriter.append(fileHeader);

            fileWriter.append("\n");

            String line;
            for (String exceptionType : this.exceptionTypesToProjectsToCounterMap.keySet()) {
                int sumForErrorType = 0;
                line = exceptionType + ",";
                for (String projectName : sortedProjectsToBugsMap.keySet()) {
                    if (this.exceptionTypesToProjectsToCounterMap.get(exceptionType).containsKey(projectName)) {
                        int number = this.exceptionTypesToProjectsToCounterMap.get(exceptionType).get(projectName);
                        line += number + ",";
                        sumForErrorType += number;
                    } else {
                        line += "0,";
                    }
                }
                line += sumForErrorType;
                fileWriter.append(line);
                fileWriter.append("\n");
            }
            line = "sum";
            for (String projectName : sortedProjectsToBugsMap.keySet()) {
                int sumForProject = 0;
                Map<String, Integer> exceptionTypesToCounter = projectsToExceptionTypesToCounterMap.get(projectName);
                for (Integer counter : exceptionTypesToCounter.values()) {
                    sumForProject += counter;
                }
                line += "," + sumForProject;
            }
            fileWriter.append(line);

            System.out.println("CSV file was created successfully");
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

    private static Map<String, Integer> sortByComparator(Map<String, Integer> unsortMap) {
        List<Entry<String, Integer>> list = new LinkedList<Entry<String, Integer>>(unsortMap.entrySet());

        Collections.sort(list, new Comparator<Entry<String, Integer>>() {
            public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
                return o2.getKey().compareTo(o1.getKey());
            }
        });

        Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
        for (Entry<String, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

}
