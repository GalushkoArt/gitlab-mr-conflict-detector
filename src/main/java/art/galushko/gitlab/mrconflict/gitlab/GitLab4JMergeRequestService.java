package art.galushko.gitlab.mrconflict.gitlab;

import art.galushko.gitlab.mrconflict.model.MergeRequestInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gitlab4j.api.models.MergeRequest;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of MergeRequestService using GitLab4J API.
 * This class provides methods for fetching merge request data from GitLab
 * using the GitLab4J library. It handles retrieving merge requests, their
 * changed files, and converting GitLab API objects to internal model objects.
 */
@Slf4j
@RequiredArgsConstructor
public class GitLab4JMergeRequestService implements MergeRequestService {

    private final GitLabClient gitLabClient;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MergeRequestInfo> getOpenMergeRequests(Long projectId) throws GitLabException {
        return getMergeRequests(projectId, "opened");
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getChangedFiles(Long projectId, Long mergeRequestIid) throws GitLabException {
        log.debug("Fetching changed files for MR {} in project {}", mergeRequestIid, projectId);

        // Get the diffs for the merge request
        var diffs = gitLabClient.getMergeRequestChanges(projectId, mergeRequestIid);

        // Create a set to store all changed files
        var changedFiles = new LinkedHashSet<String>();

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

    /**
     * Converts a GitLab API MergeRequest object to our internal MergeRequestInfo model.
     * This method handles the conversion of data formats and determines if a merge request
     * is in draft state based on its flags and title prefixes.
     *
     * @param mergeRequest the GitLab API merge request object
     * @param changedFiles the set of files changed in this merge request
     * @return a MergeRequestInfo object containing the relevant information
     */
    protected MergeRequestInfo convertToMergeRequestInfo(MergeRequest mergeRequest, Set<String> changedFiles) {
        var isDraft = mergeRequest.getWorkInProgress() != null && mergeRequest.getWorkInProgress();

        // Also check title prefixes for older GitLab versions that don't support the draft flag
        var title = mergeRequest.getTitle();
        if (!isDraft && title != null) {
            isDraft = title.startsWith("Draft:") || title.startsWith("WIP:") || title.startsWith("[WIP]");
        }

        return MergeRequestInfo.builder()
                .id(mergeRequest.getIid())
                .title(mergeRequest.getTitle())
                .sourceBranch(mergeRequest.getSourceBranch())
                .targetBranch(mergeRequest.getTargetBranch())
                .changedFiles(changedFiles)
                .labels(new LinkedHashSet<>(mergeRequest.getLabels()))
                .draft(isDraft)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MergeRequestInfo> getMergeRequestsForConflictAnalysis(Long projectId, boolean includeDraftMrs) {
        log.info("Fetching merge requests for conflict analysis in project {}", projectId);

        var openMRs = getOpenMergeRequests(projectId);

        // Filter out draft/WIP merge requests if includeDraftMrs is false
        if (!includeDraftMrs) {
            log.debug("Filtering out draft/WIP merge requests");
            return openMRs.stream()
                    .filter(mr -> !isDraftMergeRequest(mr))
                    .collect(Collectors.toList());
        }

        return openMRs;
    }

    /**
     * Checks if a merge request is in the draft state.
     * Draft MRs are identified by their draft flag or by title prefixes like "Draft:", "WIP:", or "[WIP]"
     * 
     * @param mr the merge request to check
     * @return true if the merge request is a draft, false otherwise
     */
    private boolean isDraftMergeRequest(MergeRequestInfo mr) {
        return mr.draft();
    }
}
