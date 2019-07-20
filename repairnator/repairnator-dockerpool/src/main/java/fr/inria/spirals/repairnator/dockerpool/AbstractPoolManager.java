package fr.inria.spirals.repairnator.dockerpool;

import com.spotify.docker.client.DockerClient;
import fr.inria.spirals.repairnator.InputBuildId;
import fr.inria.spirals.repairnator.docker.DockerHelper;
import fr.inria.spirals.repairnator.dockerpool.serializer.TreatedBuildTracking;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class defines the main property to manage a pool of docker containers in Repairnator.
 * This class does not provide a way to run the pool: the concrete implementations should do that.
 */
public abstract class AbstractPoolManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPoolManager.class);
    private static int counter = 0;
    private static final String DEFAULT_RUN_ID = "RUN-"+(counter++);
    private static final String DEFAULT_OUTPUT_DIR = "/var/log/repairnator";

    // we need to be able to manage concurrency on this one
    protected List<RunnablePipelineContainer> submittedRunnablePipelineContainers = new CopyOnWriteArrayList<>();
    private DockerClient docker;
    private String runId = DEFAULT_RUN_ID;
    private String dockerOutputDir = DEFAULT_OUTPUT_DIR;
    private List<SerializerEngine> engines = new ArrayList<>();

    /**
     * Lazily initialize the docker client
     */
    public DockerClient getDockerClient() {
        if (this.docker == null) {
            this.docker = DockerHelper.initDockerClient();
        }
        return this.docker;
    }

    public void removeSubmittedRunnablePipelineContainer(RunnablePipelineContainer pipelineContainer) {
        this.submittedRunnablePipelineContainers.remove(pipelineContainer);
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public void setDockerOutputDir(String dockerOutputDir) {
        this.dockerOutputDir = dockerOutputDir;
    }

    public void setEngines(List<SerializerEngine> engines) {
        this.engines = engines;
    }

    /**
     * For preparing the build, we first clean the older containers
     * @param buildId
     * @return
     */
    public TreatedBuildTracking prepareBeforeSubmitBuild(long buildId) {
        this.cleanUpOlderContainers();
        return new TreatedBuildTracking(this.engines, this.runId, buildId);
    }

    /**
     * For submitting build, we first call {@link #prepareBeforeSubmitBuild(long)}, then we create the container and add it to the list of submitted.
     */
    public RunnablePipelineContainer submitBuild(String imageId, InputBuildId inputBuildId) {
        TreatedBuildTracking treatedBuildTracking = this.prepareBeforeSubmitBuild(inputBuildId.getBuggyBuildId());
        RunnablePipelineContainer runnablePipelineContainer = new RunnablePipelineContainer(this, imageId, inputBuildId, this.dockerOutputDir, treatedBuildTracking);
        this.submittedRunnablePipelineContainers.add(runnablePipelineContainer);

        return runnablePipelineContainer;
    }

    /**
     * We call this method to kill the containers which reaches the timeout
     * FIXME: we should use a thread to inspect those containers and kill them like we do in RTScanner
     */
    public void cleanUpOlderContainers() {
        LOGGER.info("Start cleaning docker containers...");
        Instant now = new Date().toInstant();

        int nbKilled = 0;
        for (RunnablePipelineContainer runnablePipelineContainer : this.submittedRunnablePipelineContainers) {
            if (runnablePipelineContainer.getLimitDateBeforeKilling() != null && runnablePipelineContainer.getLimitDateBeforeKilling().toInstant().isBefore(now)) {
                runnablePipelineContainer.killDockerContainer(this.docker, false);
                nbKilled++;
            }
        }

        LOGGER.info("Number of killed docker containers: "+nbKilled);
    }
}
