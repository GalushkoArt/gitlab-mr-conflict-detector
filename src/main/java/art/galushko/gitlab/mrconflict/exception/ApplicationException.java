package art.galushko.gitlab.mrconflict.exception;

/**
 * Base exception class for the application.
 * All application-specific exceptions should extend this class.
 */
public class ApplicationException extends RuntimeException {
    
    /**
     * Creates a new ApplicationException with the specified error code.
     *
     * @param errorCode the error code
     */
    public ApplicationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
    }
    
    /**
     * Creates a new ApplicationException with the specified error code and message.
     *
     * @param message the error message
     */
    public ApplicationException(String message) {
        super(message);
    }
    
    /**
     * Creates a new ApplicationException with the specified error code, message, and cause.
     *
     * @param message the error message
     * @param cause the cause of the exception
     */
    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Creates a new ApplicationException with the specified error code and cause.
     *
     * @param errorCode the error code
     * @param cause the cause of the exception
     */
    public ApplicationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
    }
}