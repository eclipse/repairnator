package fr.inria.spirals.repairnator.pipelineb.github;

import fr.inria.jtravis.JTravis;
import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.serializer.HardwareInfoSerializer;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.notifier.AbstractNotifier;
import fr.inria.spirals.repairnator.notifier.PatchNotifier;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.jtravis.entities.StateType;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.GitRepositoryProjectInspector;


import fr.inria.spirals.repairnator.serializer.InspectorSerializer4GitRepository;
import fr.inria.spirals.repairnator.serializer.InspectorTimeSerializer4GitRepository;
import fr.inria.spirals.repairnator.serializer.PatchesSerializer4GitRepository;
import fr.inria.spirals.repairnator.serializer.PipelineErrorSerializer4GitRepository;
import fr.inria.spirals.repairnator.serializer.PropertiesSerializer4GitRepository;
import fr.inria.spirals.repairnator.serializer.ToolDiagnosticSerializer4GitRepository;
import fr.inria.spirals.repairnator.serializer.PullRequestSerializer4GitRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.nio.file.Files;
import java.io.IOException;
import java.io.File;

/* Main repair process for repairing with Github instead of Travis */
public class GithubMainProcess implements MainProcess {
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

    public GithubMainProcess(IDefineJSAPArgs iDefineJSAPArgs, IInitConfig iInitConfig, IInitSerializerEngines iInitSerializerEngines, IInitNotifiers iInitNotifiers) {
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

    public GithubMainProcess setInspector(ProjectInspector inspector) {
        this.inspector = inspector;
        return this;
    }

    public List<AbstractNotifier> getNotifiers() {
        return this.notifiers;
    }

    public GithubMainProcess setNotifiers(List<AbstractNotifier> notifiers) {
        this.notifiers = notifiers;
        return this;
    }
    
    public List<SerializerEngine> getEngines() {
        return this.engines;
    }

    public GithubMainProcess setEngines(List<SerializerEngine> engines) {
        this.engines = engines;
        return this;
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
        inspector.setPatchNotifier(this.patchNotifier);
        inspector.run();


        LOGGER.info("Inspector is finished. The process will exit now.");
        return true;
	}
}