package fr.inria.spirals.jtravis.helpers;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.Config;
import fr.inria.spirals.jtravis.entities.Job;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by urli on 22/12/2016.
 */
public class JobHelperTest {

    @Test
    public void testGetJobFromId() {
        Job expectedJob = new Job();
        expectedJob.setId(185719844);
        expectedJob.setCommitId(53036982);
        expectedJob.setRepositoryId(2800492);
        expectedJob.setAllowFailure(false);
        expectedJob.setBuildId(185719843);
        expectedJob.setFinishedAt(TestUtils.getDate(2016,12,21,9,56,41));
        expectedJob.setNumber("2373.1");
        expectedJob.setQueue("builds.gce");
        expectedJob.setState("passed");
        expectedJob.setStartedAt(TestUtils.getDate(2016,12,21,9,49,46));

        Config expectedConfig = new Config();
        expectedConfig.setLanguage("java");

        expectedJob.setConfig(expectedConfig);

        Job obtainedJob = JobHelper.getJobFromId(185719844);
        assertEquals(expectedJob, obtainedJob);
    }

    @Test
    public void testGetJobList() {
        int minId = 329043061;

        List<Job> jobs = JobHelper.getJobList();
        assertTrue(jobs.size() > 1);
        assertTrue(jobs.get(0).getId() > minId);
    }

    @Test
	public void testJobListIsOrdered() {
		List<Job> jobs = JobHelper.getJobList();

		Job firstJob = jobs.get(0);
		Job lastJob = jobs.get(jobs.size()-1);

		if (firstJob.getBuildId() != lastJob.getBuildId()) {
			Build firstBuild = BuildHelper.getBuildFromId(firstJob.getBuildId(), null);
			Build lastBuild = BuildHelper.getBuildFromId(lastJob.getBuildId(), null);

			if (firstBuild.getStartedAt() != null && lastBuild.getStartedAt() != null) {
				assertTrue(firstBuild.getStartedAt().after(lastBuild.getStartedAt()));
			} else {
				assertTrue(firstBuild.getCommit().getCommittedAt().getTime() >= lastBuild.getCommit().getCommittedAt().getTime());
			}

		} else {
			assertTrue(firstJob.getJobNumber() > lastJob.getJobNumber());
		}

	}
}
