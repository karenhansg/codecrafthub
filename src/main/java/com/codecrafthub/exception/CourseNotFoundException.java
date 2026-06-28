package com.codecrafthub.exception;

/**
 * Thrown when a course with the requested id does not exist in courses.json.
 */
public class CourseNotFoundException extends RuntimeException {

    public CourseNotFoundException(Long id) {
        super("Course not found with id: " + id);
    }
}
