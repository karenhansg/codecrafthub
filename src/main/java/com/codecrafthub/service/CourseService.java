package com.codecrafthub.service;

import com.codecrafthub.exception.CourseNotFoundException;
import com.codecrafthub.exception.FileStorageException;
import com.codecrafthub.exception.InvalidCourseException;
import com.codecrafthub.model.Course;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Service layer that reads and writes courses to courses.json using Jackson.
 * All file access goes through this class so controllers stay thin.
 */
@Service
public class CourseService {

    /** Allowed status values exactly as stored in JSON. */
    private static final Set<String> VALID_STATUSES = Set.of(
            "Not Started",
            "In Progress",
            "Completed"
    );

    /** JSON file stored in the project working directory. */
    private static final Path COURSES_FILE = Paths.get("courses.json");

    /** Jackson mapper configured for Java 8 date/time types. */
    private final ObjectMapper objectMapper;

    public CourseService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Runs once at startup: creates courses.json with an empty array if missing.
     */
    @PostConstruct
    public void initializeStorageFile() {
        try {
            if (!Files.exists(COURSES_FILE)) {
                objectMapper.writeValue(COURSES_FILE.toFile(), new ArrayList<Course>());
            }
        } catch (IOException ex) {
            throw new FileStorageException("Failed to create courses.json", ex);
        }
    }

    /** Returns every course from the JSON file. */
    public List<Course> getAllCourses() {
        return readCoursesFromFile();
    }

    /** Returns one course by id, or throws if it does not exist. */
    public Course getCourseById(Long id) {
        return readCoursesFromFile().stream()
                .filter(course -> course.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new CourseNotFoundException(id));
    }

    /**
     * Adds a new course with auto-generated id and created_at timestamp.
     * Client-supplied id and created_at values are ignored.
     */
    public Course createCourse(Course course) {
        validateCourse(course);

        List<Course> courses = readCoursesFromFile();
        course.setId(generateNextId(courses));
        course.setCreatedAt(LocalDateTime.now());

        courses.add(course);
        writeCoursesToFile(courses);
        return course;
    }

    /**
     * Updates an existing course while preserving its original created_at value.
     */
    public Course updateCourse(Long id, Course updatedCourse) {
        validateCourse(updatedCourse);

        List<Course> courses = readCoursesFromFile();
        for (int i = 0; i < courses.size(); i++) {
            Course existing = courses.get(i);
            if (existing.getId().equals(id)) {
                updatedCourse.setId(id);
                updatedCourse.setCreatedAt(existing.getCreatedAt());
                courses.set(i, updatedCourse);
                writeCoursesToFile(courses);
                return updatedCourse;
            }
        }

        throw new CourseNotFoundException(id);
    }

    /** Removes a course by id. */
    public void deleteCourse(Long id) {
        List<Course> courses = readCoursesFromFile();
        boolean removed = courses.removeIf(course -> course.getId().equals(id));

        if (!removed) {
            throw new CourseNotFoundException(id);
        }

        writeCoursesToFile(courses);
    }

    /**
     * Validates required fields and allowed status values.
     * Throws InvalidCourseException with a clear message for API clients.
     */
    private void validateCourse(Course course) {
        if (course.getName() == null || course.getName().isBlank()) {
            throw new InvalidCourseException("Field 'name' is required and cannot be blank.");
        }
        if (course.getDescription() == null || course.getDescription().isBlank()) {
            throw new InvalidCourseException("Field 'description' is required and cannot be blank.");
        }
        if (course.getTargetDate() == null) {
            throw new InvalidCourseException("Field 'target_date' is required (format: YYYY-MM-DD).");
        }
        if (course.getStatus() == null || course.getStatus().isBlank()) {
            throw new InvalidCourseException("Field 'status' is required.");
        }
        if (!VALID_STATUSES.contains(course.getStatus())) {
            throw new InvalidCourseException(
                    "Invalid status '" + course.getStatus() + "'. "
                            + "Must be one of: \"Not Started\", \"In Progress\", \"Completed\".");
        }
    }

    /** Next id is one greater than the current maximum (starts at 1 for an empty file). */
    private Long generateNextId(List<Course> courses) {
        return courses.stream()
                .mapToLong(Course::getId)
                .max()
                .orElse(0L) + 1L;
    }

    /** Reads the full course list from courses.json. */
    private List<Course> readCoursesFromFile() {
        try {
            if (!Files.exists(COURSES_FILE)) {
                initializeStorageFile();
            }
            List<Course> courses = objectMapper.readValue(
                    COURSES_FILE.toFile(),
                    new TypeReference<List<Course>>() {}
            );
            return new ArrayList<>(courses);
        } catch (IOException ex) {
            throw new FileStorageException("Failed to read courses from courses.json", ex);
        }
    }

    /** Writes the full course list back to courses.json (pretty-printed for readability). */
    private void writeCoursesToFile(List<Course> courses) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(COURSES_FILE.toFile(), courses);
        } catch (IOException ex) {
            throw new FileStorageException("Failed to write courses to courses.json", ex);
        }
    }
}
