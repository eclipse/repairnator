package fr.inria.spirals.repairnator.serializer;

import com.google.gson.JsonObject;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.utils.DateUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * This abstract class is used to define a SerializerImpl
 * which will be used to store data.
 */
public abstract class SerializerImpl implements Serializer {

    private List<SerializerEngine> engines;
    private SerializerType type;

    public SerializerImpl(List<SerializerEngine> engines, SerializerType type) {
        if (engines == null) {
            this.engines = Collections.emptyList();
        } else {
            this.engines = engines;
        }
        this.type = type;
    }

    public List<SerializerEngine> getEngines() {
        return engines;
    }

    @Override
    public SerializerType getType() {
        return type;
    }


    /**
     * This utility method should be used to create proper date to be used in MongoDB.
     * It create the right propertyName and put it in the result JSON object.
     */
    public void addDate(JsonObject result, String propertyName, Date value) {
        JsonObject intermediateObject = new JsonObject();
        intermediateObject.addProperty("$date", DateUtils.formatDateForMongo(value));
        result.add(propertyName, intermediateObject);
    }
}
