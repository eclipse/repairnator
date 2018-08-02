package fr.inria.spirals.repairnator.checkbranches;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerExit;
import com.spotify.docker.client.messages.HostConfig;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;

import fr.inria.spirals.repairnator.states.LauncherMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by urli on 14/03/2017.
 */
public class RunnablePipelineContainer implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunnablePipelineContainer.class);
    private String imageId;
    private String branchName;
    private RepairnatorConfig repairnatorConfig;


    public RunnablePipelineContainer(String imageId, String branchName) {
        this.imageId = imageId;
        this.branchName = branchName;
        this.repairnatorConfig = RepairnatorConfig.getInstance();
    }

    @Override
    public void run() {
        String containerId = null;
        DockerClient docker = Launcher.docker;
        try {
            LOGGER.info("Start to run check container for branch "+branchName);

            String containerName = "checkbranch_"+ branchName;

            List<String> envValues = new ArrayList<>();
            envValues.add("BRANCH_NAME="+this.branchName);
            envValues.add("REPOSITORY="+this.repairnatorConfig.getRepository());
            if (this.repairnatorConfig.getLauncherMode() == LauncherMode.REPAIR) {
                String humanPatchStr = this.repairnatorConfig.isHumanPatch() ? "--human-patch" : "";
                envValues.add("HUMAN_PATCH="+humanPatchStr);
            }

            Map<String,String> labels = new HashMap<>();
            labels.put("name",containerName);
            HostConfig hostConfig = HostConfig.builder().appendBinds(this.repairnatorConfig.getOutputPath()+":/tmp/result.txt").build();
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

            if (!this.repairnatorConfig.isSkipDelete() && exitStatus.statusCode() == 0) {
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
