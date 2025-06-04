package art.galushko.gitlab.mrconflict.gitlab;

/**
 * Exception thrown when GitLab API operations fail.
 */
public class GitLabException extends Exception {
    
    public GitLabException(String message) {
        super(message);
    }
    
    public GitLabException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public GitLabException(Throwable cause) {
        super(cause);
    }
}

