package fr.inria.spirals.repairnator.process.step.repair.sequencer.detection;

import fr.inria.spirals.repairnator.process.step.repair.sequencer.SequencerRepair;
import java.util.List;

public interface DetectionStrategy {
    List<ModificationPoint> detect(SequencerRepair repairStep);
}