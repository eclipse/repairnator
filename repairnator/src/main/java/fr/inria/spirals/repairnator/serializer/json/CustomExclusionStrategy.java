package fr.inria.spirals.repairnator.serializer.json;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

import java.lang.reflect.Modifier;

/**
 * Created by urli on 11/01/2017.
 */
public class CustomExclusionStrategy implements ExclusionStrategy {
    public boolean shouldSkipField(FieldAttributes fieldAttributes) {
        return (
                fieldAttributes.getName().equals("lastBuild") ||
                fieldAttributes.getName().equals("logger") ||
                fieldAttributes.getDeclaredClass().equals(Class.class) ||
                fieldAttributes.hasModifier(Modifier.PROTECTED)
        );
    }

    public boolean shouldSkipClass(Class<?> aClass) {
        return false;
    }
}