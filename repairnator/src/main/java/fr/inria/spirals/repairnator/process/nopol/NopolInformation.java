package fr.inria.spirals.repairnator.process.nopol;

import fr.inria.lille.repair.common.config.Config;
import fr.inria.lille.repair.common.patch.Patch;
import fr.inria.spirals.repairnator.process.testinformation.FailureLocation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by urli on 16/02/2017.
 */
public class NopolInformation {
    private FailureLocation location;
    private NopolStatus status;
    private Date dateEnd;
    private int allocatedTime;
    private int passingTime;
    private List<Patch> patches;
    private Config config;
    private String exceptionDetail;

    public NopolInformation(FailureLocation location) {
        this.status = NopolStatus.NOTLAUNCHED;
        this.patches = new ArrayList<>();
        this.location = location;
    }

    public NopolStatus getStatus() {
        return status;
    }

    public void setStatus(NopolStatus status) {
        this.status = status;
    }

    public Date getDateEnd() {
        return dateEnd;
    }

    public void setDateEnd() {
        this.dateEnd = new Date();
    }

    public int getAllocatedTime() {
        return allocatedTime;
    }

    public void setAllocatedTime(int allocatedTime) {
        this.allocatedTime = allocatedTime;
    }

    public int getPassingTime() {
        return passingTime;
    }

    public void setPassingTime(int passingTime) {
        this.passingTime = passingTime;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public String getExceptionDetail() {
        return exceptionDetail;
    }

    public void setExceptionDetail(String exceptionDetail) {
        this.exceptionDetail = exceptionDetail;
    }

    public List<Patch> getPatches() {
        return patches;
    }

    public void setPatches(List<Patch> patches) {
        this.patches = patches;
    }

    public FailureLocation getLocation() {
        return location;
    }
}
