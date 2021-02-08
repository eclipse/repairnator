package fr.inria.spirals.repairnator.process.testinformation;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ErrorType extends BugType {

    private Set<File> classFiles = new HashSet<>();

    private Set<File> packageFiles = new HashSet<>();

    private Set<File> stackFiles = new HashSet<>();

    public ErrorType(String name, String detail) {
        super(name, detail);
    }

    public Set<File> getClassFiles() {
        return classFiles;
    }

    public Set<File> getPackageFiles() {
        return packageFiles;
    }

    public Set<File> getStackFiles() {
        return stackFiles;
    }

    public void addClassFiles(Collection<File> files) {
        classFiles.addAll(files);
    }

    public void addPackageFiles(Collection<File> files) {
        packageFiles.addAll(files);
    }

    public void addStackFiles(Collection<File> files) {
        stackFiles.addAll(files);
    }

}
