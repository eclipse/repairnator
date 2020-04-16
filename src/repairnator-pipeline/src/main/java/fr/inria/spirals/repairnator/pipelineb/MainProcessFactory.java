package fr.inria.spirals.repairnator.pipeline;

import fr.inria.spirals.repairnator.InputBuildId;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.states.LauncherMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.nio.file.Files;
import java.io.IOException;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;

import fr.inria.spirals.repairnator.process.inspectors.InspectorFactory;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.GitRepositoryProjectInspector;
import fr.inria.spirals.repairnator.process.step.repair.SonarQubeRepair;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.serializer.HardwareInfoSerializer;


import fr.inria.spirals.repairnator.serializer.InspectorSerializer;
import fr.inria.spirals.repairnator.serializer.InspectorSerializer4Bears;
import fr.inria.spirals.repairnator.serializer.InspectorTimeSerializer;
import fr.inria.spirals.repairnator.serializer.PullRequestSerializer;
import fr.inria.spirals.repairnator.serializer.PropertiesSerializer;
import fr.inria.spirals.repairnator.serializer.PipelineErrorSerializer;
import fr.inria.spirals.repairnator.serializer.PatchesSerializer;
import fr.inria.spirals.repairnator.serializer.ToolDiagnosticSerializer;
import fr.inria.spirals.repairnator.serializer.PullRequestSerializer;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;

import fr.inria.spirals.repairnator.serializer.InspectorSerializer4GitRepository;
import fr.inria.spirals.repairnator.serializer.InspectorTimeSerializer4GitRepository;
import fr.inria.spirals.repairnator.serializer.PatchesSerializer4GitRepository;
import fr.inria.spirals.repairnator.serializer.PipelineErrorSerializer4GitRepository;
import fr.inria.spirals.repairnator.serializer.PropertiesSerializer4GitRepository;
import fr.inria.spirals.repairnator.serializer.ToolDiagnosticSerializer4GitRepository;
import fr.inria.spirals.repairnator.serializer.PullRequestSerializer4GitRepository;

import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.notifier.AbstractNotifier;

/*  HENRY -  make packages correct later */

/* This will manufacture different kind of repairnator type */
public class MainProcessFactory {

	public static RepairnatorConfig getConfig() {
		return RepairnatorConfig.getInstance();
	}

	/* Standard repair with Travis build id */
	public static MainProcess getDefaultMainProcess(String[] inputArgs) {

		DefaultDefineJSAPArgs defaultDefineJSAPArgs = new DefaultDefineJSAPArgs();
		DefaultInitConfig defaultInitConfig = new DefaultInitConfig();
		DefaultInitNotifiers defaultInitNotifiers = new DefaultInitNotifiers();
		DefaultInitSerializerEngines defaultInitSerializerEngines = new DefaultInitSerializerEngines();
		
		JSAP jsap;
		try {
			jsap = defaultDefineJSAPArgs.defineArgs();
		} catch (JSAPException e) {
			throw new RuntimeException("Failed to parse JSAP");
		}
		defaultInitConfig.initConfigWithJSAP(jsap,inputArgs);
		defaultInitSerializerEngines.initSerializerEngines();
		defaultInitNotifiers.initNotifiers();

		DefaultMainProcess defaultMainProcess = new DefaultMainProcess(defaultDefineJSAPArgs,
																		defaultInitConfig,
																		defaultInitSerializerEngines,
																			defaultInitNotifiers);

		serializeHardwareInfoSerializer(defaultInitSerializerEngines.getEngines());

		ProjectInspector inspector = constructInspector4Default(defaultMainProcess.getBuildToBeInspected(),defaultInitSerializerEngines.getEngines(),defaultInitNotifiers.getNotifiers());

		/* HENRY - lift the other non relevant function to here */
		defaultMainProcess = defaultMainProcess.setInspector(inspector)
												.setEngines(defaultInitSerializerEngines.getEngines())
												.setNotifiers(defaultInitNotifiers.getNotifiers());

		return defaultMainProcess;
	}

	/* Repair with git Url instead of travis */
	public static MainProcess getGithubMainProcess(String[] inputArgs) {

		GithubDefineJSAPArgs githubDefineJSAPArgs = new GithubDefineJSAPArgs();
		GithubInitConfig githubInitConfig = new GithubInitConfig();
		GithubInitNotifiers githubInitNotifiers = new GithubInitNotifiers();
		GithubInitSerializerEngines githubInitSerializerEngines = new GithubInitSerializerEngines();

		JSAP jsap;
		try {
			jsap = githubDefineJSAPArgs.defineArgs();
		} catch (JSAPException e) {
			throw new RuntimeException("Failed to parse JSAP");
		}
		githubInitConfig.initConfigWithJSAP(jsap,inputArgs);
		githubInitSerializerEngines.initSerializerEngines();
		githubInitNotifiers.initNotifiers();

		GithubMainProcess githubMainProcess = new GithubMainProcess(githubDefineJSAPArgs,
																	githubInitConfig,
																	githubInitSerializerEngines,
																	githubInitNotifiers);

		serializeHardwareInfoSerializer(githubInitSerializerEngines.getEngines());

		ProjectInspector inspector =  constructInspector4Github(githubInitSerializerEngines.getEngines(),githubInitNotifiers.getNotifiers());

		githubMainProcess = githubMainProcess.setInspector(inspector)
												.setNotifiers(githubInitNotifiers.getNotifiers())
												.setEngines(githubInitSerializerEngines.getEngines());

		return githubMainProcess;
	}

	public static MainProcess getPipelineListenerMainProcess(String[] inputArgs) {
		MainProcess defaultMainProcess = getDefaultMainProcess(inputArgs);
		return new PipelineBuildListenerMainProcess(defaultMainProcess);
	}

	/*  HENRY - manual test again later */
	public static MainProcess getJenkinsPluginMainProcess(String[] inputArgs) {
		GithubMainProcess jenkinsMainProcess = (GithubMainProcess)getGithubMainProcess(inputArgs);
		GithubDefineJSAPArgs githubDefineJSAPArgs = new GithubDefineJSAPArgs();
		JenkinsPluginInitConfig jenkinsInitConfig = new JenkinsPluginInitConfig();

		JSAP jsap;
		try {
			jsap = githubDefineJSAPArgs.defineArgs();
		} catch (JSAPException e) {
			throw new RuntimeException("Failed to parse JSAP");
		}
		jenkinsInitConfig.initConfigWithJSAP(jsap,inputArgs);

		return jenkinsMainProcess;
	}

	private static void serializeHardwareInfoSerializer(List<SerializerEngine> engines) {
		HardwareInfoSerializer hardwareInfoSerializer = new HardwareInfoSerializer(engines, getConfig().getRunId(), getConfig().getBuildId()+"");
        hardwareInfoSerializer.serialize();
	}

	/* These methods below should be called after all other inits */
	/* move serializer into project inspector and get to construct */
	private static GitRepositoryProjectInspector constructInspector4Github(List<SerializerEngine> engines,List<AbstractNotifier> notifiers) {
		GitRepositoryProjectInspector inspector;

		boolean shouldStaticAnalysis = getConfig().getRepairTools().contains(SonarQubeRepair.TOOL_NAME) && getConfig().getRepairTools().size() == 1;

		if (shouldStaticAnalysis) {
            inspector = (GitRepositoryProjectInspector) InspectorFactory.getStaticAnalysisGithubInspector(
                getConfig().getGitRepositoryUrl(),
                getConfig().getGitRepositoryBranch(),
                getConfig().getGitRepositoryIdCommit(),
                getConfig().isGitRepositoryFirstCommit(),
                getConfig().getWorkspacePath(),
                notifiers
            );
        } else {
            inspector = (GitRepositoryProjectInspector) InspectorFactory.getDefaultGithubInspector(
                getConfig().getGitRepositoryUrl(),
                getConfig().getGitRepositoryBranch(),
                getConfig().getGitRepositoryIdCommit(),
                getConfig().isGitRepositoryFirstCommit(),
                getConfig().getWorkspacePath(),
                notifiers);
        }

        inspector.getSerializers().add(new InspectorSerializer4GitRepository(engines, inspector));
        inspector.getSerializers().add(new PropertiesSerializer4GitRepository(engines, inspector));
        inspector.getSerializers().add(new InspectorTimeSerializer4GitRepository(engines, inspector));
        inspector.getSerializers().add(new PipelineErrorSerializer4GitRepository(engines, inspector));
        inspector.getSerializers().add(new PatchesSerializer4GitRepository(engines, inspector));
        inspector.getSerializers().add(new ToolDiagnosticSerializer4GitRepository(engines, inspector));
        inspector.getSerializers().add(new PullRequestSerializer4GitRepository(engines, inspector));

        return inspector;
	}

	private static ProjectInspector constructInspector4Default(BuildToBeInspected buildToBeInspected, List<SerializerEngine> engines, List<AbstractNotifier> notifiers) {
		ProjectInspector inspector;

		boolean shouldStaticAnalysis = getConfig().getRepairTools().contains(SonarQubeRepair.TOOL_NAME) && getConfig().getRepairTools().size() == 1;

		if (getConfig().getLauncherMode() == LauncherMode.BEARS) {
            inspector = InspectorFactory.getDefaultBearsInspector(buildToBeInspected, getConfig().getWorkspacePath(), notifiers);
        } else if (getConfig().getLauncherMode() == LauncherMode.CHECKSTYLE) {
            inspector = InspectorFactory.getDefaultCheckStyleInspector(buildToBeInspected, getConfig().getWorkspacePath(), notifiers);
        } else if (getConfig().getLauncherMode() == LauncherMode.REPAIR && !shouldStaticAnalysis){
            inspector = InspectorFactory.getDefaultTravisInspector(buildToBeInspected, getConfig().getWorkspacePath(), notifiers);
        } else {
            inspector = InspectorFactory.getStaticAnalysisTravisInspector(buildToBeInspected, getConfig().getWorkspacePath(), notifiers);
        }

		if (getConfig().getLauncherMode() == LauncherMode.BEARS) {
            inspector.getSerializers().add(new InspectorSerializer4Bears(engines, inspector));
        } else {
            inspector.getSerializers().add(new InspectorSerializer(engines, inspector));
        }

        inspector.getSerializers().add(new PropertiesSerializer(engines, inspector));
        inspector.getSerializers().add(new InspectorTimeSerializer(engines, inspector));
        inspector.getSerializers().add(new PipelineErrorSerializer(engines, inspector));
        inspector.getSerializers().add(new PatchesSerializer(engines, inspector));
        inspector.getSerializers().add(new ToolDiagnosticSerializer(engines, inspector));
        inspector.getSerializers().add(new PullRequestSerializer(engines, inspector));

        return inspector;
	}
}