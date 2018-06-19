package fr.inria.spirals.repairnator.process.inspectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Created by urli on 28/04/2017.
 */
public class MetricsSerializerAdapter implements JsonSerializer<Metrics> {
    private final Logger logger = LoggerFactory.getLogger(MetricsSerializerAdapter.class);

    @Override
    public JsonElement serialize(Metrics metrics, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject result = new JsonObject();
        for (Method method : Metrics.class.getDeclaredMethods()) {
            if ((method.getParameterCount() == 0) && (method.getName().startsWith("get"))) {
                String attributeName = method.getName().replace("get", "");

                try {
                    Object value = method.invoke(metrics);
                    if (value instanceof JsonElement) {
                        result.add(attributeName, (JsonElement) value);
                    } else {
                        result.add(attributeName, jsonSerializationContext.serialize(value));
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    logger.error("Error while serializing metrics.", e);
                }
            }
        }
        return result;
    }
}
