package fr.inria.spirals.jtravis.parsers;

import fr.inria.spirals.jtravis.entities.TestsInformation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by urli on 03/01/2017.
 *
 * This class is directly inspired from the work of authors of TravisTorrent project
 */
public class MavenLogParser extends JavaLogParser {

    protected static final String MVN_LINE_PATTERN = "^(-------------------------------------------------------|\\[INFO\\] Reactor Summary:)$";
    private static final String MVN_TESTS_PATTERN = " T E S T S";
    private static final String MVN_TEST_NUMBER_PATTERN = "Tests run: (\\d*), Failures: (\\d*), Errors: (\\d*)(, Skipped: (\\d*))?";

    public MavenLogParser() {
    }

    private void computePassingTests() {
        int notPassing = this.erroredTests+this.skippingTests+this.failingTests;
        this.passingTests = this.runningTests-notPassing;
    }

    private void parseTestLine(String line) {
        Pattern mvnTestNumberPattern = Pattern.compile(MVN_TEST_NUMBER_PATTERN);
        Matcher mvnTestNumberMatcher = mvnTestNumberPattern.matcher(line);

        if (mvnTestNumberMatcher.matches()) {
            int nbMatch = mvnTestNumberMatcher.groupCount();

            this.runningTests += Integer.parseInt(mvnTestNumberMatcher.group(1));
            this.failingTests += Integer.parseInt(mvnTestNumberMatcher.group(2));
            this.erroredTests += Integer.parseInt(mvnTestNumberMatcher.group(3));

            if (nbMatch == 5) {
                this.skippingTests += Integer.parseInt(mvnTestNumberMatcher.group(5));
            }
        }
    }

    @Override
    public TestsInformation parseLog(TravisFold outOfFold) {
        boolean beginTestHead = false;
        boolean inTestHead = false;
        boolean inTestBlock = false;

        Pattern mvnLinePattern = Pattern.compile(MVN_LINE_PATTERN);
        Pattern mvnTestHeadPattern = Pattern.compile(MVN_TESTS_PATTERN);

        for (String s : outOfFold.getContent()) {
            Matcher mvnLineMatcher = mvnLinePattern.matcher(s);

            if (!beginTestHead && !inTestHead && mvnLineMatcher.matches()) {
                beginTestHead = true;
                inTestBlock = false;
                continue;
            }

            if (beginTestHead && !inTestHead && mvnLineMatcher.matches()) {
                beginTestHead = false;
                inTestBlock = false;
                continue;
            }

            if (beginTestHead && inTestHead && mvnLineMatcher.matches()) {
                beginTestHead = false;
                inTestHead = false;
                inTestBlock = true;
                continue;
            }

            Matcher mvnTestHeadMatcher = mvnTestHeadPattern.matcher(s);

            if (beginTestHead && !inTestHead && mvnTestHeadMatcher.matches()) {
                inTestHead = true;
                inTestBlock = false;
                continue;
            }

            if (inTestBlock) {
                this.parseTestLine(s);
            }
        }

        this.computePassingTests();
        return this.createTestInformation();
    }
}
