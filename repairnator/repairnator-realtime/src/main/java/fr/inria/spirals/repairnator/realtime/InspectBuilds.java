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

    public static final int BUILD_SLEEP_TIME_IN_SECOND = 10;
    public static final int LIMIT_WAITING_BUILDS = 1000;
    private static final int NB_ELEMENT_TRAVIS_JOB = 250; // the number of elements returned by Travis Job endpoint

    // this fifo queue contains all the ids of the builds that we observed
    // it prevents us for watching twice the same build
    private CircularFifoQueue<Long> observedBuilds = new CircularFifoQueue<>(NB_ELEMENT_TRAVIS_JOB);

    // we use a ConcurrentLinkedDeque because new builds might be submitted while we iterate over it
    private Deque<Build> waitingBuilds = new ConcurrentLinkedDeque<>();

    private RTScanner rtScanner;
    private WatchedBuildSerializer watchedBuildSerializer;
    private boolean shouldStop;

    public InspectBuilds(RTScanner rtScanner) {
        this.rtScanner = rtScanner;
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
        return (this.waitingBuilds.size() >= RepairnatorConfig.getInstance().getMaxInspectedBuilds());
    }

    public void submitNewBuild(Build build) {
        if (this.waitingBuilds.size() < RepairnatorConfig.getInstance().getMaxInspectedBuilds()) {
            // we do not reached the maximum yet

            // we check if we already inspected this build
            if (!this.observedBuilds.contains(build.getId())) {

                // must be synchronized to avoid concurrent access
                synchronized (this.waitingBuilds) {
                    // it's not the case: we add the build to the lists
                    this.observedBuilds.add(build.getId());
                    this.waitingBuilds.add(build);
                }
                LOGGER.info("New build in waiting list (id: "+build.getId()+") Total: "+this.waitingBuilds.size()+")");
            }
        } else {
            LOGGER.debug("Build submission ignored. (maximum reached)");
        }
    }

    public void submitIfBuildIsInteresting(Build build) {
        boolean refreshStatus = RepairnatorConfig.getInstance().getJTravis().refresh(build);
        if (!refreshStatus) {
            LOGGER.error("Error while refreshing build: "+build.getId());
        } else {

            // when the refresh worked well, we check if it finished or not

            if (build.getFinishedAt() != null) {
                LOGGER.debug("Build finished (id:"+build.getId()+" | Status: "+build.getState()+")");

                // we check that the build is indeed failing
                if (build.getState() == StateType.FAILED) {

                    // if it's the case we submit it
                    this.rtScanner.submitBuildToExecution(build);
                }

                try {
                    this.watchedBuildSerializer.serialize(build);
                } catch (Throwable e) {
                    LOGGER.error("Error while serializing", e);
                }

                this.waitingBuilds.remove(build);
            }
        }
    }

    @Override
    public void run() {
        LOGGER.debug("Start running inspect builds....");
        while (!this.shouldStop) {
            LOGGER.info("Refresh all waiting build (nb builds: "+this.waitingBuilds.size()+")");

            // we iterate over all builds to refresh them
            for (Build build : this.waitingBuilds) {
                this.submitIfBuildIsInteresting(build);
            }

            try {
                Thread.sleep(RepairnatorConfig.getInstance().getBuildSleepTime() * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        LOGGER.info("This will now stop.");
    }
}
