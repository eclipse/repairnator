package fr.inria.spirals.repairnator.pipelineb;

import fr.inria.spirals.repairnator.LauncherUtils;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* serializerEngines init behavior for the default use case of Repairnator */
public class DefaultInitSerializerEngines implements IInitSerializerEngines {
	private static Logger LOGGER = LoggerFactory.getLogger(DefaultInitSerializerEngines.class);
    protected List<SerializerEngine> engines;

	@Override
	public void initSerializerEngines() {
        this.engines = new ArrayList<>();

        List<SerializerEngine> fileSerializerEngines = LauncherUtils.initFileSerializerEngines(LOGGER);
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