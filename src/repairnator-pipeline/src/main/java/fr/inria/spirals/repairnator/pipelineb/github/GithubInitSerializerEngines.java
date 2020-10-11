package fr.inria.spirals.repairnator.pipelineb.github;

import fr.inria.spirals.repairnator.LauncherUtils;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.GitRepositoryLauncherUtils;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* SerializerEngines init behavior for repairing with Github instead of Travis */
public class GithubInitSerializerEngines implements IInitSerializerEngines {
	private static Logger LOGGER = LoggerFactory.getLogger(TravisInitSerializerEngines.class);
    protected List<SerializerEngine> engines;

	@Override
	public void initSerializerEngines() {
        this.engines = new ArrayList<>();

        List<SerializerEngine> fileSerializerEngines = GitRepositoryLauncherUtils.initFileSerializerEngines(LOGGER);
        this.engines.addAll(fileSerializerEngines);

        SerializerEngine mongoDBSerializerEngine = LauncherUtils.initMongoDBSerializerEngine(LOGGER);
        if (mongoDBSerializerEngine != null) {
            this.engines.add(mongoDBSerializerEngine);
        }
    }

    @Override
    public List<SerializerEngine> getEngines() {
    	return this.engines;
    }
}