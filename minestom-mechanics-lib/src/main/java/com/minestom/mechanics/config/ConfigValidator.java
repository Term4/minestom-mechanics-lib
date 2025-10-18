package com.minestom.mechanics.config;

// TODO: I see the vision, but this is not used whatsoever, and I feel like it's being overly verbose

/**
 * Common validation utilities for configuration classes.
 * Consolidates repeated validation patterns across the codebase.
 */
public final class ConfigValidator {
    
    private ConfigValidator() {
        // Utility class
    }
    
    /**
     * Validates that a value is positive.
     * 
     * @param value the value to validate
     * @param fieldName the name of the field for error messages
     * @throws IllegalArgumentException if value is not positive
     */
    public static void requirePositive(double value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive: " + value);
        }
    }
    
    /**
     * Validates that a value is within a range.
     * 
     * @param value the value to validate
     * @param min the minimum allowed value (inclusive)
     * @param max the maximum allowed value (inclusive)
     * @param fieldName the name of the field for error messages
     * @throws IllegalArgumentException if value is outside the range
     */
    public static void requireInRange(double value, double min, double max, String fieldName) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                String.format("%s must be between %.1f and %.1f: %.1f", fieldName, min, max, value)
            );
        }
    }
    
    /**
     * Validates that a value is not null.
     * 
     * @param value the value to validate
     * @param fieldName the name of the field for error messages
     * @throws IllegalArgumentException if value is null
     */
    public static void requireNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
    }
    
    /**
     * Validates that a string is not null or empty.
     * 
     * @param value the string to validate
     * @param fieldName the name of the field for error messages
     * @throws IllegalArgumentException if value is null or empty
     */
    public static void requireNonEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }
    
    /**
     * Validates that a value is at least a minimum.
     * 
     * @param value the value to validate
     * @param min the minimum allowed value (inclusive)
     * @param fieldName the name of the field for error messages
     * @throws IllegalArgumentException if value is less than minimum
     */
    public static void requireAtLeast(double value, double min, String fieldName) {
        if (value < min) {
            throw new IllegalArgumentException(
                String.format("%s must be at least %.1f: %.1f", fieldName, min, value)
            );
        }
    }
    
    /**
     * Validates that a value is at most a maximum.
     * 
     * @param value the value to validate
     * @param max the maximum allowed value (inclusive)
     * @param fieldName the name of the field for error messages
     * @throws IllegalArgumentException if value is greater than maximum
     */
    public static void requireAtMost(double value, double max, String fieldName) {
        if (value > max) {
            throw new IllegalArgumentException(
                String.format("%s must be at most %.1f: %.1f", fieldName, max, value)
            );
        }
    }
    
    /**
     * Validates that one value is less than or equal to another.
     * 
     * @param first the first value
     * @param second the second value
     * @param firstName the name of the first field
     * @param secondName the name of the second field
     * @throws IllegalArgumentException if first > second
     */
    public static void requireLessThanOrEqual(double first, double second, String firstName, String secondName) {
        if (first > second) {
            throw new IllegalArgumentException(
                String.format("%s cannot exceed %s: %.1f > %.1f", firstName, secondName, first, second)
            );
        }
    }
    
    /**
     * Validates that an integer is positive.
     * 
     * @param value the value to validate
     * @param fieldName the name of the field for error messages
     * @throws IllegalArgumentException if value is not positive
     */
    public static void requirePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive: " + value);
        }
    }
    
    /**
     * Validates that an integer is at least a minimum.
     * 
     * @param value the value to validate
     * @param min the minimum allowed value (inclusive)
     * @param fieldName the name of the field for error messages
     * @throws IllegalArgumentException if value is less than minimum
     */
    public static void requireAtLeast(int value, int min, String fieldName) {
        if (value < min) {
            throw new IllegalArgumentException(
                String.format("%s must be at least %d: %d", fieldName, min, value)
            );
        }
    }
}

