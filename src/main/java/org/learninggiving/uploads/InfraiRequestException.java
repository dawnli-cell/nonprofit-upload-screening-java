package org.learninggiving.uploads;

public final class InfraiRequestException extends RuntimeException {
    private final String code;
    private final int status;

    public InfraiRequestException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public int status() {
        return status;
    }
}
