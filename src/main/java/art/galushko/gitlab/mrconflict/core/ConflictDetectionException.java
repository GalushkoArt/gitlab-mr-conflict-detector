package art.galushko.gitlab.mrconflict.core;

/**
 * Exception thrown when conflict detection operations fail.
 */
public class ConflictDetectionException extends Exception {
    
    public ConflictDetectionException(String message) {
        super(message);
    }
    
    public ConflictDetectionException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ConflictDetectionException(Throwable cause) {
        super(cause);
    }
}

