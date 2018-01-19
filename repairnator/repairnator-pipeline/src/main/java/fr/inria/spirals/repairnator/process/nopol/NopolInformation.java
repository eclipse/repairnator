package fr.inria.spirals.repairnator.process.nopol;

import fr.inria.lille.repair.common.config.NopolContext;
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
    private List<PatchAndDiff> patches;
    private NopolContext nopolContext;
    private String exceptionDetail;
    private int nbStatements;
    private int nbAngelicValues;
    private IgnoreStatus ignoreStatus;

    public NopolInformation(FailureLocation location, IgnoreStatus ignoreStatus) {
        this.status = NopolStatus.NOTLAUNCHED;
        this.patches = new ArrayList<>();
        this.location = location;
        this.ignoreStatus = ignoreStatus;
    }

    public IgnoreStatus getIgnoreStatus() {
        return ignoreStatus;
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

    public String getExceptionDetail() {
        return exceptionDetail;
    }

    public void setExceptionDetail(String exceptionDetail) {
        this.exceptionDetail = exceptionDetail;
    }

    public List<PatchAndDiff> getPatches() {
        return patches;
    }

    public void setPatches(List<PatchAndDiff> patches) {
        this.patches = patches;
    }

    public FailureLocation getLocation() {
        return location;
    }

    public NopolContext getNopolContext() {
        return nopolContext;
    }

    public void setNopolContext(NopolContext nopolContext) {
        this.nopolContext = nopolContext;
    }

    public int getNbStatements() {
        return nbStatements;
    }

    public void setNbStatements(int nbStatements) {
        this.nbStatements = nbStatements;
    }

    public int getNbAngelicValues() {
        return nbAngelicValues;
    }

    public void setNbAngelicValues(int nbAngelicValues) {
        this.nbAngelicValues = nbAngelicValues;
    }

    @Override
    public String toString() {
        return "NopolInformation{" +
                "location=" + location +
                ", status=" + status +
                ", dateEnd=" + dateEnd +
                ", allocatedTime=" + allocatedTime +
                ", passingTime=" + passingTime +
                ", nbPatches=" + patches.size() +
                ", nopolContext=" + nopolContext +
                ", exceptionDetail='" + exceptionDetail + '\'' +
                ", nbStatements=" + nbStatements +
                ", nbAngelicValues=" + nbAngelicValues +
                ", ignoreStatus=" + ignoreStatus +
                '}';
    }
}
