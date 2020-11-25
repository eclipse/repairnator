package fr.inria.spirals.repairnator.realtime;

import fr.inria.spirals.repairnator.GithubInputBuild;

import java.util.Date;

public class GithubCommitRunner {

    final int N_THREADS = 4;

    DockerPipelineRunner runner;

    public GithubCommitRunner(){
        runner = new DockerPipelineRunner();
        runner.initExecutorService(N_THREADS);
    }


    public void submitBuild(String gitHubURL, String commitID) {
        if (runner.getLimitDateNextRetrieveDockerImage() != null && runner.getLimitDateNextRetrieveDockerImage().before(new Date())) {
            runner.refreshDockerImage();
        }
        runner.getExecutorService().submit(runner.submitBuild(this.runner.getDockerImageId(), new GithubInputBuild(gitHubURL, commitID)));
    }
}
