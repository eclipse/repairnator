package fr.inria.spirals.repairnator.serializer.engines;

import com.google.gson.JsonElement;

import java.util.List;

/**
 * Created by urli on 27/03/2017.
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
