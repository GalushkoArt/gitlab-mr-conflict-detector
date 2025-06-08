package art.galushko.gitlab.mrconflict.gitlab;

import art.galushko.gitlab.mrconflict.security.CredentialService;
import art.galushko.gitlab.mrconflict.security.InputValidator;
import art.galushko.gitlab.mrconflict.utils.ThrowingFunction;
import art.galushko.gitlab.mrconflict.utils.ThrowingRunnable;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.gitlab4j.api.Constants.MergeRequestState;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * GitLab4J-based implementation of the GitLabClient interface.
 */
@Slf4j
public class GitLab4JClient implements GitLabClient {
    private GitLabApi gitLabApi; // Package-private for service access
    private final CredentialService credentialService;
    private final InputValidator inputValidator;

    // Cache configuration
    private static final long DEFAULT_CACHE_TTL_SECONDS = 300; // 5 minutes

    // Caffeine caches for frequently accessed data with automatic expiration
    private final Cache<Long, Project> projectByIdCache;
    private final Cache<String, List<MergeRequest>> mergeRequestsCache;
    private final Cache<String, MergeRequest> mergeRequestCache;
    private final Cache<String, List<Diff>> mergeRequestChangesCache;
    private final Cache<String, User> currentUseCache;

    /**
     * Creates a new GitLab4JClient with security services.
     */
    public GitLab4JClient(CredentialService credentialService, InputValidator inputValidator) {
        this.credentialService = credentialService;
        this.inputValidator = inputValidator;
        // Initialize caches with Caffeine
        this.projectByIdCache = Caffeine.newBuilder()
                .expireAfterWrite(DEFAULT_CACHE_TTL_SECONDS, TimeUnit.SECONDS)
                .build();

        this.mergeRequestsCache = Caffeine.newBuilder()
                .expireAfterWrite(DEFAULT_CACHE_TTL_SECONDS, TimeUnit.SECONDS)
                .build();

        this.mergeRequestCache = Caffeine.newBuilder()
                .expireAfterWrite(DEFAULT_CACHE_TTL_SECONDS, TimeUnit.SECONDS)
                .build();

        this.mergeRequestChangesCache = Caffeine.newBuilder()
                .expireAfterWrite(DEFAULT_CACHE_TTL_SECONDS, TimeUnit.SECONDS)
                .build();

        this.currentUseCache = Caffeine.newBuilder()
                .expireAfterWrite(DEFAULT_CACHE_TTL_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public GitLab4JClient authenticate(String gitlabUrl, String accessToken) {
        try {
            // Validate token format
            if (!credentialService.isValidToken(accessToken)) {
                throw new GitLabException("Invalid GitLab token format");
            }

            // Use token from environment variable if available
            var token = credentialService.getGitLabToken(accessToken);
            var url = credentialService.getGitLabUrl(gitlabUrl);

            // Validate GitLab URL
            if (!inputValidator.isValidGitLabUrl(url)) {
                throw new GitLabException("Invalid GitLab URL format: " + url);
            }

            this.gitLabApi = new GitLabApi(url, token);

            // Test authentication by getting current user
            var currentUser = getCurrentUser();
            log.info("Successfully authenticated as user: {} ({})", currentUser.getUsername(), currentUser.getName());

            return this;
        } catch (RuntimeException e) {
            // Sanitize error message to remove token if present
            var sanitizedMessage = credentialService.sanitizeErrorMessage(e.getMessage(), accessToken);
            throw new GitLabException("Failed to authenticate with GitLab: " + sanitizedMessage, e);
        }
    }

    private User getCurrentUser() {
        return currentUseCache.get("user", withRetry(
                id -> gitLabApi.getUserApi().getCurrentUser(),
                "Failed to get current user"
        ));
    }

    @Override
    public Project getProject(Long projectId) {
        return projectByIdCache.get(projectId, withRetry(
                gitLabApi.getProjectApi()::getProject,
                "Failed to get project with ID: " + projectId
        ));
    }

    @Override
    public List<MergeRequest> getMergeRequests(Long projectId, String state) {
        var filter = new MergeRequestFilter().withProjectId(projectId).withState(MergeRequestState.forValue(state));

        // Use cache if available
        return mergeRequestsCache.get(
                state + projectId,
                withRetry(id -> {
                            // Get all pages of merge requests with pagination
                            log.debug("Fetching all merge requests for project {} with state {}", projectId, state);
                            List<MergeRequest> allMergeRequests = new ArrayList<>();

                            // Use Pager to handle pagination automatically
                            var pager = gitLabApi.getMergeRequestApi().getMergeRequests(filter, 96);
                            while (pager.hasNext()) {
                                allMergeRequests.addAll(pager.next());
                                log.debug("Fetched page of merge requests, total so far: {}", allMergeRequests.size());
                            }

                            log.debug("Fetched a total of {} merge requests", allMergeRequests.size());
                            return allMergeRequests;
                        },
                        "Failed to get merge requests for project: " + projectId
                )
        );
    }

    @Override
    public MergeRequest getMergeRequest(Long projectId, Long mergeRequestIid) {
        return mergeRequestCache.get(
                getMrCacheKey(projectId, mergeRequestIid),
                withRetry(
                        id -> gitLabApi.getMergeRequestApi().getMergeRequest(projectId, mergeRequestIid),
                        "Failed to get merge request " + mergeRequestIid + " for project: " + projectId
                )
        );
    }

    @Override
    public void updateMergeRequestStatus(Long projectId, Long mergeRequestIid, Set<String> labels) {
        // Note: GitLab doesn't allow direct status updates via API for conflict detection
        // This would typically be handled by GitLab's built-in conflict detection
        // We can add labels or update the description instead
        log.debug("Updating labels for merge request IID: {} of project ID: {}", mergeRequestIid, projectId);

        // Update the merge request with new labels
        var updatedMr = withRetry(
                id -> gitLabApi.getMergeRequestApi().updateMergeRequest(projectId, mergeRequestIid,
                        null, null, null, null, null, String.join(",", labels), null, null, null, null, null),
                "Failed to update merge request status"
        ).apply(mergeRequestIid);

        // Update cache with the updated merge request
        mergeRequestCache.put(getMrCacheKey(projectId, mergeRequestIid), updatedMr);
        log.debug("Updated MR {} labels based on conflict status", mergeRequestIid);
    }

    @Override
    public boolean hasProjectAccess(Long projectId) {
        try {
            gitLabApi.getProjectApi().getProject(projectId);
            return true;
        } catch (GitLabApiException e) {
            if (e.getHttpStatus() == 403 || e.getHttpStatus() == 404) {
                return false;
            }
            throw new GitLabException("Failed to check project access", e);
        }
    }

    @Override
    public List<Diff> getMergeRequestChanges(Long projectId, Long mergeRequestIid) {
        return mergeRequestChangesCache.get(
                getMrCacheKey(projectId, mergeRequestIid),
                withRetry(
                        id -> gitLabApi.getMergeRequestApi()
                                .getMergeRequestChanges(projectId, mergeRequestIid)
                                .getChanges(),
                        "Failed to get changes for MR " + mergeRequestIid
                )
        );
    }

    @Override
    public void createMergeRequestNote(Long projectId, Long mergeRequestIid, String noteContent) {
        // Validate inputs
        if (mergeRequestIid == null || mergeRequestIid <= 0) {
            throw new GitLabException("Invalid merge request IID: " + mergeRequestIid);
        }

        if (noteContent == null || noteContent.trim().isEmpty()) {
            throw new GitLabException("Note content cannot be empty");
        }

        executeWithRetry(
                () -> gitLabApi.getNotesApi().createMergeRequestNote(projectId, mergeRequestIid, noteContent),
                "Failed to create note for MR " + mergeRequestIid
        );
    }

    @NotNull
    private static String getMrCacheKey(Long projectId, Long mergeRequestIid) {
        return projectId + "_" + mergeRequestIid;
    }

    /**
     * Executes a GitLab API call with retry logic for transient failures.
     * This method will retry the API call if it fails with a 429 (Too Many Requests) or
     * 5xx (Server Error) status code.
     *
     * @param <T>          the return type of the API call
     * @param operation    the API call to execute
     * @param errorMessage the error message to use if all retries fail
     * @return the result of the API call
     * @throws GitLabException if the API call fails after all retries
     */
    private <T, R> Function<T, R> withRetry(ThrowingFunction<T, R> operation, String errorMessage) {
        return t -> {
            long maxRetries = 3;
            long retryDelayMs = 500;

            for (long attempt = 1L; attempt <= maxRetries; attempt++) {
                try {
                    return operation.apply(t);
                } catch (Exception e) {
                    // Check if it's a GitLab API exception with a retryable status code
                    if (e instanceof GitLabApiException apiEx) {
                        int statusCode = apiEx.getHttpStatus();

                        // Retry on rate limiting (429) or server errors (5xx)
                        boolean shouldRetry = statusCode == 429 || (statusCode >= 500 && statusCode < 600);

                        if (shouldRetry && attempt < maxRetries) {
                            // Log the failure and retry
                            log.warn("GitLab API call failed with status {}, retrying ({}/{}): {}",
                                    statusCode, attempt, maxRetries, apiEx.getMessage());

                            try {
                                Thread.sleep(retryDelayMs * attempt);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                throw new GitLabException("Retry interrupted", ie);
                            }
                            continue;
                        }
                    }

                    // If we get here, either it's not a retryable error or we've exhausted retries
                    if (attempt == maxRetries) {
                        log.error("GitLab API call failed after {} attempts: {}", maxRetries, e.getMessage());
                    }
                    throw new GitLabException(errorMessage, e);
                }
            }

            // This should never happen, but just in case
            throw new GitLabException(errorMessage + " (unexpected error)");
        };
    }

    private void executeWithRetry(ThrowingRunnable operation, String errorMessage) {
        withRetry(id -> {
            operation.run();
            return null;
        }, errorMessage).apply(true);
    }
}
