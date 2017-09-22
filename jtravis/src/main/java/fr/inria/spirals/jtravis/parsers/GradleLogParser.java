package fr.inria.spirals.jtravis.parsers;

import fr.inria.spirals.jtravis.entities.TestsInformation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by urli on 22/02/2017.
 */
public class GradleLogParser extends JavaLogParser {
    //protected static final String GRADLE_LINE_PARSER = "^(:[\\w-]+)?:test[\\w-]*$";
    protected static final String GRADLE_LINE_PARSER = "(^BUILD SUCCESSFUL$)|(.*BUILD FAILED.*)";
    private static final String GRADLE_TEST_FAIL = "^(\\d+) tests completed, (\\d+) failed(, (\\d+) skipped)?$";

    @Override
    public TestsInformation parseLog(TravisFold outOfFolds) {
        Pattern pattern = Pattern.compile(GRADLE_TEST_FAIL, Pattern.MULTILINE);
        this.globalResults = new TestsInformation();

        for (String content : outOfFolds.getContent()) {
            Matcher matcher = pattern.matcher(content);

            if (matcher.matches()) {
                for (String s : outOfFolds.getContent()) {
                    matcher = pattern.matcher(s);

                    if (matcher.matches()) {
                        this.globalResults.setRunning(this.globalResults.getRunning() + Integer.parseInt(matcher.group(1)));
                        this.globalResults.setFailing(this.globalResults.getFailing() + Integer.parseInt(matcher.group(2)));

                        String skipped = matcher.group(4);
                        if (skipped != null) {
                            this.globalResults.setSkipping(this.globalResults.getSkipping() + Integer.parseInt(skipped));
                        }
                    }
                }
                this.globalResults.setPassing(this.globalResults.getRunning() - (this.globalResults.getFailing()+this.globalResults.getSkipping()));
            }
        }

        return this.globalResults;
    }

    @Override
    public List<TestsInformation> parseDetailedLog(TravisFold outOfFold) {
        this.globalResults = new TestsInformation();
        this.detailedResults = new ArrayList<TestsInformation>();
        this.detailedResults.clear();
        return this.detailedResults;
    }
}
