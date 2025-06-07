package art.galushko.gitlab.mrconflict.gitlab;

import art.galushko.gitlab.mrconflict.model.MergeRequestInfo;

import java.util.List;
import java.util.Set;

/**
 * Service for fetching merge request data from GitLab API.
 */
public interface MergeRequestService {

    /**
     * Fetches all open merge requests for a project.
     *
     * @param projectId GitLab project ID
     * @return list of merge request information
     */
    List<MergeRequestInfo> getOpenMergeRequests(Long projectId);

    /**
     * Fetches merge requests with specific state for a project.
     *
     * @param projectId GitLab project ID
     * @param state merge request state (opened, closed, merged, all)
     * @return list of merge request information
     */
    List<MergeRequestInfo> getMergeRequests(Long projectId, String state);

    /**
     * Fetches a specific merge request by its IID.
     *
     * @param projectId GitLab project ID
     * @param mergeRequestIid merge request internal ID
     * @return merge request information
     */
    MergeRequestInfo getMergeRequest(Long projectId, Long mergeRequestIid);

    /**
     * Fetches changed files for a specific merge request.
     *
     * @param projectId GitLab project ID
     * @param mergeRequestIid merge request internal ID
     * @return set of changed file paths
     */
    Set<String> getChangedFiles(Long projectId, Long mergeRequestIid);
}
