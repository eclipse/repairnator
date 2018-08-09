package fr.inria.spirals.repairnator.serializer;

/**
 * Created by urli on 27/03/2017.
 */
public enum SerializerType {
    // pipeline serializers
    INSPECTOR("inspector"),
    INSPECTOR4BEARS("inspector4bears"),
    TIMES("times"),
    PIPELINE_ERRORS("pipeline-errors"),
    PATCHES("patches"),
    TOOL_DIAGNOSTIC("tool-diagnostic"),

    // dockerpool
    TREATEDBUILD("treatedbuild"),
    ENDPROCESS("endprocess"),

    // scanner
    DETAILEDDATA("detailedData"),
    SCANNER("scanner"),
    SCANNER4BEARS("scanner4bears"),
    HARDWARE_INFO("hardwareInfo"),
    PROPERTIES("properties"),
    RTSCANNER("rtscanner"),
    BLACKLISTED("blacklisted")
    ;

    private String filename;

    SerializerType(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }
}
