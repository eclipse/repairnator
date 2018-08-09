package fr.inria.spirals.repairnator.serializer.engines;

import com.google.gson.JsonElement;

import java.util.List;

/**
 * In Repairnator, a data can be serialized in JSON or in lists for format such as CSV
 */
public class SerializedData {

    private List<Object> asList;
    private JsonElement asJson;

    public SerializedData(List<Object> list, JsonElement jsonElement) {
        this.asList = list;
        this.asJson = jsonElement;
    }

    public List<Object> getAsList() {
        return asList;
    }

    public JsonElement getAsJson() {
        return asJson;
    }
}
