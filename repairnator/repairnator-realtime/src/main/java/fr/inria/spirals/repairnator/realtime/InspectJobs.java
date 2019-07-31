package fr.inria.spirals.repairnator.realtime;

import fr.inria.jtravis.entities.Build;
import fr.inria.jtravis.entities.v2.JobV2;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * This class is launched in a dedicated thread to interrogate regularly the /job endpoint of Travis CI
 */
public class InspectJobs implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(InspectJobs.class);

    public static final int JOB_SLEEP_TIME_IN_SECOND = 10;
    private RTScanner rtScanner;
    private boolean shouldStop;

    public InspectJobs(RTScanner scanner) {
        this.rtScanner = scanner;
    }

    /**
     * This is used to stop the thread execution.
     */
    public void switchOff() {
        this.shouldStop = true;
    }

    @Override
    public void run() {
        LOGGER.debug("Start running inspect Jobs...");
        while (!shouldStop) {
            Optional<List<JobV2>> jobListOpt = RepairnatorConfig.getInstance().getJTravis().job().allFromV2();

            if (jobListOpt.isPresent()) {
                List<JobV2> jobList = jobListOpt.get();
                LOGGER.info("Retrieved "+jobList.size()+" jobs");
                for (JobV2 job : jobList) {
                    if (this.rtScanner.isRepositoryInteresting(job.getRepositoryId())) {
                        Optional<Build> optionalBuild = RepairnatorConfig.getInstance().getJTravis().build().fromId(job.getBuildId());
                        this.rtScanner.getInspectBuilds().submitNewBuild(optionalBuild.get());
                    }
                }
            }
            if (this.rtScanner.getInspectBuilds().maxSubmittedBuildsReached() || !jobListOpt.isPresent()) {
                try {
                    Thread.sleep(RepairnatorConfig.getInstance().getJobSleepTime() * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        LOGGER.info("This will now stop.");
    }
}
