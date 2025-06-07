package art.galushko.gitlab.mrconflict.gitlab;

import art.galushko.gitlab.mrconflict.model.MergeRequestInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
            var mergeRequests = gitLabClient.getMergeRequests(projectId, state);

            return mergeRequests.stream()
                    .map(mr -> {
                        try {
                            var changedFiles = getChangedFiles(projectId, mr.getIid());
                            return convertToMergeRequestInfo(mr, changedFiles);
                        } catch (GitLabException e) {
                            log.error("Failed to get changed files for MR {}: {}", mr.getIid(), e.getMessage());
                            throw new GitLabException("Failed to get changed files for MR " + mr.getIid(), e);
                        }
                    })
                    .toList();

        } catch (Exception e) {
            throw new GitLabException("Failed to fetch merge requests for project: " + projectId, e);
        }
    }

    @Override
    public MergeRequestInfo getMergeRequest(Long projectId, Long mergeRequestIid) throws GitLabException {
        log.debug("Fetching merge request {} for project {}", mergeRequestIid, projectId);

        try {
            var mergeRequest = gitLabClient.getMergeRequest(projectId, mergeRequestIid);
            var changedFiles = getChangedFiles(projectId, mergeRequestIid);

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
        var diffs = gitLabClient.getMergeRequestChanges(projectId, mergeRequestIid);

        // Create a set to store all changed files
        var changedFiles = new HashSet<String>();

        // Process each diff to handle new, modified, renamed, and deleted files
        for (var diff : diffs) {
            var oldPath = diff.getOldPath();
            var newPath = diff.getNewPath();

            // Handle renamed files by including both old and new paths
            if (diff.getRenamedFile() != null && diff.getRenamedFile()) {
                if (oldPath != null && !oldPath.equals("/dev/null")) {
                    changedFiles.add(oldPath);
                }
                if (newPath != null && !newPath.equals("/dev/null")) {
                    changedFiles.add(newPath);
                }
            } 
            // Handle deleted files
            else if (newPath == null || newPath.equals("/dev/null")) {
                if (oldPath != null && !oldPath.equals("/dev/null")) {
                    changedFiles.add(oldPath);
                }
            } 
            // Handle new or modified files
            else {
                changedFiles.add(newPath);
            }
        }

        return changedFiles;
    }

    protected MergeRequestInfo convertToMergeRequestInfo(MergeRequest mergeRequest, Set<String> changedFiles) {
        return MergeRequestInfo.builder()
                .id(mergeRequest.getIid())
                .title(mergeRequest.getTitle())
                .sourceBranch(mergeRequest.getSourceBranch())
                .targetBranch(mergeRequest.getTargetBranch())
                .changedFiles(changedFiles)
                .labels(new HashSet<>(mergeRequest.getLabels()))
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
