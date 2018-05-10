package fr.inria.spirals.repairnator.serializer;

/**
 * Created by urli on 27/03/2017.
 */
public enum SerializerType {
    // pipeline serializers
    INSPECTOR("inspector"),
    INSPECTOR4BEARS("inspector4bears"),
    TIMES("times"),
    TIMES4BEARS("times4bears"),
    NOPOL("nopol"),
    NPEFIX("npefix"),
    ASTOR("astor"),
    PIPELINE_ERRORS("pipeline-errors"),
    ASSERT_FIXER("assertFixer"),

    // dockerpool
    TREATEDBUILD("treatedbuild"),
    ENDPROCESS("endprocess"),

    // scanner
    DETAILEDDATA("detailedData"),
    SCANNER("scanner"),
    SCANNER4BEARS("scanner4bears"),
    HARDWARE_INFO("hardwareInfo"),
    METRICS("bearmetrics"),
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
