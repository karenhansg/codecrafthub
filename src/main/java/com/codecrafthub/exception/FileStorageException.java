package com.codecrafthub.exception;

/**
 * Thrown when reading from or writing to courses.json fails.
 */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
