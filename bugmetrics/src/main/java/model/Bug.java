package model;

import java.util.HashMap;
import java.util.Map;

public class Bug {

    private String branchName;
    private Project project;
    private Map<ExceptionType, Integer> exceptionTypeToOccurrenceCounterMap;
    private Map<String, Integer> stepToDurationMap;

    Bug(String branchName, Project project) {
        this.branchName = branchName;
        this.project = project;
        this.exceptionTypeToOccurrenceCounterMap = new HashMap<>();
        this.stepToDurationMap = new HashMap<>();
    }

    public String getBranchName() {
        return this.branchName;
    }

    public Project getProject() {
        return this.project;
    }

    public void addExceptionType(ExceptionType exceptionType) {
        if (!this.exceptionTypeToOccurrenceCounterMap.keySet().contains(exceptionType)) {
            this.exceptionTypeToOccurrenceCounterMap.put(exceptionType, 0);
        }
        this.exceptionTypeToOccurrenceCounterMap.put(exceptionType, this.exceptionTypeToOccurrenceCounterMap.get(exceptionType) + 1);
    }

    public boolean containsExceptionType(String exceptionTypeDescription) {
        for (ExceptionType exceptionType : this.exceptionTypeToOccurrenceCounterMap.keySet()) {
            if (exceptionType.getDescription().equals(exceptionTypeDescription)) {
                return true;
            }
        }
        return false;
    }

    public int getOccurrenceCounterOfExceptionType(String exceptionTypeDescription) {
        for (ExceptionType exceptionType : this.exceptionTypeToOccurrenceCounterMap.keySet()) {
            if (exceptionType.getDescription().equals(exceptionTypeDescription)) {
                return this.exceptionTypeToOccurrenceCounterMap.get(exceptionType);
            }
        }
        return 0;
    }

    public int getOccurrenceCounterOfAllExceptionTypes() {
        int occurrenceCounter = 0;
        for (Map.Entry<ExceptionType, Integer> exceptionTypeToOccurrenceCounter : this.exceptionTypeToOccurrenceCounterMap.entrySet()) {
            occurrenceCounter += exceptionTypeToOccurrenceCounter.getValue();
        }
        return occurrenceCounter;
    }

    public void addStepToDuration(String stepName, int duration) {
        this.stepToDurationMap.put(stepName, duration);
    }

    public int getStepDuration(String stepName) {
        return this.stepToDurationMap.get(stepName);
    }
}
