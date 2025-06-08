package art.galushko.gitlab.mrconflict.core;

import art.galushko.gitlab.mrconflict.config.AppConfig;
import art.galushko.gitlab.mrconflict.di.ServiceFactory;
import art.galushko.gitlab.mrconflict.formatter.ConflictFormatter;
import art.galushko.gitlab.mrconflict.gitlab.GitLabClient;
import art.galushko.gitlab.mrconflict.gitlab.GitLabException;
import art.galushko.gitlab.mrconflict.gitlab.MergeRequestService;
import art.galushko.gitlab.mrconflict.model.MergeRequestConflict;
import art.galushko.gitlab.mrconflict.model.MergeRequestInfo;
import art.galushko.gitlab.mrconflict.security.CredentialService;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * Service for analyzing merge request conflicts.
 * This class separates business logic from CLI concerns.
 */
@Slf4j
public class ConflictAnalysisService {

    private final GitLabClient gitLabClient;
    private final MergeRequestService mergeRequestService;
    private final ConflictDetector conflictDetector;
    private final ConflictFormatter conflictFormatter;
    private final CredentialService credentialService;
    private final AppConfig config;
    private static final String CONFLICTS_LABEL = "conflicts";
    private static final String CONFLICT_LABEL_PREFIX = "conflict:MR";

    /**
     * Creates a new ConflictAnalysisService.
     */
    public ConflictAnalysisService() {
        var serviceFactory = ServiceFactory.getInstance();
        this.config = serviceFactory.getConfig();
        this.gitLabClient = serviceFactory.getGitLabClient();
        this.mergeRequestService = serviceFactory.getMergeRequestService();
        this.conflictDetector = serviceFactory.getConflictDetector();
        this.conflictFormatter = serviceFactory.getConflictFormatter();
        this.credentialService = new CredentialService();
    }

    /**
     * Authenticates with GitLab.
     *
     * @throws GitLabException if authentication fails
     */
    public void authenticate() throws GitLabException {
        var token = config.getGitlabToken();
        var url = config.getGitlabUrl();

        // Log with masked token
        log.debug("Authenticating with GitLab at {} using token {}", url, credentialService.maskToken(token));

        gitLabClient.authenticate(url, token);
    }

    /**
     * Checks if the user has access to the specified project.
     *
     * @return true if the user has access
     * @throws GitLabException if the check fails
     */
    public boolean hasAccessToProjectFromConfig() throws GitLabException {
        return gitLabClient.hasProjectAccess(config.getProjectId());
    }

    /**
     * Fetches merge requests for conflict analysis.
     *
     * @return list of merge requests
     * @throws GitLabException if merge requests cannot be fetched
     */
    public List<MergeRequestInfo> fetchMergeRequests()
            throws GitLabException {
        var projectId = config.getProjectId();
        var specificMergeRequestIids = config.getMergeRequestIids();

        if (specificMergeRequestIids != null && !specificMergeRequestIids.isEmpty()) {
            // Fetch specific merge request
            log.info("Fetching specific merge requests: {}", specificMergeRequestIids);
            return specificMergeRequestIids.stream()
                    .map(iid -> mergeRequestService.getMergeRequest(projectId, iid))
                    .toList();
        } else {
            // Fetch all open merge requests for conflict analysis
            log.info("Fetching all open merge requests for conflict analysis");
            return mergeRequestService.getMergeRequestsForConflictAnalysis(projectId, config.getIncludeDraftMrs());
        }
    }

    /**
     * Detects conflicts between merge requests.
     *
     * @param mergeRequests  list of merge requests to analyze
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
    public Set<Long> getConflictingMergeRequestIds(List<MergeRequestConflict> conflicts) {
        return conflictDetector.getConflictingMergeRequestIds(conflicts);
    }


    /**
     * Updates GitLab with conflict information.
     *
     * @param projectId         GitLab project ID
     * @param conflicts         list of conflicts
     * @param createNotes       whether to create notes on merge requests
     * @param updateStatus      whether to update the merge request status
     * @param dryRun            whether to perform a dry run (no changes)
     */
    public void updateGitLabWithConflicts(Long projectId,
                                          List<MergeRequestConflict> conflicts,
                                          boolean createNotes, boolean updateStatus, boolean dryRun) {
        try {
            var mergeRequests = mergeRequestService.getMergeRequests(projectId, "opened");
            log.info("Processing {} merge requests for conflict updates", mergeRequests.size());

            for (var mergeRequest : mergeRequests) {
                final long mrId = mergeRequest.id();
                log.debug("Processing MR #{} ({})", mrId, mergeRequest.title());

                // Find all conflicts involving this MR
                final List<MergeRequestConflict> relevantConflicts = findConflictsForMr(conflicts, mrId);
                final Set<Long> conflictingMrIds = extractConflictingMrIds(relevantConflicts, mrId);

                // Get current MR details and labels
                final var labels = new LinkedHashSet<>(mergeRequest.labels());
                final var originalLabels = new LinkedHashSet<>(labels);

                // Update labels based on conflict status
                if (relevantConflicts.isEmpty()) {
                    labels.remove(CONFLICTS_LABEL);
                    labels.removeIf(label -> label.startsWith(CONFLICT_LABEL_PREFIX));
                } else {
                    labels.add(CONFLICTS_LABEL);
                    final Set<String> newConflictLabels = conflictingMrIds.stream()
                            .map(id -> CONFLICT_LABEL_PREFIX + id)
                            .collect(toSet());

                    labels.removeIf(label -> label.startsWith(CONFLICT_LABEL_PREFIX));

                    labels.addAll(newConflictLabels);
                }

                // Apply changes if labels were modified
                if (labelsHaveChanged(originalLabels, labels)) {
                    final Set<String> resolvedConflictLabels = findResolvedConflictLabels(originalLabels, labels);
                    log.info("""
                            Updating labels for MR {} in project {}:
                            Current labels: {}
                            Resolved labels: {}""", mergeRequest.title(), projectId, labels, resolvedConflictLabels);

                    if (updateStatus && !dryRun) {
                        gitLabClient.updateMergeRequestStatus(projectId, mrId, labels);
                    }

                    if (createNotes && !dryRun) {
                        createConflictNote(projectId, mrId, relevantConflicts, resolvedConflictLabels);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Failed to update GitLab with conflict information: {}", e.getMessage());
        }
    }

    /**
     * Creates a note on a merge request with conflict information.
     *
     * @param projectId         GitLab project ID
     * @param mergeRequestIid   merge request IID
     * @param conflicts         list of conflicts
     * @param resolvedConflicts list of resolved conflicts (labels)
     */
    private void createConflictNote(Long projectId, Long mergeRequestIid, List<MergeRequestConflict> conflicts, Set<String> resolvedConflicts) {
        try {
            var resolvedConflictMrs = resolvedConflicts.stream()
                    .map(label -> label.replaceFirst("conflict:MR", ""))
                    .map(Long::parseLong)
                    .map(iid -> gitLabClient.getMergeRequest(projectId, iid))
                    .toList();
            var noteContent = conflictFormatter.formatConflictNote(conflicts, mergeRequestIid, resolvedConflictMrs);

            gitLabClient.createMergeRequestNote(projectId, mergeRequestIid, noteContent);

            log.info("Created conflict note for MR {} in project {}", mergeRequestIid, projectId);
        } catch (Exception e) {
            log.error("Failed to create conflict note for MR {}: {}", mergeRequestIid, e.getMessage());
        }
    }

    private List<MergeRequestConflict> findConflictsForMr(List<MergeRequestConflict> conflicts, long mrId) {
        return conflicts.stream()
                .filter(conflict -> isInvolvedInConflict(conflict, mrId))
                .toList();
    }

    private boolean isInvolvedInConflict(MergeRequestConflict conflict, long mrId) {
        return conflict.firstMr().id() == mrId || conflict.secondMr().id() == mrId;
    }

    private Set<Long> extractConflictingMrIds(List<MergeRequestConflict> conflicts, long mrId) {
        return conflicts.stream()
                .map(conflict -> getOtherMrId(conflict, mrId))
                .collect(toSet());
    }

    private long getOtherMrId(MergeRequestConflict conflict, long mrId) {
        return conflict.firstMr().id() == mrId ?
                conflict.secondMr().id() :
                conflict.firstMr().id();
    }

    private boolean labelsHaveChanged(Set<String> originalLabels, Set<String> newLabels) {
        return !originalLabels.equals(newLabels);
    }

    private Set<String> findResolvedConflictLabels(Set<String> originalLabels, Set<String> newLabels) {
        return originalLabels.stream()
                .filter(label -> label.startsWith(CONFLICT_LABEL_PREFIX))
                .filter(label -> !newLabels.contains(label))
                .collect(toSet());
    }

}
