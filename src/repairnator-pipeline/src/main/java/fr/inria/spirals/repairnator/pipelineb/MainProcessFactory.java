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

		/* HENRY - put this in the getHardwareInforSerilizer later*/
		HardwareInfoSerializer hardwareInfoSerializer = getHardwareInfoSerializer(defaultInitSerializerEngines.getEngines());
		hardwareInfoSerializer.serialize();

		ProjectInspector inspector = constructInspector4Default(defaultMainProcess,defaultInitSerializerEngines.getEngines());

		/* HENRY - lift the other non relevant function to here */
		defaultMainProcess.setInspector(inspector)
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

		HardwareInfoSerializer hardwareInfoSerializer = getHardwareInfoSerializer(githubInitSerializerEngines.getEngines());
		hardwareInfoSerializer.serialize();

		/*  HENRY - paramenters instead of github mainprocess*/
		ProjectInspector inspector =  constructInspector4Github(githubMainProcess, githubInitSerializerEngines.getEngines());

		githubMainProcess.setInspector(inspector)
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



	private static HardwareInfoSerializer getHardwareInfoSerializer(List<SerializerEngine> engines) {
		HardwareInfoSerializer hardwareInfoSerializer = new HardwareInfoSerializer(engines, getConfig().getRunId(), getConfig().getBuildId()+"");
        hardwareInfoSerializer.serialize();

        return hardwareInfoSerializer;
	}

	/* These methods below should be called after all other inits */
	/* move serializer into project inspector and get to construct */
	private static ProjectInspector constructInspector4Github(GithubMainProcess githubMainProcess,List<SerializerEngine> engines) {
		List<AbstractDataSerializer> serializers = new ArrayList<>();
		ProjectInspector inspector;

		boolean shouldStaticAnalysis = getConfig().getRepairTools().contains(SonarQubeRepair.TOOL_NAME) && getConfig().getRepairTools().size() == 1;

		if (shouldStaticAnalysis) {
            inspector = InspectorFactory.getStaticAnalysisGithubInspector(
                getConfig().getGitRepositoryUrl(),
                getConfig().getGitRepositoryBranch(),
                getConfig().getGitRepositoryIdCommit(),
                getConfig().isGitRepositoryFirstCommit(),
                getConfig().getWorkspacePath(),
                serializers,
                githubMainProcess.getNotifiers()
            );
        } else {
            inspector = InspectorFactory.getDefaultGithubInspector(
                getConfig().getGitRepositoryUrl(),
                getConfig().getGitRepositoryBranch(),
                getConfig().getGitRepositoryIdCommit(),
                getConfig().isGitRepositoryFirstCommit(),
                getConfig().getWorkspacePath(),
                serializers,
                githubMainProcess.getNotifiers());
        }

		if (getConfig().getLauncherMode() == LauncherMode.BEARS) {
            serializers.add(new InspectorSerializer4Bears(engines, inspector));
        } else {
            serializers.add(new InspectorSerializer(engines, inspector));
        }

        serializers.add(new InspectorSerializer4GitRepository(engines, inspector));
        serializers.add(new PropertiesSerializer4GitRepository(engines, inspector));
        serializers.add(new InspectorTimeSerializer4GitRepository(engines, inspector));
        serializers.add(new PipelineErrorSerializer4GitRepository(engines, inspector));
        serializers.add(new PatchesSerializer4GitRepository(engines, inspector));
        serializers.add(new ToolDiagnosticSerializer4GitRepository(engines, inspector));
        serializers.add(new PullRequestSerializer4GitRepository(engines, inspector));

        return inspector;
	}

	private static ProjectInspector constructInspector4Default(DefaultMainProcess defaultMainProcess,List<SerializerEngine> engines) {
		List<AbstractDataSerializer> serializers = new ArrayList<>();
		ProjectInspector inspector;

		boolean shouldStaticAnalysis = getConfig().getRepairTools().contains(SonarQubeRepair.TOOL_NAME) && getConfig().getRepairTools().size() == 1;

		if (getConfig().getLauncherMode() == LauncherMode.BEARS) {
            inspector = InspectorFactory.getDefaultBearsInspector(defaultMainProcess.getBuildToBeInspected(), getConfig().getWorkspacePath(), serializers, defaultMainProcess.getNotifiers());
        } else if (getConfig().getLauncherMode() == LauncherMode.CHECKSTYLE) {
            inspector = InspectorFactory.getDefaultCheckStyleInspector(defaultMainProcess.getBuildToBeInspected(), getConfig().getWorkspacePath(), serializers, defaultMainProcess.getNotifiers());
        } else if (getConfig().getLauncherMode() == LauncherMode.REPAIR && !shouldStaticAnalysis){
            inspector = InspectorFactory.getDefaultTravisInspector(defaultMainProcess.getBuildToBeInspected(), getConfig().getWorkspacePath(), serializers, defaultMainProcess.getNotifiers());
        } else {
            inspector = InspectorFactory.getStaticAnalysisTravisInspector(defaultMainProcess.getBuildToBeInspected(), getConfig().getWorkspacePath(), serializers, defaultMainProcess.getNotifiers());
        }

		if (getConfig().getLauncherMode() == LauncherMode.BEARS) {
            serializers.add(new InspectorSerializer4Bears(engines, inspector));
        } else {
            serializers.add(new InspectorSerializer(engines, inspector));
        }

        serializers.add(new PropertiesSerializer(engines, inspector));
        serializers.add(new InspectorTimeSerializer(engines, inspector));
        serializers.add(new PipelineErrorSerializer(engines, inspector));
        serializers.add(new PatchesSerializer(engines, inspector));
        serializers.add(new ToolDiagnosticSerializer(engines, inspector));
        serializers.add(new PullRequestSerializer(engines, inspector));

        return inspector;
	}
}