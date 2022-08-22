package fr.inria.spirals.repairnator.realtime;

import java.util.Date;

/**
 * It contains the information about the projects monitored with FlacocoScanner.
 */
public class RepositoryScanInformation {
    private Date startDateForScanning;
    private Date scanEndsAt;
    private boolean isFirstScan;

    public RepositoryScanInformation(Date startDateForScanning, Date scanEndsAt, boolean isFirstScan) {
        this.startDateForScanning = startDateForScanning;
        this.scanEndsAt = scanEndsAt;
        this.isFirstScan = isFirstScan;
    }

    public Date getStartDateForScanning() {
        return startDateForScanning;
    }

    public void setStartDateForScanning(Date startDateForScanning) {
        this.startDateForScanning = startDateForScanning;
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
                "startDateForScanning=" + startDateForScanning +
                ", scanEndsAt=" + scanEndsAt +
                ", isFirstScan=" + isFirstScan +
                '}';
    }
}
