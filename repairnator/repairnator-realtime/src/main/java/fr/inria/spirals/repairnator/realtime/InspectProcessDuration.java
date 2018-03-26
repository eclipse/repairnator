package fr.inria.spirals.repairnator.realtime;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.EndProcessNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public class InspectProcessDuration implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(InspectProcessDuration.class);
    private static final int SLEEP_TIME = 10; // seconds

    private Duration duration;
    private EndProcessNotifier endProcessNotifier;
    private InspectBuilds inspectBuilds;
    private InspectJobs inspectJobs;
    private BuildRunner buildRunner;

    public InspectProcessDuration(InspectBuilds inspectBuilds, InspectJobs inspectJobs, BuildRunner buildRunner, EndProcessNotifier endProcessNotifier) {
        this(inspectBuilds, inspectJobs, buildRunner);
        this.endProcessNotifier = endProcessNotifier;
    }

    public InspectProcessDuration(InspectBuilds inspectBuilds, InspectJobs inspectJobs, BuildRunner buildRunner) {
        this.duration = RepairnatorConfig.getInstance().getDuration();
        this.inspectBuilds = inspectBuilds;
        this.inspectJobs = inspectJobs;
        this.buildRunner = buildRunner;
    }

    @Override
    public void run() {
        Instant endOfProcessDate = new Date().toInstant().plus(duration);
        LOGGER.info("The process will finish at: " + endOfProcessDate);

        while (!new Date().toInstant().isAfter(endOfProcessDate)) {
            try {
                Thread.sleep(SLEEP_TIME * 1000);
            } catch (InterruptedException e) {
                LOGGER.warn("Sleep interrupted: premature stop will occured.");
            }
        }

        LOGGER.info("The process will now stop.");
        this.inspectBuilds.switchOff();
        this.inspectJobs.switchOff();
        this.buildRunner.switchOff();

        if (this.endProcessNotifier != null) {
            this.endProcessNotifier.notifyEnd();
        }
    }
}
