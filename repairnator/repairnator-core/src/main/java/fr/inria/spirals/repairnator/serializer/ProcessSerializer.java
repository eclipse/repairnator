package fr.inria.spirals.repairnator.serializer;

import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;

import java.util.List;

/**
 * Created by urli on 27/03/2017.
 */
public abstract class ProcessSerializer extends Serializer {

    public ProcessSerializer(List<SerializerEngine> engines, SerializerType type) {
        super(engines, type);
    }

    public abstract void serialize();
}
