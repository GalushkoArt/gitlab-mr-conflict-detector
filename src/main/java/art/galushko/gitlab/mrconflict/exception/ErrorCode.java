package art.galushko.gitlab.mrconflict.exception;

import lombok.Getter;

/**
 * Enum of error codes for the application.
 */
@Getter
public enum ErrorCode {
    // Success
    SUCCESS(0, "Operation completed successfully"),
    
    // General errors
    UNKNOWN_ERROR(1, "An unknown error occurred"),
    CONFIGURATION_ERROR(2, "Configuration error"),
    
    // GitLab API errors
    GITLAB_API_ERROR(10, "GitLab API error"),
    GITLAB_AUTHENTICATION_ERROR(11, "GitLab authentication error"),
    GITLAB_ACCESS_ERROR(12, "GitLab access error"),
    GITLAB_RESOURCE_NOT_FOUND(13, "GitLab resource not found"),
    
    // Conflict detection errors
    CONFLICT_DETECTION_ERROR(20, "Error during conflict detection"),
    CONFLICTS_DETECTED(21, "Conflicts detected between merge requests"),
    
    // CLI errors
    CLI_ARGUMENT_ERROR(30, "Invalid command line arguments"),
    
    // File system errors
    FILE_SYSTEM_ERROR(40, "File system error");

    private final int exitCode;
    private final String message;
    
    /**
     * Creates a new ErrorCode.
     *
     * @param exitCode the exit code
     * @param message the error message
     */
    ErrorCode(int exitCode, String message) {
        this.exitCode = exitCode;
        this.message = message;
    }

    /**
     * Gets a formatted error message with the error code.
     *
     * @return the formatted error message
     */
    public String getFormattedMessage() {
        return String.format("[%d] %s", exitCode, message);
    }
}