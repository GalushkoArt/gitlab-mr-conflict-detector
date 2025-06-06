package art.galushko.gitlab.mrconflict.gitlab;

import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Project;

import java.util.List;
import java.util.Optional;

/**
 * Interface for GitLab API operations.
 */
public interface GitLabClient {

    /**
     * Authenticates with GitLab using the provided token.
     *
     * @param gitlabUrl   GitLab instance URL
     * @param accessToken personal access token
     * @return
     * @throws GitLabException if authentication fails
     */
    GitLab4JClient authenticate(String gitlabUrl, String accessToken) throws GitLabException;

    /**
     * Gets a project by its ID.
     *
     * @param projectId GitLab project ID
     * @return project information
     * @throws GitLabException if project cannot be retrieved
     */
    Project getProject(Long projectId) throws GitLabException;

    /**
     * Gets a project by its path (namespace/project-name).
     *
     * @param projectPath project path
     * @return project information
     * @throws GitLabException if project cannot be retrieved
     */
    Project getProject(String projectPath) throws GitLabException;

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
     * Gets merge requests for a project.
     *
     * @param projectId GitLab project ID
     * @param state merge request state filter (opened, closed, merged, all)
     * @return list of merge requests
     * @throws GitLabException if merge requests cannot be retrieved
     */
    List<MergeRequest> getMergeRequests(Long projectId, String state) throws GitLabException;

    /**
     * Gets a specific merge request.
     *
     * @param projectId GitLab project ID
     * @param mergeRequestIid merge request internal ID
     * @return merge request information
     * @throws GitLabException if merge request cannot be retrieved
     */
    MergeRequest getMergeRequest(Long projectId, Long mergeRequestIid) throws GitLabException;

    /**
     * Updates the merge request status based on conflict detection results.
     *
     * @param projectId GitLab project ID
     * @param mergeRequestIid merge request internal ID
     * @param hasConflicts whether conflicts were detected
     * @throws GitLabException if status cannot be updated
     */
    void updateMergeRequestStatus(Long projectId, Long mergeRequestIid, boolean hasConflicts) 
            throws GitLabException;

    /**
     * Checks if the current user has permission to access the project.
     *
     * @param projectId GitLab project ID
     * @return true if user has access
     * @throws GitLabException if permission check fails
     */
    boolean hasProjectAccess(Long projectId) throws GitLabException;

    /**
     * Gets the default branch for a project.
     *
     * @param projectId GitLab project ID
     * @return default branch name
     * @throws GitLabException if default branch cannot be determined
     */
    String getDefaultBranch(Long projectId) throws GitLabException;

    /**
     * Checks if a branch is protected.
     *
     * @param projectId GitLab project ID
     * @param branchName name of the branch to check
     * @return true if branch is protected
     * @throws GitLabException if protection status cannot be determined
     */
    boolean isBranchProtected(Long projectId, String branchName) throws GitLabException;

    /**
     * Gets the changes for a merge request.
     *
     * @param projectId GitLab project ID
     * @param mergeRequestIid merge request internal ID
     * @return list of changes (diffs)
     * @throws GitLabException if changes cannot be retrieved
     */
    List<org.gitlab4j.api.models.Diff> getMergeRequestChanges(Long projectId, Long mergeRequestIid) throws GitLabException;

    /**
     * Creates a note (comment) on a merge request.
     *
     * @param projectId GitLab project ID
     * @param mergeRequestIid merge request internal ID
     * @param noteContent content of the note
     * @throws GitLabException if note cannot be created
     */
    void createMergeRequestNote(Long projectId, Long mergeRequestIid, String noteContent) throws GitLabException;
}
