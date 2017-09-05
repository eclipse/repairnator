import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by urli on 29/08/2017.
 */
public class CheckIfIdsWereInOtherFile {
    public static void main(String[] args) throws IOException {
        List<String> allIds = Files.readAllLines(new File(args[0]).toPath());
        List<String> findIds = Files.readAllLines(new File(args[1]).toPath());

        List<String> results = new ArrayList<>();
        for (String s : findIds) {
            if (allIds.contains(s)) {
                results.add(s);
            }
        }

        System.out.println(findIds.size()+" ids read, and got: "+results.size());
        System.out.println("Results:");
        System.out.println(StringUtils.join(results,"\n"));
    }
}
