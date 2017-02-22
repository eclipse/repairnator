package fr.inria.spirals.jtravis.parsers;

import fr.inria.spirals.jtravis.entities.TestsInformation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by urli on 22/02/2017.
 */
public class GradleLogParser extends JavaLogParser {
    protected static final String GRADLE_LINE_PARSER = "^:[\\w-]+:test[\\w-]*$";
    private static final String GRADLE_TEST_FAIL = "^(\\d+) tests completed, (\\d+) failed(, (\\d+) skipped)?$";

    @Override
    public TestsInformation parseLog(TravisFold outOfFolds) {
        Pattern pattern = Pattern.compile(GRADLE_TEST_FAIL, Pattern.MULTILINE);

        Matcher matcher = pattern.matcher(outOfFolds.getWholeContent());

        if (matcher.find()) {
            for (String s : outOfFolds.getContent()) {
                matcher = pattern.matcher(s);

                if (matcher.matches()) {
                    this.runningTests += Integer.parseInt(matcher.group(1));
                    this.failingTests += Integer.parseInt(matcher.group(2));

                    String skipped = matcher.group(4);
                    if (skipped != null) {
                        this.skippingTests += Integer.parseInt(skipped);
                    }
                }
            }
            this.passingTests = this.runningTests - (this.failingTests+this.skippingTests);
        }
        return this.createTestInformation();
    }
}
