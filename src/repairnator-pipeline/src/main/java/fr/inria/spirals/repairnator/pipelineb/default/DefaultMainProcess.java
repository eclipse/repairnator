package fr.inria.spirals.repairnator.pipeline;

import fr.inria.jtravis.JTravis;
import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.serializer.HardwareInfoSerializer;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector4Bears;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector4Checkstyle;
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
import fr.inria.spirals.repairnator.notifier.AbstractNotifier;
import fr.inria.spirals.repairnator.notifier.PatchNotifier;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.jtravis.entities.StateType;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.process.inspectors.InspectorFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.nio.file.Files;
import java.io.IOException;
import java.io.File;

/* Main repair process for the default use case of Repairnator after defining args, init notifiers, config and serializersEngines */
public class DefaultMainProcess implements MainProcess {
	private static Logger LOGGER = LoggerFactory.getLogger(DefaultMainProcess.class);
	protected ProjectInspector inspector;
	protected List<SerializerEngine> engines;
	protected List<AbstractNotifier> notifiers;
	protected PatchNotifier patchNotifier ;
	protected BuildToBeInspected buildToBeInspected;

    private IDefineJSAPArgs iDefineJSAPArgs;
    private IInitConfig iInitConfig;
    private IInitSerializerEngines iInitSerializerEngines;
    private IInitNotifiers iInitNotifiers;

	public DefaultMainProcess(List<SerializerEngine> engines,List<AbstractNotifier> notifiers,PatchNotifier patchNotifier) {
		this.engines = engines;
		this.notifiers = notifiers;
		this.patchNotifier = patchNotifier;
	}

    public DefaultMainProcess(IDefineJSAPArgs iDefineJSAPArgs, IInitConfig iInitConfig, IInitSerializerEngines iInitSerializerEngines, IInitNotifiers iInitNotifiers) {
        this.iDefineJSAPArgs = iDefineJSAPArgs;
        this.iInitConfig = iInitConfig;
        this.iInitNotifiers = iInitNotifiers;
        this.iInitSerializerEngines = iInitSerializerEngines;

        this.engines = iInitSerializerEngines.getEngines();
        this.notifiers = iInitNotifiers.getNotifiers();
        this.patchNotifier = iInitNotifiers.getPatchNotifers();
    }

	protected static RepairnatorConfig getConfig() {
        return RepairnatorConfig.getInstance();
    }

    public ProjectInspector getInspector() {
        return this.inspector;
    }

	protected boolean getBuildToBeInspected() {
        JTravis jTravis = this.getConfig().getJTravis();
        Optional<Build> optionalBuild = jTravis.build().fromId(this.getConfig().getBuildId());
        if (!optionalBuild.isPresent()) {
            LOGGER.error("Error while retrieving the buggy build. The process will exit now.");
            return false;
        }

        Build buggyBuild = optionalBuild.get();
        if (buggyBuild.getFinishedAt() == null) {
            LOGGER.error("Apparently the buggy build is not yet finished (maybe it has been restarted?). The process will exit now.");
            return false;
        }
        String runId = this.getConfig().getRunId();

        if (this.getConfig().getLauncherMode() == LauncherMode.BEARS) {
            Optional<Build> optionalBuildPatch = jTravis.build().fromId(this.getConfig().getNextBuildId());
            if (!optionalBuildPatch.isPresent()) {
                LOGGER.error("Error while getting patched build: null value was obtained. The process will exit now.");
                return false;
            }

            Build patchedBuild = optionalBuildPatch.get();
            LOGGER.info("The patched build (" + patchedBuild.getId() + ") was successfully retrieved from Travis.");

            if (buggyBuild.getState() == StateType.FAILED) {
                this.buildToBeInspected = new BuildToBeInspected(buggyBuild, patchedBuild, ScannedBuildStatus.FAILING_AND_PASSING, runId);
            } else {
                this.buildToBeInspected = new BuildToBeInspected(buggyBuild, patchedBuild, ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES, runId);
            }
        } else {
            Optional<Build> optionalNextPassing = jTravis.build().getAfter(buggyBuild, true, StateType.PASSED);
            if (optionalNextPassing.isPresent()) {
                this.buildToBeInspected = new BuildToBeInspected(buggyBuild, optionalNextPassing.get(), ScannedBuildStatus.FAILING_AND_PASSING, runId);
            } else {
                this.buildToBeInspected = new BuildToBeInspected(buggyBuild, null, ScannedBuildStatus.ONLY_FAIL, runId);
            }

            // switch off push mechanism in case of test project
            // and switch off serialization
            String project = buggyBuild.getRepository().getSlug().toLowerCase();
            if (this.getListOfProjectsToIgnore().contains(project)) {
                this.getConfig().setPush(false);
                this.getConfig().setFork(false);
                this.getConfig().setCreatePR(false);
                this.engines.clear();
                LOGGER.info("The build "+this.getConfig().getBuildId()+" is from a project to be ignored ("+project+"), thus the pipeline deactivated serialization for that build.");
            }
        }
        return true;
    }

    private List<String> getListOfProjectsToIgnore() {
        List<String> result = new ArrayList<>();
        if (this.getConfig().getProjectsToIgnoreFilePath() != null) {
            try {
                List<String> lines = Files.readAllLines(new File(this.getConfig().getProjectsToIgnoreFilePath()).toPath());
                for (String line : lines) {
                    result.add(line.trim().toLowerCase());
                }
            } catch (IOException e) {
                LOGGER.error("Error while reading projects to be ignored from file "+this.getConfig().getProjectsToIgnoreFilePath(), e);
            }
        }
        return result;
    }

    @Override
    public IDefineJSAPArgs getIDefineJSAPArgs() {
        return this.iDefineJSAPArgs;
    }

    @Override
    public IInitConfig getIInitConfig() {
        return this.iInitConfig;
    }

    @Override
    public IInitNotifiers getIInitNotifiers() {
        return this.iInitNotifiers;
    }

    @Override
    public IInitSerializerEngines getIInitSerializerEngines() {
        return this.iInitSerializerEngines;
    }

    @Override
    public void setPatchNotifier(PatchNotifier patchNotifier) {
        this.patchNotifier = patchNotifier;
    }

    @Override
    public PatchNotifier getPatchNotifier() {
        return this.patchNotifier;
    }

    @Override
	public boolean run() {
		LOGGER.info("Start by getting the build (buildId: "+this.getConfig().getBuildId()+") with the following config: "+this.getConfig());
        if (!this.getBuildToBeInspected()) {
            return false;
        }

        HardwareInfoSerializer hardwareInfoSerializer = new HardwareInfoSerializer(this.engines, this.getConfig().getRunId(), this.getConfig().getBuildId()+"");
        hardwareInfoSerializer.serialize();

        List<AbstractDataSerializer> serializers = new ArrayList<>();

        if (this.getConfig().getLauncherMode() == LauncherMode.BEARS) {
            inspector = new ProjectInspector4Bears(buildToBeInspected, this.getConfig().getWorkspacePath(), serializers, this.notifiers);
        } else if (this.getConfig().getLauncherMode() == LauncherMode.CHECKSTYLE) {
            inspector = new ProjectInspector4Checkstyle(buildToBeInspected, this.getConfig().getWorkspacePath(), serializers, this.notifiers);
        } else {
            inspector = InspectorFactory.getDefaultTravisInspector(buildToBeInspected, this.getConfig().getWorkspacePath(), serializers, this.notifiers);
        }

        System.out.println("Finished " + this.inspector.isPipelineEnding());
        if (this.getConfig().getLauncherMode() == LauncherMode.BEARS) {
            serializers.add(new InspectorSerializer4Bears(this.engines, inspector));
        } else {
            serializers.add(new InspectorSerializer(this.engines, inspector));
        }

        serializers.add(new PropertiesSerializer(this.engines, inspector));
        serializers.add(new InspectorTimeSerializer(this.engines, inspector));
        serializers.add(new PipelineErrorSerializer(this.engines, inspector));
        serializers.add(new PatchesSerializer(this.engines, inspector));
        serializers.add(new ToolDiagnosticSerializer(this.engines, inspector));
        serializers.add(new PullRequestSerializer(this.engines, inspector));

        inspector.setPatchNotifier(this.patchNotifier);
        inspector.run();

        LOGGER.info("Inspector is finished. The process will exit now.");
        return true;
	}
}