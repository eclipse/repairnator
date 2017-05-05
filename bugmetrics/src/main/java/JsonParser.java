import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * Created by fermadeiral on 02/05/17.
 */
public class JsonParser {

    private String jsonFileFolderPath;

    // Metrics
    private int numberOfBugs;
    private Map<String, Integer> projectsToBugsMap = new HashMap<String, Integer>();
    private Map<String, Integer> exceptionTypesToCounterMap = new HashMap<String, Integer>();

    JsonParser(String jsonFileFolderPath) {
        this.jsonFileFolderPath = jsonFileFolderPath;
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

        JSONParser parser;
        try {
            for (int i = 0; i < files.length; i++) {
                parser = new JSONParser();

                JSONObject bug = (JSONObject) parser.parse(new FileReader(files[i]));

                this.numberOfBugs++;

                String project = (String) bug.get("bugRepo");
                if (!this.projectsToBugsMap.keySet().contains(project)) {
                    this.projectsToBugsMap.put(project, 0);
                }
                this.projectsToBugsMap.put(project, this.projectsToBugsMap.get(project)+1);

                JSONArray arrays = (JSONArray) bug.get("error-types");
                for (Object object : arrays) {
                    String errorType = object.toString().replace(":", "");
                    if (!this.exceptionTypesToCounterMap.containsKey(errorType)) {
                        this.exceptionTypesToCounterMap.put(errorType, 0);
                    }
                    this.exceptionTypesToCounterMap.put(errorType, this.exceptionTypesToCounterMap.get(errorType) + 1);
                }
            }

            System.out.println("#Bugs: " + this.numberOfBugs);

            System.out.println("#Projects: " + this.projectsToBugsMap.keySet().size());

            Map<String, Integer> sortedMapAsc = sortByComparator(this.exceptionTypesToCounterMap, false);
            System.out.println("#Error types: " + sortedMapAsc.keySet().size());
            for (Entry entry : sortedMapAsc.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, Integer> sortByComparator(Map<String, Integer> unsortMap, final boolean order) {
        List<Entry<String, Integer>> list = new LinkedList<Entry<String, Integer>>(unsortMap.entrySet());

        Collections.sort(list, new Comparator<Entry<String, Integer>>() {
            public int compare(Entry<String, Integer> o1,
                               Entry<String, Integer> o2) {
                if (order) {
                    return o1.getValue().compareTo(o2.getValue());
                } else {
                    return o2.getValue().compareTo(o1.getValue());

                }
            }
        });

        Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
        for (Entry<String, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

}
