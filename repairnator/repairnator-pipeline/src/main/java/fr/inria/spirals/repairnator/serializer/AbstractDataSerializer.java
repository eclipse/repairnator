package fr.inria.spirals.repairnator.serializer;

import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;

import java.util.List;


/**
 * Created by urli on 20/01/2017.
 */
public abstract class AbstractDataSerializer extends Serializer {



    public AbstractDataSerializer(List<SerializerEngine> engines, SerializerType type) {
        super(engines, type);
    }

    protected String getPrettyPrintState(ProjectInspector inspector) {

        JobStatus jobStatus = inspector.getJobStatus();
        if (jobStatus.isHasBeenPatched()) {
            return "PATCHED";
        }

        if (jobStatus.isReproducedAsFail()) {
            return "test failure";
        }

        if (jobStatus.isReproducedAsError()) {
            return "test errors";
        }

        return jobStatus.getPipelineState().name();
    }

    public abstract void serializeData(ProjectInspector inspector);
}
