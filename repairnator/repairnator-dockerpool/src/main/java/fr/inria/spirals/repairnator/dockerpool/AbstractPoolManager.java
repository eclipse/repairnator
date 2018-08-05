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

public class AbstractPoolManager {
    public static final Logger LOGGER = LoggerFactory.getLogger(AbstractPoolManager.class);
    private static int counter = 0;
    private static final String DEFAULT_RUN_ID = "RUN-"+(counter++);
    private static final String DEFAULT_OUTPUT_DIR = "/var/log/repairnator";

    protected List<RunnablePipelineContainer> submittedRunnablePipelineContainers = new CopyOnWriteArrayList<>();
    private DockerClient docker;
    private String runId = DEFAULT_RUN_ID;
    private String dockerOutputDir = DEFAULT_OUTPUT_DIR;
    private List<SerializerEngine> engines = new ArrayList<>();

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

    public TreatedBuildTracking prepareBeforeSubmitBuild(long buildId) {
        this.cleanUpOlderContainers();
        return new TreatedBuildTracking(this.engines, this.runId, buildId);
    }

    public RunnablePipelineContainer submitBuild(String imageId, InputBuildId inputBuildId) {
        TreatedBuildTracking treatedBuildTracking = this.prepareBeforeSubmitBuild(inputBuildId.getBuggyBuildId());
        RunnablePipelineContainer runnablePipelineContainer = new RunnablePipelineContainer(this, imageId, inputBuildId, this.dockerOutputDir, treatedBuildTracking);
        this.submittedRunnablePipelineContainers.add(runnablePipelineContainer);

        return runnablePipelineContainer;
    }

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
