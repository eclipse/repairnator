package fr.inria.spirals.repairnator.serializer.engines;

import fr.inria.spirals.repairnator.serializer.SerializerType;

import java.util.List;

/**
 * Created by urli on 27/03/2017.
 */
public interface SerializerEngine {
    void serialize(List<SerializedData> data, SerializerType serializer);
}
