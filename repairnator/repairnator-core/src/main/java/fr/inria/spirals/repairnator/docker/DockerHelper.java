package fr.inria.spirals.repairnator.docker;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Image;

import java.util.List;

public class DockerHelper {

    public static DockerClient initDockerClient() {
        DockerClient docker;
        try {
            docker = DefaultDockerClient.fromEnv().build();
        } catch (DockerCertificateException e) {
            throw new RuntimeException("Error while initializing docker client.");
        }
        return docker;
    }

    public static String findDockerImage(String imageName, DockerClient docker) {
        if (docker == null) {
            throw new RuntimeException("Docker client has not been initialized. No docker image can be found.");
        }

        try {
            List<Image> allImages = docker.listImages(DockerClient.ListImagesParam.allImages());

            String imageId = null;
            for (Image image : allImages) {
                if (image.repoTags() != null && image.repoTags().contains(imageName)) {
                    imageId = image.id();
                    break;
                }
            }

            if (imageId == null) {
                throw new RuntimeException("There was a problem when looking for the docker image with argument \""+imageName+"\": no image has been found.");
            }
            return imageId;
        } catch (InterruptedException|DockerException e) {
            throw new RuntimeException("Error while looking for the docker image",e);
        }
    }

}
