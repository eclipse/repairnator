package fr.inria.spirals.repairnator.scanner;

import fr.inria.spirals.repairnator.BuildListener;
import javax.jms.Message;

public class ScannerBuildListener implements BuildListener{
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


    /**
     * Method implemented from MessageListener and is called 
     * each time this is done with the previous message
     *
     * @param message ActiveMQ message object containing a string message.
     */
    public void onMessage(Message message) {}
}
