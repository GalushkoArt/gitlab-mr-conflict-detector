package art.galushko.gitlab.mrconflict.git;

/**
 * Exception thrown when Git operations fail.
 */
public class GitOperationException extends Exception {
    
    public GitOperationException(String message) {
        super(message);
    }
    
    public GitOperationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public GitOperationException(Throwable cause) {
        super(cause);
    }
}

