package art.galushko.gitlab.mrconflict.core;

import art.galushko.gitlab.mrconflict.di.ServiceFactory;
import art.galushko.gitlab.mrconflict.gitlab.GitLabClient;
import art.galushko.gitlab.mrconflict.gitlab.GitLabException;
import art.galushko.gitlab.mrconflict.gitlab.MergeRequestService;
import art.galushko.gitlab.mrconflict.model.MergeRequestConflict;
import art.galushko.gitlab.mrconflict.model.MergeRequestInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for analyzing merge request conflicts.
 * This class separates business logic from CLI concerns.
 */
@Slf4j
public class ConflictAnalysisService {
    
    private final ServiceFactory serviceFactory;
    private final GitLabClient gitLabClient;
    private final MergeRequestService mergeRequestService;
    private final ConflictDetector conflictDetector;
    
    /**
     * Creates a new ConflictAnalysisService.
     */
    public ConflictAnalysisService() {
        this.serviceFactory = ServiceFactory.getInstance();
        this.gitLabClient = serviceFactory.getGitLabClient();
        this.mergeRequestService = serviceFactory.getMergeRequestService();
        this.conflictDetector = serviceFactory.getConflictDetector();
    }
    
    /**
     * Authenticates with GitLab.
     *
     * @param gitlabUrl GitLab instance URL
     * @param gitlabToken GitLab personal access token
     * @throws GitLabException if authentication fails
     */
    public void authenticate(String gitlabUrl, String gitlabToken) throws GitLabException {
        gitLabClient.authenticate(gitlabUrl, gitlabToken);
    }
    
    /**
     * Checks if the user has access to the specified project.
     *
     * @param projectId GitLab project ID
     * @return true if the user has access
     * @throws GitLabException if the check fails
     */
    public boolean hasProjectAccess(Long projectId) throws GitLabException {
        return gitLabClient.hasProjectAccess(projectId);
    }
    
    /**
     * Fetches merge requests for conflict analysis.
     *
     * @param projectId GitLab project ID
     * @param specificMergeRequestIid specific merge request IID (optional)
     * @return list of merge requests
     * @throws GitLabException if merge requests cannot be fetched
     */
    public List<MergeRequestInfo> fetchMergeRequests(Long projectId, Long specificMergeRequestIid) 
            throws GitLabException {
        
        if (specificMergeRequestIid != null) {
            // Fetch specific merge request
            log.info("Fetching specific merge request: {}", specificMergeRequestIid);
            var mr = mergeRequestService.getMergeRequest(projectId, specificMergeRequestIid);
            return List.of(mr);
        } else {
            // Fetch all open merge requests for conflict analysis
            log.info("Fetching all open merge requests for conflict analysis");
            return mergeRequestService.getOpenMergeRequests(projectId);
        }
    }
    
    /**
     * Detects conflicts between merge requests.
     *
     * @param mergeRequests list of merge requests to analyze
     * @param ignorePatterns patterns for files/directories to ignore
     * @return list of detected conflicts
     */
    public List<MergeRequestConflict> detectConflicts(List<MergeRequestInfo> mergeRequests, 
                                                     List<String> ignorePatterns) {
        return conflictDetector.detectConflicts(mergeRequests, ignorePatterns);
    }
    
    /**
     * Formats conflicts into a human-readable string.
     *
     * @param conflicts list of conflicts
     * @return formatted output
     */
    public String formatConflicts(List<MergeRequestConflict> conflicts) {
        if (conflicts.isEmpty()) {
            return "No conflicts detected.";
        }
        
        return conflictDetector.formatConflicts(conflicts);
    }
    
    /**
     * Gets the IDs of merge requests that have conflicts.
     *
     * @param conflicts list of conflicts
     * @return set of merge request IDs
     */
    public Set<Integer> getConflictingMergeRequestIds(List<MergeRequestConflict> conflicts) {
        return conflictDetector.getConflictingMergeRequestIds(conflicts);
    }
    
    /**
     * Updates GitLab with conflict information.
     *
     * @param projectId GitLab project ID
     * @param conflictingMrIds IDs of merge requests with conflicts
     * @param conflicts list of conflicts
     * @param createNotes whether to create notes on merge requests
     * @param updateStatus whether to update merge request status
     * @param dryRun whether to perform a dry run (no changes)
     */
    public void updateGitLabWithConflicts(Long projectId, Set<Integer> conflictingMrIds,
                                         List<MergeRequestConflict> conflicts,
                                         boolean createNotes, boolean updateStatus, boolean dryRun) {
        if (dryRun) {
            log.info("Dry run mode - skipping GitLab updates");
            return;
        }

        try {
            for (Integer mrId : conflictingMrIds) {
                if (createNotes) {
                    createConflictNote(projectId, mrId.longValue(), conflicts);
                }

                if (updateStatus) {
                    gitLabClient.updateMergeRequestStatus(projectId, mrId.longValue(), true);
                }
            }

        } catch (Exception e) {
            log.warn("Failed to update GitLab with conflict information: {}", e.getMessage());
        }
    }
    
    /**
     * Creates a note on a merge request with conflict information.
     *
     * @param projectId GitLab project ID
     * @param mergeRequestIid merge request IID
     * @param conflicts list of conflicts
     */
    private void createConflictNote(Long projectId, Long mergeRequestIid,
                                   List<MergeRequestConflict> conflicts) {
        try {
            // Find conflicts involving this MR
            var relevantConflicts = conflicts.stream()
                    .filter(conflict -> conflict.firstMr().id() == mergeRequestIid ||
                            conflict.secondMr().id() == mergeRequestIid)
                    .collect(Collectors.toList());

            if (!relevantConflicts.isEmpty()) {
                String noteContent = formatConflictNote(relevantConflicts, mergeRequestIid);

                gitLabClient.createMergeRequestNote(projectId, mergeRequestIid, noteContent);

                log.info("Created conflict note for MR {} in project {}", mergeRequestIid, projectId);
            }

        } catch (Exception e) {
            log.warn("Failed to create conflict note for MR {}: {}", mergeRequestIid, e.getMessage());
        }
    }
    
    /**
     * Formats a note with conflict information.
     *
     * @param conflicts list of conflicts
     * @param mergeRequestIid merge request IID
     * @return formatted note
     */
    private String formatConflictNote(List<MergeRequestConflict> conflicts, Long mergeRequestIid) {
        StringBuilder note = new StringBuilder();
        note.append("## Merge Request Conflict Analysis\n\n");
        note.append("This merge request has conflicts with the following merge requests:\n\n");

        for (MergeRequestConflict conflict : conflicts) {
            MergeRequestInfo otherMr = conflict.firstMr().id() == mergeRequestIid ?
                    conflict.secondMr() : conflict.firstMr();

            note.append("### Conflict with MR !").append(otherMr.id())
                    .append(" (").append(otherMr.title()).append(")\n");
            note.append("- **Source branch:** ").append(otherMr.sourceBranch()).append("\n");
            note.append("- **Target branch:** ").append(otherMr.targetBranch()).append("\n");
            note.append("- **Conflict reason:** ").append(conflict.reason()).append("\n");
            note.append("- **Conflicting files:** ").append(conflict.conflictingFiles().size()).append("\n");

            // List conflicting files (limit to 10 to avoid huge comments)
            int fileLimit = Math.min(conflict.conflictingFiles().size(), 10);
            for (int i = 0; i < fileLimit; i++) {
                note.append("  - `").append(conflict.conflictingFiles().get(i)).append("`\n");
            }

            if (conflict.conflictingFiles().size() > fileLimit) {
                note.append("  - ... and ").append(conflict.conflictingFiles().size() - fileLimit)
                        .append(" more files\n");
            }

            note.append("\n");
        }

        note.append("Please resolve these conflicts before merging.\n");
        return note.toString();
    }
}