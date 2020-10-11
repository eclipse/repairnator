package fr.inria.spirals.repairnator.pipelineb;

import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;

import java.util.List;

public interface IInitSerializerEngines {

	void initSerializerEngines();

	List<SerializerEngine> getEngines();
}