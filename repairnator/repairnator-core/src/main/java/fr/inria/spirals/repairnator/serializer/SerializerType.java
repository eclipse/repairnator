package fr.inria.spirals.repairnator.serializer;

/**
 * Created by urli on 27/03/2017.
 */
public enum SerializerType {
    // pipeline serializers
    INSPECTOR("inspector","All data!A1:L1"),
    INSPECTOR4BEARS("inspector4bears","All data!A1:P1"),
    TIMES("times","Duration Data!A1:P1"),
    TIMES4BEARS("times4bears","Duration Data!A1:T1"),
    NOPOL("nopol","Nopol Stats!A1:T1"),
    NPEFIX("npefix","NPEFix!A1:Z1"),
    ASTOR("astor","Astor!A1:Z1"),

    // dockerpool
    TREATEDBUILD("treatedbuild","Treated Build Tracking!A1:G1"),
    ENDPROCESS("endprocess","End Process!A1:I1"),

    // scanner
    DETAILEDDATA("detailedData","Scanner Detailed Data!A1:K1"),
    SCANNER("scanner","Scanner Data!A1:N1"),
    SCANNER4BEARS("scanner4bears","Scanner Data!A1:P1"),
    HARDWARE_INFO("hardwareInfo","Hardware Info!A:V"),
    METRICS("bearmetrics","Bear Metrics!A:Z"),
    RTSCANNER("rtscanner","RTScanner!A:Z"),
    BLACKLISTED("blacklisted", "Blacklisted!A:Z")
    ;

    private String filename;
    private String range;

    SerializerType(String filename, String range) {
        this.filename = filename;
        this.range = range;
    }

    public String getFilename() {
        return filename;
    }

    public String getRange() {
        return range;
    }
}
