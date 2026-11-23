package ems.service;

import ems.model.Employee;

import java.math.BigDecimal;

/** Input validation rules (section 8 of the lab). All methods trim their input. */
public final class Validator {
    private Validator() {
    }

    /** Required text field. Returns the trimmed text. */
    public static String requireText(String value, String field) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(field + " is required.");
        }
        return value.trim();
    }

    /** A positive whole number (used for employee and department IDs). */
    public static int parseId(String value, String field) throws ValidationException {
        String text = requireText(value, field);
        int number;
        try {
            number = Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            throw new ValidationException(field + " must be a valid whole number.");
        }
        if (number <= 0) {
            throw new ValidationException(field + " must be a positive number.");
        }
        return number;
    }

    /** A positive whole age. */
    public static int parseAge(String value) throws ValidationException {
        String text = requireText(value, "Age");
        int age;
        try {
            age = Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            throw new ValidationException("Age must be a valid whole number.");
        }
        if (age <= 0) {
            throw new ValidationException("Age must be a positive number.");
        }
        if (age > Employee.MAX_AGE) {
            throw new ValidationException("Age must not be greater than " + Employee.MAX_AGE + ".");
        }
        return age;
    }

    /**
     * A non-negative finite number (salary, bonus, hourly rate, hours).
     * BigDecimal is used for parsing so that text such as "NaN", "Infinity" or "12d" is rejected.
     */
    public static double parseNonNegative(String value, String field) throws ValidationException {
        String text = requireText(value, field);
        BigDecimal number;
        try {
            number = new BigDecimal(text);
        } catch (NumberFormatException ex) {
            throw new ValidationException(field + " must be a valid number.");
        }
        if (number.signum() < 0) {
            throw new ValidationException(field + " must not be negative.");
        }
        double result = number.doubleValue();
        if (Double.isInfinite(result)) {
            throw new ValidationException(field + " is too large.");
        }
        return result;
    }
}
