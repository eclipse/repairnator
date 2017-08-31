package model;

public class ExceptionType {

    private String description;
    private boolean isExceptionTypeOut;

    ExceptionType(String description, boolean isExceptionTypeOut) {
        this.description = description;
        this.isExceptionTypeOut = isExceptionTypeOut;
    }

    public String getDescription() {
        return this.description;
    }

    public boolean isExceptionTypeOut() {
        return this.isExceptionTypeOut;
    }
}
