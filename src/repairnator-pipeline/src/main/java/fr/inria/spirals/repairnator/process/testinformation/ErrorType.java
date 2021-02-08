package fr.inria.spirals.repairnator.process.testinformation;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ErrorType extends BugType {

    /**
     * Source class files where the error occurred.
     */
    private Set<File> classFiles = new HashSet<>();

    /**
     * Source package files (directories) that contain classes where the error occurred or classes that are
     * present in the stack trace of the error.
     */
    private Set<File> packageFiles = new HashSet<>();

    /**
     * Source class files that are present in the error stack trace.
     */
    private Set<File> stackFiles = new HashSet<>();

    /**
     * Class representing a type of error that occurred during a test execution
     *
     * @param name   The name of the type of error
     * @param detail The detailed message generated for this error
     */
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
