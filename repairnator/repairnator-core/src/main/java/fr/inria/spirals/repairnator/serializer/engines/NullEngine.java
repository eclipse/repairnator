package fr.inria.spirals.repairnator.serializer.engines;

import fr.inria.spirals.repairnator.serializer.SerializerType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class NullEngine implements SerializerEngine {
    @Override
    public void serialize(List<SerializedData> data, SerializerType serializer) {
        // nothing
    }
}
