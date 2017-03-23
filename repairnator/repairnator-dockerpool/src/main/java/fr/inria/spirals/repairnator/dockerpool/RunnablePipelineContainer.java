package fr.inria.spirals.repairnator.dockerpool;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerExit;
import com.spotify.docker.client.messages.HostConfig;
import fr.inria.spirals.repairnator.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by urli on 14/03/2017.
 */
public class RunnablePipelineContainer implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunnablePipelineContainer.class);
    private String imageId;
    private int buildId;
    private String logDirectory;
    private String runId;
    private String accessToken;

    public RunnablePipelineContainer(String imageId, int buildId, String logDirectory, String runId, String accessToken) {
        this.imageId = imageId;
        this.buildId = buildId;
        this.logDirectory = logDirectory;
        this.runId = runId;
        this.accessToken = accessToken;
    }

    @Override
    public void run() {
        try {
            LOGGER.info("Start to build and run container for build id "+buildId);
            DockerClient docker = Launcher.docker;

            String containerName = "repairnator-pipeline_"+ Utils.formatFilenameDate(new Date())+"_"+this.buildId;
            String[] envValues = new String[] {
                "BUILD_ID="+this.buildId,
                "LOG_FILENAME="+containerName,
                "GITHUB_LOGIN="+System.getenv("GITHUB_LOGIN"),
                "GITHUB_OAUTH="+System.getenv("GITHUB_OAUTH"),
                "RUN_ID="+this.runId,
                "GOOGLE_ACCESS_TOKEN="+this.accessToken
            };

            Map<String,String> labels = new HashMap<>();
            labels.put("name",containerName);
            HostConfig hostConfig = HostConfig.builder().appendBinds(this.logDirectory+":/var/log").build();
            ContainerConfig containerConfig = ContainerConfig.builder()
                    .image(imageId)
                    .env(envValues)
                    .hostname(Utils.getHostname())
                    .hostConfig(hostConfig)
                    .labels(labels)
                    .build();

            LOGGER.info("Create the container: "+containerName);
            ContainerCreation container = docker.createContainer(containerConfig);

            Launcher.runningDockerContainer.add(container.id());
            LOGGER.info("Start the container: "+containerName);
            docker.startContainer(container.id());

            ContainerExit exitStatus = docker.waitContainer(container.id());

            LOGGER.info("The container has finished with status code: "+exitStatus.statusCode());
            docker.removeContainer(container.id());
            Launcher.runningDockerContainer.remove(container.id());
        } catch (InterruptedException|DockerException e) {
            LOGGER.error("Error while creating/running the container for build id "+buildId, e);
        }

    }
}
