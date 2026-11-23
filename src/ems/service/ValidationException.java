package ems.service;

/** Thrown when user input or a business rule is violated. The message is safe to show to the user. */
public class ValidationException extends Exception {
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
