package fr.inria.spirals.repairnator.serializer;


import fr.inria.spirals.repairnator.ProjectState;
import fr.inria.spirals.repairnator.process.step.GatherTestInformation;


/**
 * Created by urli on 20/01/2017.
 */
public abstract class AbstractDataSerializer {

    private static final String TRAVIS_URL = "http://travis-ci.org/";

    public AbstractDataSerializer() {
    }

    protected String getTravisUrl(int buildId, String slug) {
        return TRAVIS_URL + slug + "/builds/" + buildId;
    }

    protected String getPrettyPrintState(ProjectState state, GatherTestInformation gatherTestInformation) {
        switch (state) {
            case NONE:
            case INIT:
                return "not clonable";

            case CLONABLE:
                return "error in check out";

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
                return "PATCHED";

            case SOURCEDIRCOMPUTED:
            case CLASSPATHCOMPUTED:
                if (gatherTestInformation.getNbFailingTests() > 0) {
                    return "test failure";
                } else {
                    return "test errors";
                }

            case DOESNOTHAVEPREVIOUSVERSION:
                return "does not have previous build";

            case PREVIOUSVERSIONISNOTINTERESTING:
                return "previous build is not interesting";

            case BUILDCHECKEDOUT:
                return "build checked out";

            case BUILDNOTCHECKEDOUT:
                return "build not checked out";

            case PREVIOUSBUILDCHECKEDOUT:
                return "not buildable";

            case PREVIOUSBUILDNOTCHECKEDOUT:
                return "previous build not checked out";

            case PREVIOUSBUILDCODECHECKEDOUT:
                return "previous build code checked out";

            case PREVIOUSBUILDCODENOTCHECKEDOUT:
                return "previous build code not checked out";

            case FIXERBUILD_CASE1:
                return "FIXERBUILD_CASE1";

            case FIXERBUILD_CASE2:
                return "FIXERBUILD_CASE2";

            default:
                return "unknown";
        }
    }

    public abstract void serializeData(ProjectInspector inspector);
}
