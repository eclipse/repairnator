package fr.inria.spirals.repairnator.process.step.repair.sequencer;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.util.ArrayList;

import fr.inria.astor.core.entities.SuspiciousModificationPoint;

// this class can be replaced by the same-name class in Astor lib once the latter one become public
public class SuspiciousFile {
    private int suspiciousLineNumber;
    private String suspiciousLine;
    private List<String> allLines;
    private String className;
    private String fileName;

    public SuspiciousFile(SuspiciousModificationPoint smp) throws Exception {
        File suspFile = smp.getCodeElement().getPosition().getFile();
        fileName = suspFile.getName();
        className = smp.getSuspicious().getClassName();

        suspiciousLineNumber = smp.getSuspicious().getLineNumber();

        String line;
        int counter = 1;
        BufferedReader br = new BufferedReader(new FileReader(suspFile));
        allLines = new ArrayList<String>();
        while ((line = br.readLine()) != null) {
            if (counter == suspiciousLineNumber) {
                suspiciousLine = line;
            }
            allLines.add(line);
            counter++;
        }
        br.close();
    }

    public int getSuspiciousLineNumber() {
        return suspiciousLineNumber;
    }

    public String getSuspiciousLine() {
        return suspiciousLine;
    }

    public List<String> getAllLines() {
        return allLines;
    }

    public String getClassName() {
        return className;
    }

    public String getFileName() {
        return fileName;
    }
}