package art.galushko.gitlab.mrconflict.gitlab;

/**
 * Interface for GitLab authentication operations.
 * This interface follows the Interface Segregation Principle by focusing only on authentication.
 */
public interface GitLabAuthenticationClient {
    
    /**
     * Authenticates with GitLab using the provided token.
     *
     * @param gitlabUrl   GitLab instance URL
     * @param accessToken personal access token
     * @return an authenticated client
     */
    GitLabClient authenticate(String gitlabUrl, String accessToken);
    
    /**
     * Checks if the current user has permission to access the project.
     *
     * @param projectId GitLab project ID
     * @return true if the user has access
     */
    boolean hasProjectAccess(Long projectId);
}