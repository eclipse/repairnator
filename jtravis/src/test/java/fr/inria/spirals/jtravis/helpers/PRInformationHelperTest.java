package fr.inria.spirals.jtravis.helpers;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.Commit;
import fr.inria.spirals.jtravis.entities.Config;
import fr.inria.spirals.jtravis.entities.PRInformation;
import fr.inria.spirals.jtravis.entities.Repository;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by urli on 04/01/2017.
 */
public class PRInformationHelperTest {
    @Test
    public void testGetBuildWithPRWhenMergeCommitDeleted() {
        Build originBuild = BuildHelper.getBuildFromId(187029370, null);
        PRInformation obtainedPRInfo = PRInformationHelper.getPRInformationFromBuild(originBuild);

        assertNull(obtainedPRInfo);
    }

    @Test
    public void testGetCommitInformationFromPR() {
        Build originBuild = BuildHelper.getBuildFromId(186814810, null);

        PRInformation prInformation = PRInformationHelper.getPRInformationFromBuild(originBuild);
        assertEquals("pvojtechovsky/spoon", prInformation.getOtherRepo().getSlug());
        assertEquals("master", prInformation.getBase().getBranch());
        assertEquals("lazyQueries", prInformation.getHead().getBranch());
        assertEquals("7a55cd2c526a7bbb914dacbe6ba2ddc621f23870", prInformation.getBase().getSha());
        assertEquals("1ae580bc0f1bbeb140db074bb84c23080620f156", prInformation.getHead().getSha());
    }
}
