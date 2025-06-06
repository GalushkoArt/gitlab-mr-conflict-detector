package art.galushko.gitlab.mrconflict.gitlab;

import org.gitlab4j.api.models.Diff;
import org.gitlab4j.api.models.MergeRequest;

import java.util.List;

/**
 * Interface for GitLab merge request operations.
 * This interface follows the Interface Segregation Principle by focusing only on merge request-related operations.
 */
public interface GitLabMergeRequestClient {
    
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
     * Gets the changes for a merge request.
     *
     * @param projectId GitLab project ID
     * @param mergeRequestIid merge request internal ID
     * @return list of changes (diffs)
     * @throws GitLabException if changes cannot be retrieved
     */
    List<Diff> getMergeRequestChanges(Long projectId, Long mergeRequestIid) throws GitLabException;
    
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