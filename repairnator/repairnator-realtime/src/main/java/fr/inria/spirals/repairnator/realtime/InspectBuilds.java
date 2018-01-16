package fr.inria.spirals.repairnator.realtime;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.BuildStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class InspectBuilds implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(InspectBuilds.class);

    public static final int BUILD_SLEEP_TIME = 10;
    public static final int LIMIT_SUBMITTED_BUILDS = 100;

    private Deque<Build> waitingBuilds = new ConcurrentLinkedDeque<>();
    private int nbSubmittedBuilds;
    private RTScanner rtScanner;
    private int sleepTime;
    private int maxSubmittedBuilds;

    public InspectBuilds(RTScanner rtScanner) {
        this.rtScanner = rtScanner;
        this.sleepTime = -1;
        this.maxSubmittedBuilds = -1;
    }

    public void setSleepTime(int sleepTime) {
        this.sleepTime = sleepTime;
    }

    public void setMaxSubmittedBuilds(int maxSubmittedBuilds) {
        this.maxSubmittedBuilds = maxSubmittedBuilds;
    }

    public boolean maxSubmittedBuildsReached() {
        return (this.nbSubmittedBuilds >= this.maxSubmittedBuilds);
    }

    public void submitNewBuild(Build build) {
        if (this.maxSubmittedBuilds == -1) {
            throw new RuntimeException("You must set maxSubmittedBuilds before running this.");
        }
        if (this.nbSubmittedBuilds < this.maxSubmittedBuilds) {
            if (!this.waitingBuilds.contains(build)) {
                this.waitingBuilds.add(build);
                synchronized (this) {
                    this.nbSubmittedBuilds++;
                }
                LOGGER.info("New build submitted (id: "+build.getId()+") Total: "+this.nbSubmittedBuilds+" | Limit: "+maxSubmittedBuilds+")");
            }
        } else {
            LOGGER.debug("Build submission ignored. (total reached)");
        }
    }

    @Override
    public void run() {
        LOGGER.debug("Start running inspect builds....");
        if (this.sleepTime == -1) {
            throw new RuntimeException("You must set sleepTime before running this.");
        }
        while (true) {
            LOGGER.info("Refresh all inspected build status (nb builds: "+this.nbSubmittedBuilds+")");
            for (Build build : this.waitingBuilds) {
                build.refreshStatus();
                if (build.getFinishedAt() != null) {
                    LOGGER.debug("Build finished (id:"+build.getId()+" | Status: "+build.getBuildStatus()+")");
                    if (build.getBuildStatus() == BuildStatus.FAILED) {
                        this.rtScanner.submitBuildToExecution(build);
                    }
                    this.waitingBuilds.remove(build);
                    synchronized (this) {
                        this.nbSubmittedBuilds--;
                    }
                }
            }

            try {
                Thread.sleep(this.sleepTime * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
