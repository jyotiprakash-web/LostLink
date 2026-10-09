package exception;

/**
 * Thrown when an item with a specified ID does not exist in the repository.
 */
public class ItemNotFoundException extends Exception {
    private static final long serialVersionUID = 1L;
    public ItemNotFoundException(String message) {
        super(message);
    }

    public ItemNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
