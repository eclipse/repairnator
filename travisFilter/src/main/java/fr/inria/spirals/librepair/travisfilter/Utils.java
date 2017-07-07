package fr.inria.spirals.librepair.travisfilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by urli on 07/07/2017.
 */
public class Utils {

    private static Set<String> getFileContent(String path) throws IOException {
        HashSet<String> result = new HashSet<String>();
        File file = new File(path);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        while (reader.ready()) {
            result.add(reader.readLine().trim());
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: utils <new list> <old list 1> [old list 2, old list 3, ...]");
            System.exit(1);
        }

        Set<String> newList = getFileContent(args[0]);
        Set<String> oldList = new HashSet<String>();

        for (int i = 1; i < args.length; i++) {
            oldList.addAll(getFileContent(args[i]));
        }

        Set<String> newListWithoutDuplicates = new HashSet<String>();

        for (String s : newList) {
            if (!oldList.contains(s)) {
                newListWithoutDuplicates.add(s);
            }
        }

        for (String s : newListWithoutDuplicates) {
            System.out.println(s);
        }

        System.out.printf("Total : "+newListWithoutDuplicates.size());
    }
}
