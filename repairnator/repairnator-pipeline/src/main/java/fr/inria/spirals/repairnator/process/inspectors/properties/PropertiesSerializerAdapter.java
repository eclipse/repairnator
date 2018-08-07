package fr.inria.spirals.repairnator.process.inspectors.properties;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class PropertiesSerializerAdapter implements JsonSerializer<Properties> {
    private final Logger logger = LoggerFactory.getLogger(PropertiesSerializerAdapter.class);

    @Override
    public JsonElement serialize(Properties properties, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject result = new JsonObject();
        for (Method method : Properties.class.getDeclaredMethods()) {
            if ((method.getParameterCount() == 0) && (method.getName().startsWith("get"))) {
                String attributeName = method.getName().replace("get", "");
                attributeName = this.decapitalize(attributeName);

                try {
                    Object value = method.invoke(properties);
                    if (value instanceof JsonElement) {
                        result.add(attributeName, (JsonElement) value);
                    } else {
                        result.add(attributeName, jsonSerializationContext.serialize(value));
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    logger.error("Error while serializing properties.", e);
                }
            }
        }
        return result;
    }

    private String decapitalize(String string) {
        if (string == null || string.length() == 0) {
            return string;
        }
        char c[] = string.toCharArray();
        c[0] = Character.toLowerCase(c[0]);
        return new String(c);
    }
}
