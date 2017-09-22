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

    //protected static final String MVN_LINE_PATTERN = "^(-------------------------------------------------------|\\[INFO\\] Reactor Summary:)$";
    protected static final String MVN_LINE_PATTERN = "((\\[INFO\\].*)|(\\[ERROR\\].*)|((.*The command ){1}((\"mvn .*)|(\"./mvnw .*)){1}))";
    private static final String MVN_TESTS_PATTERN = ".* T E S T S";
    // | ([[1;34mINFO[m]  T E S T S)
    private static final String MVN_RESULTS_PATTERN = "(.*Results:)|(.*Results :)";
    private static final String MVN_TEST_NUMBER_PATTERN = ".*Tests run: (\\d*), Failures: (\\d*), Errors: (\\d*)(, Skipped: (\\d*))?";

    public MavenLogParser() {
    }

    private void computePassingTests() {
        int notPassing = this.erroredTests+this.skippingTests+this.failingTests;
        this.passingTests = this.runningTests-notPassing;
    }

    private Boolean parseTestLine(String line) {
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

            return  true;
        }
        else
            return false;
    }

    @Override
    public TestsInformation parseLog(TravisFold outOfFold) {
        boolean inTestBlock = false;
        boolean inTestResults = false;

        Pattern mvnTestHeadPattern = Pattern.compile(MVN_TESTS_PATTERN);
        Pattern mvnTestResultsPattern = Pattern.compile(MVN_RESULTS_PATTERN);
        for (String s : outOfFold.getContent()) {
            Matcher mvnTestResultsMatcher = mvnTestResultsPattern.matcher(s);
            Matcher mvnTestHeadMatcher = mvnTestHeadPattern.matcher(s);
            if (!inTestBlock && mvnTestHeadMatcher.matches()) {
                inTestBlock = true;
                continue;
            }
            if (inTestBlock && mvnTestResultsMatcher.matches()) {
                inTestResults = true;
                continue;
            }

            if (inTestResults) {
                if(this.parseTestLine(s)){
                    inTestBlock = false;
                    inTestResults = false;
                }

            }
        }

        this.computePassingTests();
        return this.createTestInformation();
    }
}
