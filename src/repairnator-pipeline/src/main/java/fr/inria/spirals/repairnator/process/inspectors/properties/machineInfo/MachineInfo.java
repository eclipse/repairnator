package fr.inria.spirals.repairnator.process.inspectors.properties.machineInfo;

public class MachineInfo {

    private String hostName;
    private int numberCPU;
    private long freeMemory;
    private long totalMemory;

    public MachineInfo() {}

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getNumberCPU() {
        return numberCPU;
    }

    public void setNumberCPU(int numberCPU) {
        this.numberCPU = numberCPU;
    }

    public long getFreeMemory() {
        return freeMemory;
    }

    public void setFreeMemory(long freeMemory) {
        this.freeMemory = freeMemory;
    }

    public long getTotalMemory() {
        return totalMemory;
    }

    public void setTotalMemory(long totalMemory) {
        this.totalMemory = totalMemory;
    }
}
