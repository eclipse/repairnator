package fr.inria.spirals.jtravis.helpers;

public class Version {

    private static Boolean versionV3 = true;

    public static void setVersion(Boolean version) {
        versionV3 = version;
    }

    public static Boolean getVersionV3() {
        return versionV3;
    }
}
