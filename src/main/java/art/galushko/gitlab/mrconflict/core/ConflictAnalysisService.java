package art.galushko.gitlab.mrconflict.core;

import art.galushko.gitlab.mrconflict.di.ServiceFactory;
import art.galushko.gitlab.mrconflict.formatter.ConflictFormatter;
import art.galushko.gitlab.mrconflict.gitlab.GitLabClient;
import art.galushko.gitlab.mrconflict.gitlab.GitLabException;
import art.galushko.gitlab.mrconflict.gitlab.MergeRequestService;
import art.galushko.gitlab.mrconflict.model.MergeRequestConflict;
import art.galushko.gitlab.mrconflict.model.MergeRequestInfo;
import art.galushko.gitlab.mrconflict.security.CredentialService;
import art.galushko.gitlab.mrconflict.security.InputValidator;
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
    private final ConflictFormatter conflictFormatter;
    private final CredentialService credentialService;
    private final InputValidator inputValidator;

    /**
     * Creates a new ConflictAnalysisService.
     */
    public ConflictAnalysisService() {
        this.serviceFactory = ServiceFactory.getInstance();
        this.gitLabClient = serviceFactory.getGitLabClient();
        this.mergeRequestService = serviceFactory.getMergeRequestService();
        this.conflictDetector = serviceFactory.getConflictDetector();
        this.conflictFormatter = serviceFactory.getConflictFormatter();
        this.credentialService = new CredentialService();
        this.inputValidator = new InputValidator();
    }

    /**
     * Authenticates with GitLab.
     * Supports reading credentials from environment variables.
     *
     * @param gitlabUrl GitLab instance URL
     * @param gitlabToken GitLab personal access token
     * @throws GitLabException if authentication fails
     */
    public void authenticate(String gitlabUrl, String gitlabToken) throws GitLabException {
        // Validate and potentially get credentials from environment variables
        String token = credentialService.getGitLabToken(gitlabToken);
        String url = credentialService.getGitLabUrl(gitlabUrl);

        // Validate token format
        if (!credentialService.isValidToken(token)) {
            throw new GitLabException("Invalid GitLab token format");
        }

        // Log with masked token
        log.debug("Authenticating with GitLab at {} using token {}", url, credentialService.maskToken(token));

        gitLabClient.authenticate(url, token);
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

        // Validate project ID
        if (projectId == null || projectId <= 0) {
            throw new GitLabException("Invalid project ID: " + projectId);
        }

        // Validate project ID format
        if (!inputValidator.isValidProjectId(projectId.toString())) {
            throw new GitLabException("Invalid project ID format: " + projectId);
        }

        if (specificMergeRequestIid != null) {
            // Validate merge request IID if provided
            if (specificMergeRequestIid <= 0) {
                throw new GitLabException("Invalid merge request IID: " + specificMergeRequestIid);
            }

            // Validate merge request IID format
            if (!inputValidator.isValidMergeRequestIid(specificMergeRequestIid.toString())) {
                throw new GitLabException("Invalid merge request IID format: " + specificMergeRequestIid);
            }

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
        return conflictFormatter.formatConflicts(conflicts);
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
                String noteContent = conflictFormatter.formatConflictNote(relevantConflicts, mergeRequestIid);

                gitLabClient.createMergeRequestNote(projectId, mergeRequestIid, noteContent);

                log.info("Created conflict note for MR {} in project {}", mergeRequestIid, projectId);
            }

        } catch (Exception e) {
            log.warn("Failed to create conflict note for MR {}: {}", mergeRequestIid, e.getMessage());
        }
    }

}
