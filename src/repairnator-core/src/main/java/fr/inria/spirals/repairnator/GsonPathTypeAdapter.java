package fr.inria.spirals.repairnator;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * This class can be used in gson to properly serialize Path object.
 */
public class GsonPathTypeAdapter extends TypeAdapter<Path> {
    @Override
    public void write(JsonWriter jsonWriter, Path path) throws IOException {
        jsonWriter.beginObject();
        jsonWriter.name("path").value(path.toAbsolutePath().toString());
        jsonWriter.endObject();
    }

    @Override
    public Path read(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();
        String absolutePath = jsonReader.nextString();
        jsonReader.endObject();
        return new File(absolutePath).toPath();
    }
}
