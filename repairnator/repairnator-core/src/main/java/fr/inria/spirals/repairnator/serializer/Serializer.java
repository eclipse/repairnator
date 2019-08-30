package fr.inria.spirals.repairnator.serializer;

/** used to save data */
public interface Serializer {
    SerializerType getType();
    void serialize();
}
