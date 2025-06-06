package art.galushko.gitlab.mrconflict.gitlab;

import org.gitlab4j.api.models.Branch;

import java.util.List;
import java.util.Optional;

/**
 * Interface for GitLab branch operations.
 * This interface follows the Interface Segregation Principle by focusing only on branch-related operations.
 */
public interface GitLabBranchClient {
    
    /**
     * Gets all branches for a project.
     *
     * @param projectId GitLab project ID
     * @return list of branches
     * @throws GitLabException if branches cannot be retrieved
     */
    List<Branch> getBranches(Long projectId) throws GitLabException;
    
    /**
     * Gets protected branches for a project.
     *
     * @param projectId GitLab project ID
     * @return list of protected branch names
     * @throws GitLabException if protected branches cannot be retrieved
     */
    List<String> getProtectedBranches(Long projectId) throws GitLabException;
    
    /**
     * Gets a specific branch information.
     *
     * @param projectId GitLab project ID
     * @param branchName name of the branch
     * @return branch information if found
     * @throws GitLabException if branch cannot be retrieved
     */
    Optional<Branch> getBranch(Long projectId, String branchName) throws GitLabException;
    
    /**
     * Checks if a branch is protected.
     *
     * @param projectId GitLab project ID
     * @param branchName name of the branch to check
     * @return true if branch is protected
     * @throws GitLabException if protection status cannot be determined
     */
    boolean isBranchProtected(Long projectId, String branchName) throws GitLabException;
}