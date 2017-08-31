package model;

import java.util.ArrayList;
import java.util.List;

public class Project implements Comparable<Project> {

    private String name;
    private List<Bug> bugs;

    Project(String name) {
        this.name = name;
        this.bugs = new ArrayList<>();
    }

    public String getName() {
        return this.name;
    }

    public void addBug(Bug bug) {
        this.bugs.add(bug);
    }

    public List<Bug> getBugs() {
        return this.bugs;
    }

    public int getNumberOfBugs() {
        return this.bugs.size();
    }

    public boolean containsExceptionType(String exceptionTypeDescription) {
        for (Bug bug : this.bugs) {
            if (bug.containsExceptionType(exceptionTypeDescription)) {
                return true;
            }
        }
        return false;
    }

    public int getOccurrenceCounterOfExceptionType(String exceptionTypeDescription) {
        int occurrenceCounter = 0;
        for (Bug bug : this.bugs) {
            occurrenceCounter += bug.getOccurrenceCounterOfExceptionType(exceptionTypeDescription);
        }
        return occurrenceCounter;
    }

    public int getOccurrenceCounterOfAllExceptionTypes() {
        int occurrenceCounter = 0;
        for (Bug bug : this.bugs) {
            occurrenceCounter += bug.getOccurrenceCounterOfAllExceptionTypes();
        }
        return occurrenceCounter;
    }

    @Override
    public int compareTo(Project project) {
        return this.name.compareTo(project.getName());
    }
}
