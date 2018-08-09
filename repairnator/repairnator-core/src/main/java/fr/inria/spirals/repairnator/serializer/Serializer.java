package fr.inria.spirals.repairnator.serializer;

import com.google.gson.JsonObject;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * This abstract class is used to define a Serializer
 * which will be used to store data.
 */
public abstract class Serializer {

    private static final String MONGO_UTC_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final SimpleDateFormat MONGO_DATE_FORMAT = new SimpleDateFormat(MONGO_UTC_FORMAT);
    private List<SerializerEngine> engines;
    private SerializerType type;

    public Serializer(List<SerializerEngine> engines, SerializerType type) {
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

    public SerializerType getType() {
        return type;
    }

    /**
     * This utility method should be used to create proper date to be used in MongoDB.
     * It create the right propertyName and put it in the result JSON object.
     */
    public void addDate(JsonObject result, String propertyName, Date value) {
        JsonObject intermediateObject = new JsonObject();
        intermediateObject.addProperty("$date", MONGO_DATE_FORMAT.format(value));
        result.add(propertyName, intermediateObject);
    }
}
