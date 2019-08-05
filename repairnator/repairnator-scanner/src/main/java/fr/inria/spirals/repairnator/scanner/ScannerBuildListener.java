package fr.inria.spirals.repairnator.scanner;

public class ScannerBuildListener {
    private static ScannerBuildListener scannerBuildListener;
    private static Launcher launcher;

    public ScannerBuildListener (){}
    public void setLauncher (Launcher launcher) {
        this.launcher = launcher;
    }

    public static ScannerBuildListener getInstance() {
        if (scannerBuildListener == null) {
            scannerBuildListener = new ScannerBuildListener();
        }
        return scannerBuildListener;
    }
    public void runAsConsumerServer() {}
}
