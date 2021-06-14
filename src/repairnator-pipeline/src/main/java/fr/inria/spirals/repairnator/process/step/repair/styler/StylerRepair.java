package fr.inria.spirals.repairnator.process.step.repair.styler;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import fr.inria.spirals.repairnator.config.StylerConfig;
import fr.inria.spirals.repairnator.docker.DockerHelper;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.step.repair.AbstractRepairStep;

/**
 * StylerRepair is the repair step that calls the Styler docker image.
 * Styler is an automatic repair system for Checkstyle errors based
 * on machine learning.
 *
 * Check Styler's paper for more info:
 * arxiv.org/pdf/1904.01754
 *
 * Styler's base repo:
 * https://github.com/KTH/styler/
 *
 * @author andre15silva
 */
public class StylerRepair extends AbstractRepairStep {

	public static final String TOOL_NAME = "StylerRepair";
	private final DockerClient docker;

	public StylerRepair() {
		this.docker = DockerHelper.initDockerClient();
	}

	@Override
	public String getRepairToolName() {
		return TOOL_NAME;
	}

	@Override
	protected StepStatus businessExecute() {
		this.getLogger().info("Starting StylerRepair step");

		StylerConfig config = StylerConfig.getInstance();

		String stylerCommand = "./styler_repairnator.sh "
				+ this.getInspector().getGitSlug() + " "
				+ this.getInspector().getBuggyBuild().getBranch() + " "
				+ this.getInspector().getGitCommit() + " "
				+ config.getSnicHost() + " "
				+ config.getSnicUsername() + " "
				+ config.getSnicPassword() + " "
				+ config.getSnicPath()
				+ "/out/results.json";

		HostConfig.Builder hostConfigBuilder = HostConfig.builder();
		hostConfigBuilder.appendBinds(HostConfig.Bind
						.from(this.getInspector().getWorkspace())
						.to("/out")
						.build());
		HostConfig hostConfig = hostConfigBuilder.build();

		ContainerConfig containerConfig = ContainerConfig.builder()
				.image(config.getDockerTag())
				.hostConfig(hostConfig)
				.cmd("bash", "-c", stylerCommand)
				.attachStdout(true)
				.attachStderr(true)
				.build();

		try {
			ContainerCreation container = docker.createContainer(containerConfig);

			docker.startContainer(container.id());
			docker.waitContainer(container.id());

			String stdOut = docker.logs(
					container.id(),
					DockerClient.LogsParam.stdout()
			).readFully();

			String stdErr = docker.logs(
					container.id(),
					DockerClient.LogsParam.stderr()
			).readFully();

			this.getLogger().debug("stdOut: \n" + stdOut);
			this.getLogger().debug("stdErr: \n" + stdErr);

			docker.removeContainer(container.id());
		} catch (DockerException e) {
			this.getLogger().error(e.getMessage());
			return StepStatus.buildSkipped(this);
		} catch (InterruptedException e) {
			this.getLogger().error(e.getMessage());
			return StepStatus.buildSkipped(this);
		}

		return StepStatus.buildSuccess(this);
	}

}