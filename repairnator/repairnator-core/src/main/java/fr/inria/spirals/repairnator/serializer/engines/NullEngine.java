package fr.inria.spirals.repairnator.serializer.engines;

import fr.inria.spirals.repairnator.serializer.SerializerType;

import java.util.List;

public class NullEngine implements SerializerEngine {
    @Override
    public void serialize(List<SerializedData> data, SerializerType serializer) {
        // nothing
    }
}
