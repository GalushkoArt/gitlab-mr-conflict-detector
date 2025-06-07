package art.galushko.gitlab.mrconflict.gitlab;

import org.gitlab4j.api.models.Diff;
import org.gitlab4j.api.models.MergeRequest;

import java.util.List;
import java.util.Set;

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
     */
    List<MergeRequest> getMergeRequests(Long projectId, String state);

    /**
     * Gets a specific merge request.
     *
     * @param projectId GitLab project ID
     * @param mergeRequestIid merge request internal ID
     * @return merge request information
     */
    MergeRequest getMergeRequest(Long projectId, Long mergeRequestIid);

    /**
     * Updates the merge request status based on conflict detection results.
     *
     * @param projectId GitLab project ID
     * @param mergeRequestIid merge request internal ID
     */
    void updateMergeRequestStatus(Long projectId, Long mergeRequestIid, Set<String> labels);

    /**
     * Gets the changes for a merge request.
     *
     * @param projectId GitLab project ID
     * @param mergeRequestIid merge request internal ID
     * @return list of changes (diffs)
     */
    List<Diff> getMergeRequestChanges(Long projectId, Long mergeRequestIid);

    /**
     * Creates a note (comment) on a merge request.
     *
     * @param projectId GitLab project ID
     * @param mergeRequestIid merge request internal ID
     * @param noteContent content of the note
     */
    void createMergeRequestNote(Long projectId, Long mergeRequestIid, String noteContent);
}
