package fr.inria.spirals.repairnator.pipeline;

import fr.inria.spirals.repairnator.pipeline.LauncherAPI;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.notifier.PatchNotifier;

import com.martiansoftware.jsap.JSAP;

public class Launcher implements LauncherAPI{
	private String[] args;

	public Launcher(String[] args) {
		this.args = args;
	}

	@Override
	public void launch() {

	}

	@Override
	public RepairnatorConfig getConfig() {
        return RepairnatorConfig.getInstance();
    }

    @Override
    public JSAP defineArgs() {
    	return null;
    }

    @Override
    public boolean mainProcess() {
        return true;
    }

    @Override
    public ProjectInspector getInspector() {
        return null;
    }

    @Override
    public void setPatchNotifier(PatchNotifier patchNotifier) {
    }
}