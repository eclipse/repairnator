package fr.inria.spirals.repairnator.checkbranches;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerExit;
import com.spotify.docker.client.messages.HostConfig;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;

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
    private String branchName;
    private String output;
    private RepairnatorConfig repairnatorConfig;
    private boolean skipDelete;
    private String repository;


    public RunnablePipelineContainer(String imageId, String repository, String branchName, String output, boolean skipDelete) {
        this.imageId = imageId;
        this.branchName = branchName;
        this.output = output;
        this.repairnatorConfig = RepairnatorConfig.getInstance();
        this.skipDelete = skipDelete;
        this.repository = repository;
    }

    @Override
    public void run() {
        String containerId = null;
        DockerClient docker = Launcher.docker;
        try {
            LOGGER.info("Start to run check container for branch "+branchName);

            String containerName = "checkbranch_"+ branchName;

            String[] envValues = new String[] {
                "BRANCH_NAME="+this.branchName,
                "REPOSITORY="+this.repository
            };

            Map<String,String> labels = new HashMap<>();
            labels.put("name",containerName);
            HostConfig hostConfig = HostConfig.builder().appendBinds(this.output+":/tmp/result.txt").build();
            ContainerConfig containerConfig = ContainerConfig.builder()
                    .image(imageId)
                    .env(envValues)
                    .hostname(Utils.getHostname())
                    .hostConfig(hostConfig)
                    .labels(labels)
                    .build();

            LOGGER.info("Create the container: "+containerName);
            ContainerCreation container = docker.createContainer(containerConfig);

            containerId = container.id();

            LOGGER.info("Start the container: "+containerName+" (id: "+containerId+")");
            docker.startContainer(container.id());

            ContainerExit exitStatus = docker.waitContainer(containerId);

            LOGGER.info("The container has finished with status code: "+exitStatus.statusCode());

            if (!skipDelete && exitStatus.statusCode() == 0) {
                LOGGER.info("Container will be removed.");
                docker.removeContainer(containerId);
            }
            LOGGER.info("TREATED Docker container : "+containerId);
        } catch (InterruptedException e) {
            LOGGER.error("Error while running the container for branch name "+branchName, e);
            killDockerContainer(docker, containerId);
        } catch (DockerException e) {
            LOGGER.error("Error while creating or running the container for branch name "+branchName, e);
        }
        Launcher.submittedRunnablePipelineContainers.remove(this);
    }

    private void killDockerContainer(DockerClient docker, String containerId) {
        LOGGER.info("Killing docker container: "+containerId);
        try {
            docker.killContainer(containerId);
            docker.removeContainer(containerId);
        } catch (DockerException|InterruptedException e) {
            LOGGER.error("Error while killing docker container "+containerId, e);
        }

    }

}
