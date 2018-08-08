package fr.inria.spirals.repairnator.realtime;

import fr.inria.jtravis.entities.Build;
import fr.inria.jtravis.entities.StateType;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.realtime.serializer.WatchedBuildSerializer;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * This class is used to refresh regularly the build information.
 * It should be launched in a dedicated thread.
 */
public class InspectBuilds implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(InspectBuilds.class);

    public static final int BUILD_SLEEP_TIME = 10;
    public static final int LIMIT_SUBMITTED_BUILDS = 100;
    private static final int NB_ELEMENT_TRAVIS_JOB = 250; // the number of elements returned by Travis Job endpoint

    private CircularFifoQueue<Long> observedBuilds = new CircularFifoQueue<>(NB_ELEMENT_TRAVIS_JOB);
    private Deque<Build> waitingBuilds = new ConcurrentLinkedDeque<>();
    private int nbSubmittedBuilds;
    private RTScanner rtScanner;
    private int sleepTime;
    private int maxSubmittedBuilds;
    private WatchedBuildSerializer watchedBuildSerializer;
    private boolean shouldStop;

    public InspectBuilds(RTScanner rtScanner) {
        this.rtScanner = rtScanner;
        this.sleepTime = RepairnatorConfig.getInstance().getBuildSleepTime();
        this.maxSubmittedBuilds = RepairnatorConfig.getInstance().getMaxInspectedBuilds();
        this.watchedBuildSerializer = new WatchedBuildSerializer(this.rtScanner.getEngines(), this.rtScanner);
    }

    /**
     * This is used to stop the thread.
     */
    public void switchOff() {
        this.shouldStop = true;
    }

    /**
     * @return true if the number of build to inspect reach the limit
     */
    public boolean maxSubmittedBuildsReached() {
        return (this.nbSubmittedBuilds >= this.maxSubmittedBuilds);
    }

    public void submitNewBuild(Build build) {
        if (this.maxSubmittedBuilds == -1) {
            throw new RuntimeException("You must set maxSubmittedBuilds before running this.");
        }
        if (this.nbSubmittedBuilds < this.maxSubmittedBuilds) {
            if (!this.observedBuilds.contains(build.getId())) {
                this.observedBuilds.add(build.getId());
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
        while (!this.shouldStop) {
            LOGGER.info("Refresh all inspected build status (nb builds: "+this.nbSubmittedBuilds+")");
            for (Build build : this.waitingBuilds) {
                boolean refreshStatus = RepairnatorConfig.getInstance().getJTravis().refresh(build);
                if (!refreshStatus) {
                    LOGGER.error("Error while refreshing build: "+build.getId());
                } else {
                    if (build.getFinishedAt() != null) {
                        LOGGER.debug("Build finished (id:"+build.getId()+" | Status: "+build.getState()+")");
                        if (build.getState() == StateType.FAILED) {
                            this.rtScanner.submitBuildToExecution(build);
                        }
                        try {
                            this.watchedBuildSerializer.serialize(build);
                        } catch (Throwable e) {
                            LOGGER.error("Error while serializing", e);
                        }

                        this.waitingBuilds.remove(build);
                        synchronized (this) {
                            this.nbSubmittedBuilds--;
                        }
                    }
                }
            }

            try {
                Thread.sleep(this.sleepTime * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        LOGGER.info("This will now stop.");
    }
}
