package fr.inria.spirals.jtravis.parsers;

import fr.inria.spirals.jtravis.entities.BuildTool;
import fr.inria.spirals.jtravis.entities.TestsInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by urli on 03/01/2017.
 */
public class LogParser {
    private Logger logger = LoggerFactory.getLogger(LogParser.class);

    private String log;
    private int logId;

    private List<TravisFold> folds;
    private TravisFold outOfFold;
    private JavaLogParser logParser;
    private BuildTool buildTool;

    public LogParser(String log) {
        this(log, -1);
    }

    public LogParser(String log, int logId) {
        this.log = log;
        this.logId = logId;
        this.folds = new ArrayList<TravisFold>();
        this.buildTool = BuildTool.UNKNOWN;
        if (log != null) {
            this.analyzeLogToCreateFolds();
        }
    }

    private void analyzeLogToCreateFolds() {
        BufferedReader reader = new BufferedReader(new StringReader(log));
        TravisFold fold = null;
        this.outOfFold = new TravisFold("outOfFold");

        Pattern patternFoldStart = Pattern.compile(".*travis_fold:start:([\\w\\.]*)");
        Pattern patternFoldEnd = Pattern.compile(".*travis_fold:end:([\\w\\.]*)");

        Pattern mvnPattern = Pattern.compile(MavenLogParser.MVN_LINE_PATTERN, Pattern.MULTILINE);
        Pattern gradlePattern = Pattern.compile(GradleLogParser.GRADLE_LINE_PARSER, Pattern.MULTILINE);

        try {
            while (reader.ready()) {
                String line = reader.readLine();

                if (line != null) {
                    Matcher matcherFoldStart = patternFoldStart.matcher(line);
                    Matcher matcherFoldEnd = patternFoldEnd.matcher(line);

                    if (matcherFoldStart.matches()) {
                        String title = matcherFoldStart.group(1);
                        fold = new TravisFold(title);
                        this.folds.add(fold);
                        continue;
                    }

                    if (matcherFoldEnd.matches()) {
                        fold = null;
                        continue;
                    }

                    if (fold == null) {
                        this.outOfFold.addContent(line);

                        Matcher mvnMatcher = mvnPattern.matcher(line);
                        Matcher gradleMatcher = gradlePattern.matcher(line);
                        if (mvnMatcher.matches()) {
                            this.logParser = new MavenLogParser();
                            this.buildTool = BuildTool.MAVEN;
                        } else if (gradleMatcher.matches()) {
                            this.logParser = new GradleLogParser();
                            this.buildTool = BuildTool.GRADLE;
                        }

                    } else {
                        fold.addContent(line);
                    }
                } else {
                    break;
                }
            }
            reader.close();
        } catch (IOException e) {
            logger.error("Error while reading logs",e);
        }

        if (this.buildTool == BuildTool.UNKNOWN) {
            logger.warn("The build tool has not been recognized.");
        }

    }

    public BuildTool getBuildTool() {
        return buildTool;
    }

    public TestsInformation getTestsInformation() {
        if (this.buildTool != BuildTool.UNKNOWN) {
            return this.logParser.parseLog(this.outOfFold);
        }
        return null;
    }

    public List<TestsInformation> getDetailedTestsInformation() {
        if (this.buildTool == BuildTool.MAVEN) {
            return this.logParser.parseDetailedLog(this.outOfFold);
        }
        return null;
    }
}
