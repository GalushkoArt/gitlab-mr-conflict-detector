package art.galushko.gitlab.mrconflict.exception;

/**
 * Exception thrown when there is a configuration error.
 */
public class ConfigurationException extends ApplicationException {
    
    /**
     * Creates a new ConfigurationException with the default error code.
     *
     * @param message the error message
     */
    public ConfigurationException(String message) {
        super(ErrorCode.CONFIGURATION_ERROR, message);
    }
    
    /**
     * Creates a new ConfigurationException with the default error code and cause.
     *
     * @param message the error message
     * @param cause the cause of the exception
     */
    public ConfigurationException(String message, Throwable cause) {
        super(ErrorCode.CONFIGURATION_ERROR, message, cause);
    }
    
    /**
     * Creates a new ConfigurationException with the specified error code and message.
     *
     * @param errorCode the error code
     * @param message the error message
     */
    public ConfigurationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    /**
     * Creates a new ConfigurationException with the specified error code, message, and cause.
     *
     * @param errorCode the error code
     * @param message the error message
     * @param cause the cause of the exception
     */
    public ConfigurationException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}