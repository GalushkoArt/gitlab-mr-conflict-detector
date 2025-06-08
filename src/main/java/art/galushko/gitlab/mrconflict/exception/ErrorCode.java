package art.galushko.gitlab.mrconflict.exception;

import lombok.Getter;

/**
 * Enum of error codes for the application.
 */
@Getter
public enum ErrorCode {
    // GitLab API errors
    GITLAB_API_ERROR("GitLab API error"),
    ;

    private final String message;
    
    /**
     * Creates a new ErrorCode.
     *
     * @param message the error message
     */
    ErrorCode(String message) {
        this.message = message;
    }
}