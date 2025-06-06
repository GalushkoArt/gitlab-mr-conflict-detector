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
     * @return the authenticated client
     * @throws GitLabException if authentication fails
     */
    GitLabClient authenticate(String gitlabUrl, String accessToken) throws GitLabException;
    
    /**
     * Checks if the current user has permission to access the project.
     *
     * @param projectId GitLab project ID
     * @return true if user has access
     * @throws GitLabException if permission check fails
     */
    boolean hasProjectAccess(Long projectId) throws GitLabException;
}