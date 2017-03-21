package fr.inria.spirals.jtravis.entities;

import fr.inria.spirals.jtravis.parsers.LogParser;

/**
 * Business object to deal with log in Travis CI API
 * If the body of the log has been archived, it is lazily get as plain text from the archive using job endpoint (see {@link https://docs.travis-ci.com/api#logs})
 *
 * @author Simon Urli
 */
public class Log {

    private String body;
    private TestsInformation testsInformation;
    private BuildTool buildTool;
    private int jobId;

    public Log(int jobId, String body) {
        this.jobId = jobId;
        this.body = body;
    }

    public String getBody() {
        return this.body;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public TestsInformation getTestsInformation() {
        if (testsInformation == null) {
            String body = getBody();
            LogParser logParser = new LogParser(body);

            this.testsInformation = logParser.getTestsInformation();
        }

        return this.testsInformation;
    }

    public BuildTool getBuildTool() {
        if (this.buildTool == null) {
            String body = getBody();

            LogParser logParser = new LogParser(body);
            this.buildTool = logParser.getBuildTool();
        }

        return buildTool;
    }
}
