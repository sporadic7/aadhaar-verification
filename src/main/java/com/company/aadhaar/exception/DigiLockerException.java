package com.company.aadhaar.exception;

public class DigiLockerException extends RuntimeException {
    public DigiLockerException(String message) {
        super(message);
    }

    public DigiLockerException(String message, Throwable cause) {
        super(message, cause);
    }
}

