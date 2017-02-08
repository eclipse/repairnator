package fr.inria.spirals.repairnator.serializer;

import fr.inria.spirals.repairnator.process.ProjectInspector;
import fr.inria.spirals.repairnator.process.ProjectState;

/**
 * Created by urli on 20/01/2017.
 */
public abstract class AbstractDataSerializer {

    private static final String TRAVIS_URL = "http://travis-ci.org/";

    public AbstractDataSerializer() {}

    protected String getTravisUrl(int buildId, String slug) {
        return TRAVIS_URL+slug+"/builds/"+buildId;
    }

    protected String getPrettyPrintState(ProjectState state) {
        switch (state) {
            case NONE:
            case INIT:
                return "not clonable";

            case CLONABLE:
                return "not buildable";

            case NOTTESTABLE:
            case BUILDABLE:
                return "not testable";

            case TESTABLE:
                return "testable";

            case HASTESTERRORS:
                return "test errors";

            case HASTESTFAILURE:
                return "test failure";

            case NOTFAILING:
                return "not failing";

            case PATCHED:
                return  "PATCHED";

            default:
                return  "unknown";
        }
    }

    public abstract void serializeData(ProjectInspector inspector);
}
