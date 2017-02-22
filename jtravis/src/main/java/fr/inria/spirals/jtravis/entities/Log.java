package fr.inria.spirals.jtravis.entities;

import fr.inria.spirals.jtravis.helpers.LogHelper;
import fr.inria.spirals.jtravis.parsers.LogParser;
import fr.inria.spirals.jtravis.parsers.MavenLogParser;
import fr.inria.spirals.jtravis.pojos.LogPojo;

/**
 * Business object to deal with log in Travis CI API
 * If the body of the log has been archived, it is lazily get as plain text from the archive using job endpoint (see {@link https://docs.travis-ci.com/api#logs})
 *
 * @author Simon Urli
 */
public class Log extends LogPojo {

    private TestsInformation testsInformation;
    private BuildTool buildTool;

    @Override
    public String getBody() {
        if (super.getBody() != null && !super.getBody().equals("")) {
            return super.getBody();
        } else {
            String body = LogHelper.getRawLogFromEmptyLog(this);
            this.setBody(body);
            return body;
        }
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
        if (buildTool == null) {
            String body = getBody();

            LogParser logParser = new LogParser(body);
            buildTool = logParser.getBuildTool();
        }

        return buildTool;
    }
}
