package fr.inria.spirals.repairnator.pipeline;

import fr.inria.spirals.repairnator.notifier.AbstractNotifier;
import fr.inria.spirals.repairnator.notifier.PatchNotifier;

import java.util.List;

public interface IInitNotifiers {

	void initNotifiers();

	List<AbstractNotifier> getNotifiers();

    PatchNotifier getPatchNotifiers();

}