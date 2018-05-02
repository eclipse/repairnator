package fr.inria.spirals.repairnator.pipeline;

import fr.inria.spirals.repairnator.process.step.repair.AbstractRepairStep;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

public class RepairToolsManager {
    private static RepairToolsManager instance;
    private Map<String, AbstractRepairStep> repairTools;
    private ServiceLoader<AbstractRepairStep> repairToolLoader = ServiceLoader.load(AbstractRepairStep.class);

    private RepairToolsManager() {
        this.repairTools = new HashMap<>();
        this.discoverRepairTools();
    }

    private static RepairToolsManager getInstance() {
        if (instance == null) {
            instance = new RepairToolsManager();
        }
        return instance;
    }

    public void discoverRepairTools() {
        this.repairTools.clear();
        this.repairToolLoader.reload();

        for (AbstractRepairStep repairStep : this.repairToolLoader) {
            this.repairTools.put(repairStep.getRepairToolName(), repairStep);
        }
    }

    public static AbstractRepairStep getStepFromName(String name) {
        return getInstance().repairTools.get(name);
    }

    public static Set<String> getRepairToolsName() {
        return getInstance().repairTools.keySet();
    }
}
