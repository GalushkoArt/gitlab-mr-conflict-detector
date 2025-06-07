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
    
    // Pattern for validating merge request IIDs (numeric only)
    private static final Pattern MR_IID_PATTERN = Pattern.compile("^\\d+$");
    
    // Pattern for validating branch names (alphanumeric, dash, underscore, forward slash)
    private static final Pattern BRANCH_NAME_PATTERN = Pattern.compile("^[\\w\\-./]+$");
    
    // Characters that need to be escaped in GitLab API parameters
    private static final String[] CHARACTERS_TO_ESCAPE = {"&", "?", "#", "%"};

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

    /**
     * Validates a merge request IID.
     *
     * @param mrIid the merge request IID to validate
     * @return true if the merge request IID is valid, false otherwise
     */
    public boolean isValidMergeRequestIid(String mrIid) {
        if (mrIid == null || mrIid.trim().isEmpty()) {
            return false;
        }
        
        return MR_IID_PATTERN.matcher(mrIid).matches();
    }

    /**
     * Validates a branch name.
     *
     * @param branchName the branch name to validate
     * @return true if the branch name is valid, false otherwise
     */
    public boolean isValidBranchName(String branchName) {
        if (branchName == null || branchName.trim().isEmpty()) {
            return false;
        }
        
        return BRANCH_NAME_PATTERN.matcher(branchName).matches();
    }

    /**
     * Escapes special characters in a string for use in GitLab API parameters.
     *
     * @param input the input string to escape
     * @return the escaped string
     */
    public String escapeForGitLabApi(String input) {
        if (input == null) {
            return null;
        }
        
        String result = input;
        for (String character : CHARACTERS_TO_ESCAPE) {
            result = result.replace(character, "%" + Integer.toHexString(character.charAt(0)));
        }
        
        return result;
    }
}