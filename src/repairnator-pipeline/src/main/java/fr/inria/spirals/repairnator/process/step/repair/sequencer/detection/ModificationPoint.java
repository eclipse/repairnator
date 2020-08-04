package fr.inria.spirals.repairnator.process.step.repair.sequencer.detection;

import fr.inria.astor.core.entities.SuspiciousModificationPoint;

import java.nio.file.Path;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModificationPoint that = (ModificationPoint) o;
        return suspiciousLine == that.suspiciousLine &&
                filePath.equals(that.filePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePath, suspiciousLine);
    }
}
