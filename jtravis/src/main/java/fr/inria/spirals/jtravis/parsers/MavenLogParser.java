package fr.inria.spirals.jtravis.parsers;

import fr.inria.spirals.jtravis.entities.TestsInformation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by urli on 03/01/2017.
 *
 * This class is directly inspired from the work of authors of TravisTorrent project
 */
public class MavenLogParser extends JavaLogParser {

    //protected static final String MVN_LINE_PATTERN = "^(-------------------------------------------------------|\\[INFO\\] Reactor Summary:)$";
    protected static final String MVN_LINE_PATTERN = "((\\[\u001B\\[1;34mINFO\u001B\\[m\\].*)|(\\[INFO\\].*)|(\\[ERROR\\].*)|((.*The command ){1}((\"mvn .*)|(\"./mvnw .*)){1}))";
    private static final String MVN_TESTS_PATTERN = ".* T E S T S";
    // | ([[1;34mINFO[m]  T E S T S)
    private static final String MVN_RESULTS_PATTERN = "(.*Results:)|(.*Results :)";
    private static final String MVN_TEST_NUMBER_PATTERN = ".*Tests run: (\\d*), Failures: (\\d*), Errors: (\\d*)(, Skipped: (\\d*))?";

    public MavenLogParser() {
    }

    private void computePassingTests() {
        int notPassing = this.globalResults.getErrored()+this.globalResults.getSkipping()+this.globalResults.getFailing();
        this.globalResults.setPassing(this.globalResults.getRunning()- notPassing);
    }

    private Boolean parseTestLine(String line) {
        Pattern mvnTestNumberPattern = Pattern.compile(MVN_TEST_NUMBER_PATTERN);
        Matcher mvnTestNumberMatcher = mvnTestNumberPattern.matcher(line);

        if (mvnTestNumberMatcher.matches()) {
            int nbMatch = mvnTestNumberMatcher.groupCount();
            TestsInformation res = new TestsInformation();

            this.globalResults.setRunning(this.globalResults.getRunning() + Integer.parseInt(mvnTestNumberMatcher.group(1)));
            res.setRunning(Integer.parseInt(mvnTestNumberMatcher.group(1)));
            this.globalResults.setFailing(this.globalResults.getFailing() + Integer.parseInt(mvnTestNumberMatcher.group(2)));
            res.setFailing(Integer.parseInt(mvnTestNumberMatcher.group(2)));
            this.globalResults.setErrored(this.globalResults.getErrored() + Integer.parseInt(mvnTestNumberMatcher.group(3)));
            res.setErrored(Integer.parseInt(mvnTestNumberMatcher.group(3)));

            if (nbMatch == 5) {
                this.globalResults.setSkipping(this.globalResults.getSkipping() + Integer.parseInt(mvnTestNumberMatcher.group(5)));
                res.setSkipping(Integer.parseInt(mvnTestNumberMatcher.group(5)));
            }

            res.setPassing(res.getRunning() - (res.getFailing()+res.getErrored()+res.getSkipping()));
            this.detailedResults.add(res);
            return  true;
        }
        else
            return false;
    }

    @Override
    public TestsInformation parseLog(TravisFold outOfFold) {
        if(this.globalResults == null)
            this.advancedParseLog(outOfFold);
        return this.globalResults;
    }

    @Override
    public List<TestsInformation> parseDetailedLog(TravisFold outOfFold) {
        if(this.detailedResults == null)
            this.advancedParseLog(outOfFold);
        return this.detailedResults;
    }

    private void advancedParseLog(TravisFold outOfFold) {
        boolean inTestBlock = false;
        boolean inTestResults = false;
        this.globalResults = new TestsInformation();
        this.detailedResults = new ArrayList<TestsInformation>();

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
    }
}
