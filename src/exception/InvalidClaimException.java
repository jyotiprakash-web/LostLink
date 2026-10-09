package exception;

/**
 * Thrown when a claim fails validation or an illegal claim transition is attempted.
 */
public class InvalidClaimException extends Exception {
    private static final long serialVersionUID = 1L;
    public InvalidClaimException(String message) {
        super(message);
    }

    public InvalidClaimException(String message, Throwable cause) {
        super(message, cause);
    }
}
