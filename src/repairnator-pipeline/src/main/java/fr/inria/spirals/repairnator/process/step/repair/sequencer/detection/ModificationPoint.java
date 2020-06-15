package fr.inria.spirals.repairnator.process.step.repair.sequencer.detection;

import fr.inria.astor.core.entities.SuspiciousModificationPoint;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ModificationPoint {
    private Path filePath;
    private int suspiciousLine;

    public static ModificationPoint CreateFrom(SuspiciousModificationPoint p) {
        return new ModificationPoint(
                p.getCodeElement().getPosition().getFile().toPath(),
                p.getSuspicious().getLineNumber()
        );
    }

//    log based detection placeholder
//    public static ModificationPoint CreateFrom(LogBasedModificationPoint p) {
//        return new ModificationPoint(p);
//    }

    public ModificationPoint(Path filepath, int suspiciousLine){
        this.filePath = filepath;
        this.suspiciousLine = suspiciousLine;
    }

    public int getSuspiciousLine() {
        return suspiciousLine;
    }

    public Path getFilePath() {
        return filePath;
    }
}
