package art.galushko.gitlab.mrconflict.gitlab;

import art.galushko.gitlab.mrconflict.model.MergeRequestInfo;
import org.gitlab4j.api.models.MergeRequest;

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
     * @throws GitLabException if merge requests cannot be fetched
     */
    List<MergeRequestInfo> getOpenMergeRequests(Long projectId) throws GitLabException;

    /**
     * Fetches merge requests with specific state for a project.
     *
     * @param projectId GitLab project ID
     * @param state merge request state (opened, closed, merged, all)
     * @return list of merge request information
     * @throws GitLabException if merge requests cannot be fetched
     */
    List<MergeRequestInfo> getMergeRequests(Long projectId, String state) throws GitLabException;

    /**
     * Fetches a specific merge request by its IID.
     *
     * @param projectId GitLab project ID
     * @param mergeRequestIid merge request internal ID
     * @return merge request information
     * @throws GitLabException if merge request cannot be fetched
     */
    MergeRequestInfo getMergeRequest(Long projectId, Long mergeRequestIid) throws GitLabException;

    /**
     * Fetches changed files for a specific merge request.
     *
     * @param projectId GitLab project ID
     * @param mergeRequestIid merge request internal ID
     * @return set of changed file paths
     * @throws GitLabException if changed files cannot be fetched
     */
    Set<String> getChangedFiles(Long projectId, Long mergeRequestIid) throws GitLabException;

    /**
     * Converts GitLab4J MergeRequest to our MergeRequestInfo model.
     *
     * @param mergeRequest GitLab4J merge request
     * @param changedFiles set of changed files
     * @return converted merge request info
     */
    MergeRequestInfo convertToMergeRequestInfo(MergeRequest mergeRequest, Set<String> changedFiles);
}
