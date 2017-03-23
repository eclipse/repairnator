package fr.inria.spirals.repairnator.serializer;


import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;


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

    protected String getPrettyPrintState(JobStatus jobStatus) {
        switch (jobStatus.getState()) {
            case NONE:
            case INIT:
            case NOTCLONABLE:
                return "not clonable";

            case CLONABLE:
            case BUILDNOTCHECKEDOUT:
            case PREVIOUSBUILDNOTCHECKEDOUT:
            case PREVIOUSBUILDCODENOTCHECKEDOUT:
                return "error in check out";

            case BUILDCHECKEDOUT:
            case PREVIOUSBUILDCHECKEDOUT:
            case PREVIOUSBUILDCODECHECKEDOUT:
            case NOTBUILDABLE:
                return "not buildable";

            case BUILDABLE:
            case NOTTESTABLE:
                return "not testable";

            case TESTABLE:
                return "testable";

            case NOTFAILING:
                return "not failing";

            case HASTESTFAILURE:
                return "test failure";

            case HASTESTERRORS:
                return "test errors";

            case CLASSPATHCOMPUTED:
            case SOURCEDIRCOMPUTED:
            case CLASSPATHNOTCOMPUTED:
            case SOURCEDIRNOTCOMPUTED:
            case NOTPATCHED:
                if (jobStatus.isReproducedAsFail()) {
                    return "test failure";
                } else {
                    return "test errors";
                }

            case PATCHED:
                return "PATCHED";

            case FIXERBUILDCASE1:
                return "FIXERBUILD_CASE1";

            case FIXERBUILDCASE2:
                return "FIXERBUILD_CASE2";

            default:
                return "unknown";
        }
    }

    public abstract void serializeData(ProjectInspector inspector);
}
