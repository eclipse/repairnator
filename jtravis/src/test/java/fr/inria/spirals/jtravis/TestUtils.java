package fr.inria.spirals.jtravis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by urli on 22/02/2017.
 */
public class TestUtils {

    public static String readFile(String path) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(path));

        String result = "";

        while (reader.ready()) {
            result += reader.readLine()+"\n";
        }
        return result;
    }
}
