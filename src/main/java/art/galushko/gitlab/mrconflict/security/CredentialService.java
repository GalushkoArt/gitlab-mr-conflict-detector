package art.galushko.gitlab.mrconflict.security;

import lombok.extern.slf4j.Slf4j;
import java.util.regex.Pattern;

/**
 * Service for securely handling credentials.
 * Provides methods for reading credentials from environment variables,
 * masking sensitive information in logs, and validating tokens.
 */
@Slf4j
public class CredentialService {

    private static final String ENV_GITLAB_TOKEN = "GITLAB_TOKEN";
    private static final String ENV_GITLAB_URL = "GITLAB_URL";
    private static final String MASKED_TOKEN = "********";
    
    // Pattern to validate GitLab token format (alphanumeric, typically 20+ chars)
    private static final Pattern TOKEN_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{20,}$");

    /**
     * Gets the GitLab token from environment variable or the provided token.
     * Prioritizes the environment variable if available.
     *
     * @param providedToken token provided directly (e.g., from command line)
     * @return the GitLab token to use
     */
    public String getGitLabToken(String providedToken) {
        String envToken = System.getenv(ENV_GITLAB_TOKEN);
        if (envToken != null && !envToken.trim().isEmpty()) {
            log.debug("Using GitLab token from environment variable");
            return envToken;
        }
        
        if (providedToken == null || providedToken.trim().isEmpty()) {
            log.warn("No GitLab token provided and {} environment variable not set", ENV_GITLAB_TOKEN);
        }
        
        return providedToken;
    }

    /**
     * Gets the GitLab URL from environment variable or the provided URL.
     * Prioritizes the environment variable if available.
     *
     * @param providedUrl URL provided directly (e.g., from command line)
     * @return the GitLab URL to use
     */
    public String getGitLabUrl(String providedUrl) {
        String envUrl = System.getenv(ENV_GITLAB_URL);
        if (envUrl != null && !envUrl.trim().isEmpty()) {
            log.debug("Using GitLab URL from environment variable");
            return envUrl;
        }
        
        return providedUrl;
    }

    /**
     * Masks a GitLab token for secure logging.
     *
     * @param token the token to mask
     * @return masked token
     */
    public String maskToken(String token) {
        if (token == null || token.isEmpty()) {
            return "";
        }
        return MASKED_TOKEN;
    }

    /**
     * Validates the GitLab token format.
     *
     * @param token the token to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean isValidToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        return TOKEN_PATTERN.matcher(token).matches();
    }

    /**
     * Sanitizes error messages to remove sensitive information.
     *
     * @param message the original error message
     * @param token the token to mask in the message
     * @return sanitized message
     */
    public String sanitizeErrorMessage(String message, String token) {
        if (message == null || token == null || token.isEmpty()) {
            return message;
        }
        
        // Replace token with masked version if present in the message
        return message.replace(token, MASKED_TOKEN);
    }
}