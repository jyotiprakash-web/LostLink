package exception;

/**
 * Thrown when an item contains invalid data, fails field validation, or violates item rules.
 */
public class InvalidItemException extends Exception {
    private static final long serialVersionUID = 1L;
    public InvalidItemException(String message) {
        super(message);
    }

    public InvalidItemException(String message, Throwable cause) {
        super(message, cause);
    }
}
