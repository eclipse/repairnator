package fr.inria.spirals.repairnator.realtime;

import java.util.Date;

/**
 * It contains the information about the projects monitored with FlacocoScanner.
 */
public class RepositoryScanInformation {
    private Date scanEndsAt;
    private boolean isFirstScan;

    public RepositoryScanInformation(Date scanEndsAt, boolean isFirstScan) {
        this.scanEndsAt = scanEndsAt;
        this.isFirstScan = isFirstScan;
    }

    public Date getScanEndsAt() {
        return scanEndsAt;
    }

    public void setScanEndsAt(Date scanEndsAt) {
        this.scanEndsAt = scanEndsAt;
    }

    public boolean isFirstScan() {
        return isFirstScan;
    }

    public void setFirstScan(boolean firstScan) {
        isFirstScan = firstScan;
    }

    @Override
    public String toString() {
        return "RepositoryScanInformation{" +
                "scanEndsAt=" + scanEndsAt +
                ", isFirstScan=" + isFirstScan +
                '}';
    }
}
