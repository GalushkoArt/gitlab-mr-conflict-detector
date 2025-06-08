package art.galushko.gitlab.mrconflict.security;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/**
 * Validator for user inputs to prevent security issues.
 * Provides methods for validating and sanitizing various types of inputs.
 */
@Slf4j
public class InputValidator {

    // Pattern for validating project IDs (numeric only)
    private static final Pattern PROJECT_ID_PATTERN = Pattern.compile("^\\d+$");

    /**
     * Validates a GitLab URL.
     *
     * @param url the URL to validate
     * @return true if the URL is valid, false otherwise
     */
    public boolean isValidGitLabUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        try {
            URI uri = new URI(url);
            return uri.getScheme() != null && (uri.getScheme().equals("http") || uri.getScheme().equals("https"));
        } catch (URISyntaxException e) {
            log.error("Invalid GitLab URL format: {}", url);
            return false;
        }
    }

    /**
     * Validates a project ID.
     *
     * @param projectId the project ID to validate
     * @return true if the project ID is valid, false otherwise
     */
    public boolean isValidProjectId(String projectId) {
        if (projectId == null || projectId.trim().isEmpty()) {
            return false;
        }
        
        return PROJECT_ID_PATTERN.matcher(projectId).matches();
    }
}