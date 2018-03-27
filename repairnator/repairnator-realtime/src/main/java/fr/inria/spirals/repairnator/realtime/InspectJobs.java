package fr.inria.spirals.repairnator.realtime;

import fr.inria.jtravis.entities.Job;
import fr.inria.jtravis.helpers.JobHelper;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class InspectJobs implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(InspectJobs.class);

    public static final int JOB_SLEEP_TIME = 60;
    private RTScanner rtScanner;
    private int sleepTime;
    private boolean shouldStop;

    public InspectJobs(RTScanner scanner) {
        this.rtScanner = scanner;
        this.sleepTime = RepairnatorConfig.getInstance().getJobSleepTime();
    }

    public void switchOff() {
        this.shouldStop = true;
    }

    @Override
    public void run() {
        LOGGER.debug("Start running inspect Jobs...");
        if (sleepTime == -1) {
            throw new RuntimeException("Sleep time has to be set before running this.");
        }
        while (!shouldStop) {
            List<Job> jobList = JobHelper.getJobList();

            if (jobList != null) {
                LOGGER.info("Retrieved "+jobList.size()+" jobs");
                for (Job job : jobList) {
                    if (this.rtScanner.isRepositoryInteresting(job.getRepositoryId())) {
                        this.rtScanner.submitWaitingBuild(job.getBuildId());
                    }
                }
            }
            if (this.rtScanner.getInspectBuilds().maxSubmittedBuildsReached() || jobList == null) {
                LOGGER.debug("Max number of submitted builds reached. Sleep for "+sleepTime+" seconds.");
                try {
                    Thread.sleep(sleepTime * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        LOGGER.info("This will now stop.");
    }
}
