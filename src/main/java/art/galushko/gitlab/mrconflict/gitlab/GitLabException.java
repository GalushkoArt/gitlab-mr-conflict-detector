package art.galushko.gitlab.mrconflict.gitlab;

import art.galushko.gitlab.mrconflict.exception.ApplicationException;

/**
 * Exception thrown when GitLab API operations fail.
 */
public class GitLabException extends ApplicationException {

    /**
     * Creates a new GitLabException with the specified message.
     *
     * @param message the error message
     */
    public GitLabException(String message) {
        super(message);
    }

    /**
     * Creates a new GitLabException with the specified message and cause.
     *
     * @param message the error message
     * @param cause the cause of the exception
     */
    public GitLabException(String message, Throwable cause) {
        super(message, cause);
    }
}
