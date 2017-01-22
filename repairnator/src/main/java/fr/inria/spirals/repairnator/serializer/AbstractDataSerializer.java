package fr.inria.spirals.repairnator.serializer;

import fr.inria.spirals.repairnator.process.ProjectInspector;

/**
 * Created by urli on 20/01/2017.
 */
public abstract class AbstractDataSerializer {

    public AbstractDataSerializer() {}

    public abstract void serializeData(ProjectInspector inspector);
}
