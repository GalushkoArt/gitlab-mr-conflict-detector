package art.galushko.gitlab.mrconflict.gitlab;

/**
 * Composite interface for GitLab API operations.
 * This interface follows the Interface Segregation Principle by extending
 * smaller, focused interfaces for different GitLab API operations.
 */
public interface GitLabClient extends GitLabAuthenticationClient, GitLabProjectClient, 
                                      GitLabBranchClient, GitLabMergeRequestClient {
}
