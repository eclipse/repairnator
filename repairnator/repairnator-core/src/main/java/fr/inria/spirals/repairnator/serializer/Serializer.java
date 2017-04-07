package fr.inria.spirals.repairnator.serializer;

import com.google.gson.JsonObject;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by urli on 09/03/2017.
 */
public abstract class Serializer {

    private static final String MONGO_UTC_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final SimpleDateFormat MONGO_DATE_FORMAT = new SimpleDateFormat(MONGO_UTC_FORMAT);
    private List<SerializerEngine> engines;
    private SerializerType type;

    public Serializer(List<SerializerEngine> engines, SerializerType type) {
        if (engines == null) {
            this.engines = Collections.EMPTY_LIST;
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

    public void addDate(JsonObject result, String propertyName, Date value) {
        JsonObject intermediateObject = new JsonObject();
        intermediateObject.addProperty("$date", MONGO_DATE_FORMAT.format(value));
        result.add(propertyName, intermediateObject);
    }
}
