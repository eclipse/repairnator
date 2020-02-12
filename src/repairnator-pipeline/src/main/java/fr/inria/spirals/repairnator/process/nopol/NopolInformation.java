package fr.inria.spirals.repairnator.process.nopol;

import fr.inria.lille.repair.common.config.NopolContext;
import fr.inria.spirals.repairnator.process.testinformation.FailureLocation;

import java.util.Set;

/**
 * Created by urli on 16/02/2017.
 */
public class NopolInformation {
    private Set<FailureLocation> location;
    private NopolStatus status;
    private int allocatedTime;
    private int passingTime;
    private NopolContext nopolContext;
    private String exceptionDetail;
    private int nbStatements;
    private int nbAngelicValues;
    private IgnoreStatus ignoreStatus;

    public NopolInformation(Set<FailureLocation> location, IgnoreStatus ignoreStatus) {
        this.status = NopolStatus.NOTLAUNCHED;
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


    public Set<FailureLocation> getLocation() {
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
                ", allocatedTime=" + allocatedTime +
                ", passingTime=" + passingTime +
                ", nopolContext=" + nopolContext +
                ", exceptionDetail='" + exceptionDetail + '\'' +
                ", nbStatements=" + nbStatements +
                ", nbAngelicValues=" + nbAngelicValues +
                ", ignoreStatus=" + ignoreStatus +
                '}';
    }
}
