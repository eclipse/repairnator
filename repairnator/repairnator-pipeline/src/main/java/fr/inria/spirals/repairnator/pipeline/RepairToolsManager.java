package fr.inria.spirals.repairnator.pipeline;

import fr.inria.spirals.repairnator.process.step.repair.AssertFixerRepair;
import fr.inria.spirals.repairnator.process.step.repair.AstorRepair;
import fr.inria.spirals.repairnator.process.step.repair.NPERepair;
import fr.inria.spirals.repairnator.process.step.repair.NopolRepair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class RepairToolsManager {
	private static RepairToolsManager instance;
	private static Logger LOGGER = LoggerFactory.getLogger(RepairToolsManager.class);
	private Set<String> availableRepairTools;

	static {
		AssertFixerRepair.init();
		AstorRepair.init();
		NopolRepair.init();
		NPERepair.init();
	}

	private RepairToolsManager() {
		this.availableRepairTools = new HashSet<>();
	}

	private static RepairToolsManager getInstance() {
		if (instance == null) {
			instance = new RepairToolsManager();
		}
		return instance;
	}

	public static void registerRepairTool(String name) {
		LOGGER.info("Record repair tool: " + name);
		if (getInstance().availableRepairTools.contains(name)) {
			LOGGER.warn(name+" tool has already been registered. Previous one will be ignored.");
		}
		getInstance().availableRepairTools.add(name);
	}

	public static Set<String> getAvailableRepairTools() {
		return getInstance().availableRepairTools;
	}
}
