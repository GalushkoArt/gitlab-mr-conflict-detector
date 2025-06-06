package art.galushko.gitlab.mrconflict.gitlab;

import art.galushko.gitlab.mrconflict.exception.ApplicationException;
import art.galushko.gitlab.mrconflict.exception.ErrorCode;

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
        super(ErrorCode.GITLAB_API_ERROR, message);
    }

    /**
     * Creates a new GitLabException with the specified message and cause.
     *
     * @param message the error message
     * @param cause the cause of the exception
     */
    public GitLabException(String message, Throwable cause) {
        super(ErrorCode.GITLAB_API_ERROR, message, cause);
    }

    /**
     * Creates a new GitLabException with the specified cause.
     *
     * @param cause the cause of the exception
     */
    public GitLabException(Throwable cause) {
        super(ErrorCode.GITLAB_API_ERROR, cause);
    }

    /**
     * Creates a new GitLabException with the specified error code and message.
     *
     * @param errorCode the error code
     * @param message the error message
     */
    public GitLabException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Creates a new GitLabException with the specified error code, message, and cause.
     *
     * @param errorCode the error code
     * @param message the error message
     * @param cause the cause of the exception
     */
    public GitLabException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
