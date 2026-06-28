package com.codecrafthub.exception;

/**
 * Thrown when request data fails validation (missing fields, invalid status, etc.).
 */
public class InvalidCourseException extends RuntimeException {

    public InvalidCourseException(String message) {
        super(message);
    }
}
