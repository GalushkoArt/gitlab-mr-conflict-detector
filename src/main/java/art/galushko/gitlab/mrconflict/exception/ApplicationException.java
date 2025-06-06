package art.galushko.gitlab.mrconflict.exception;

/**
 * Base exception class for the application.
 * All application-specific exceptions should extend this class.
 */
public class ApplicationException extends RuntimeException {
    
    private final ErrorCode errorCode;
    
    /**
     * Creates a new ApplicationException with the specified error code.
     *
     * @param errorCode the error code
     */
    public ApplicationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    
    /**
     * Creates a new ApplicationException with the specified error code and message.
     *
     * @param errorCode the error code
     * @param message the error message
     */
    public ApplicationException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * Creates a new ApplicationException with the specified error code, message, and cause.
     *
     * @param errorCode the error code
     * @param message the error message
     * @param cause the cause of the exception
     */
    public ApplicationException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * Creates a new ApplicationException with the specified error code and cause.
     *
     * @param errorCode the error code
     * @param cause the cause of the exception
     */
    public ApplicationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
    
    /**
     * Gets the error code.
     *
     * @return the error code
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    /**
     * Gets the exit code for the application.
     *
     * @return the exit code
     */
    public int getExitCode() {
        return errorCode.getExitCode();
    }
}