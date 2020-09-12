package fr.inria.spirals.repairnator.process.step.repair.sequencer.detection;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;
import fr.inria.spirals.repairnator.process.step.repair.sequencer.SequencerRepair;
import org.slf4j.Logger;

import java.util.List;

public interface DetectionStrategy {
    List<ModificationPoint> detect(SequencerRepair repairStep);
    boolean validate(RepairPatch patch);
    void setup(ProjectInspector inspector, String pom, Logger logger);
}