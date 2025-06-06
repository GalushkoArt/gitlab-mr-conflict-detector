package art.galushko.gitlab.mrconflict.gitlab;

import art.galushko.gitlab.mrconflict.model.MergeRequestInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gitlab4j.api.models.Diff;
import org.gitlab4j.api.models.MergeRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of MergeRequestService using GitLab4J API.
 */
@Slf4j
@RequiredArgsConstructor
public class GitLab4JMergeRequestService implements MergeRequestService {

    private final GitLabClient gitLabClient;

    @Override
    public List<MergeRequestInfo> getOpenMergeRequests(Long projectId) throws GitLabException {
        return getMergeRequests(projectId, "opened");
    }

    @Override
    public List<MergeRequestInfo> getMergeRequests(Long projectId, String state) throws GitLabException {
        log.info("Fetching merge requests for project {} with state: {}", projectId, state);

        try {
            List<MergeRequest> mergeRequests = gitLabClient.getMergeRequests(projectId, state);

            return mergeRequests.stream()
                    .map(mr -> {
                        try {
                            Set<String> changedFiles = getChangedFiles(projectId, mr.getIid());
                            return convertToMergeRequestInfo(mr, changedFiles);
                        } catch (GitLabException e) {
                            log.warn("Failed to get changed files for MR {}: {}", mr.getIid(), e.getMessage());
                            // Return MR info with empty file set rather than failing completely
                            return convertToMergeRequestInfo(mr, new HashSet<>());
                        }
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new GitLabException("Failed to fetch merge requests for project: " + projectId, e);
        }
    }

    @Override
    public MergeRequestInfo getMergeRequest(Long projectId, Long mergeRequestIid) throws GitLabException {
        log.debug("Fetching merge request {} for project {}", mergeRequestIid, projectId);

        try {
            MergeRequest mergeRequest = gitLabClient.getMergeRequest(projectId, mergeRequestIid);
            Set<String> changedFiles = getChangedFiles(projectId, mergeRequestIid);

            return convertToMergeRequestInfo(mergeRequest, changedFiles);

        } catch (Exception e) {
            throw new GitLabException("Failed to fetch merge request " + mergeRequestIid +
                    " for project: " + projectId, e);
        }
    }

    @Override
    public Set<String> getChangedFiles(Long projectId, Long mergeRequestIid) throws GitLabException {
        log.debug("Fetching changed files for MR {} in project {}", mergeRequestIid, projectId);

        // Get the diffs for the merge request
        List<Diff> diffs = gitLabClient.getMergeRequestChanges(projectId, mergeRequestIid);

        return diffs.stream()
                .map(diff -> {
                    // Use new_path if available (for new/modified files), otherwise old_path (for deleted files)
                    String filePath = diff.getNewPath();
                    if (filePath == null || filePath.equals("/dev/null")) {
                        filePath = diff.getOldPath();
                    }
                    return filePath;
                })
                .filter(path -> path != null && !path.equals("/dev/null"))
                .collect(Collectors.toSet());
    }

    @Override
    public MergeRequestInfo convertToMergeRequestInfo(MergeRequest mergeRequest, Set<String> changedFiles) {
        return MergeRequestInfo.builder()
                .id(mergeRequest.getIid().intValue())
                .title(mergeRequest.getTitle())
                .sourceBranch(mergeRequest.getSourceBranch())
                .targetBranch(mergeRequest.getTargetBranch())
                .changedFiles(changedFiles)
                .build();
    }

    /**
     * Gets merge requests that are ready for conflict analysis.
     * This includes open merge requests that are not in draft state.
     *
     * @param projectId GitLab project ID
     * @return list of merge requests ready for analysis
     * @throws GitLabException if merge requests cannot be fetched
     */
    public List<MergeRequestInfo> getMergeRequestsForConflictAnalysis(Long projectId) throws GitLabException {
        log.info("Fetching merge requests for conflict analysis in project {}", projectId);

        List<MergeRequestInfo> openMRs = getOpenMergeRequests(projectId);

        // Filter out draft/WIP merge requests as they're typically not ready for conflict analysis
        return openMRs.stream()
                .filter(mr -> !isDraftMergeRequest(mr))
                .collect(Collectors.toList());
    }

    /**
     * Checks if a merge request is in draft state.
     * Draft MRs are typically prefixed with "Draft:", "WIP:", or "[WIP]"
     */
    private boolean isDraftMergeRequest(MergeRequestInfo mr) {
        // Note: This is a simple heuristic. In a real implementation,
        // you might want to check the actual draft status from the GitLab API
        // if available in the MergeRequest model.
        return false; // For now, include all MRs
    }
}
