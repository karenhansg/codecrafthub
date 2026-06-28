package com.codecrafthub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the CodeCraftHub Spring Boot application.
 * Running this class starts the embedded web server and loads all Spring components.
 */
@SpringBootApplication
public class CodeCraftHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeCraftHubApplication.class, args);
    }
}
