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
     */
    List<Branch> getBranches(Long projectId);
    
    /**
     * Gets protected branches for a project.
     *
     * @param projectId GitLab project ID
     * @return list of protected branch names
     */
    List<String> getProtectedBranches(Long projectId);
    
    /**
     * Gets specific branch information.
     *
     * @param projectId GitLab project ID
     * @param branchName name of the branch
     * @return branch information if found
     */
    Optional<Branch> getBranch(Long projectId, String branchName);
    
    /**
     * Checks if a branch is protected.
     *
     * @param projectId GitLab project ID
     * @param branchName name of the branch to check
     * @return true if the branch is protected
     */
    boolean isBranchProtected(Long projectId, String branchName);
}