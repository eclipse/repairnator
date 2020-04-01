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

/* This will manufacture different kind of repairnator type */
public class MainProcessFactory {

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

		DefaultMainProcess defaultMainProcess = new DefaultMainProcess(defaultDefineJSAPArgs,defaultInitConfig,defaultInitSerializerEngines,defaultInitNotifiers);
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

		GithubMainProcess githubMainProcess = new GithubMainProcess(githubDefineJSAPArgs,githubInitConfig,githubInitSerializerEngines,githubInitNotifiers);
		return githubMainProcess;
	}

	/* */
	public static MainProcess getPipelineListenerMainProcess(String[] inputArgs) {
		MainProcess defaultMainProcess = getDefaultMainProcess(inputArgs);
		return new PipelineBuildListenerMainProcess(defaultMainProcess);
	}

	public static void getJenkinsMainProcess() {
		/* todo */
	}
}