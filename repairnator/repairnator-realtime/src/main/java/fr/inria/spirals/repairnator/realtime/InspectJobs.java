package fr.inria.spirals.repairnator.realtime;

import fr.inria.spirals.jtravis.entities.Job;
import fr.inria.spirals.jtravis.helpers.JobHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class InspectJobs implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(InspectJobs.class);

    public static final int JOB_SLEEP_TIME = 10;
    private RTScanner rtScanner;
    private int sleepTime;

    public InspectJobs(RTScanner scanner) {
        this.rtScanner = scanner;
        this.sleepTime = -1;
    }

    public void setSleepTime(int sleepTime) {
        this.sleepTime = sleepTime;
    }

    @Override
    public void run() {
    	LOGGER.debug("Start running inspect Jobs...");
        if (sleepTime == -1) {
            throw new RuntimeException("Sleep time has to be set before running this.");
        }
        while (true) {
            List<Job> jobList = JobHelper.getJobList();

            LOGGER.info("Retrieved "+jobList.size()+" jobs");
            for (Job job : jobList) {
                if (this.rtScanner.isRepositoryInteresting(job.getRepositoryId())) {
                    this.rtScanner.submitWaitingBuild(job.getBuildId());
                }
            }

            /*try {
                Thread.sleep(sleepTime * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
        }
    }
}
