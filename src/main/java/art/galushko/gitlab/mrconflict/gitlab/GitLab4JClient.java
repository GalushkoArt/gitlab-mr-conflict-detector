package art.galushko.gitlab.mrconflict.gitlab;

import art.galushko.gitlab.mrconflict.security.CredentialService;
import art.galushko.gitlab.mrconflict.security.InputValidator;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.gitlab4j.api.Constants.MergeRequestState;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static art.galushko.gitlab.mrconflict.utils.CollectionUtils.mapCollection;
import static art.galushko.gitlab.mrconflict.utils.ThrowingFunction.wrap;

/**
 * GitLab4J-based implementation of GitLabClient interface.
 */
@Slf4j
public class GitLab4JClient implements GitLabClient {
    GitLabApi gitLabApi; // Package-private for service access
    private final CredentialService credentialService;
    private final InputValidator inputValidator;

    // Cache configuration
    private static final long DEFAULT_CACHE_TTL_SECONDS = 300; // 5 minutes

    // Caffeine caches for frequently accessed data with automatic expiration
    private final Cache<Long, Project> projectByIdCache;
    private final Cache<String, Project> projectByPathCache;
    private final Cache<Long, List<Branch>> branchesCache;
    private final Cache<Long, List<String>> protectedBranchesCache;
    private final Cache<String, List<MergeRequest>> mergeRequestsCache;
    private final Cache<String, MergeRequest> mergeRequestCache;
    private final Cache<String, List<Diff>> mergeRequestChangesCache;

    /**
     * Creates a new GitLab4JClient with security services.
     */
    public GitLab4JClient() {
        this.credentialService = new CredentialService();
        this.inputValidator = new InputValidator();

        // Initialize caches with Caffeine
        this.projectByIdCache = Caffeine.newBuilder()
                .expireAfterWrite(DEFAULT_CACHE_TTL_SECONDS, TimeUnit.SECONDS)
                .build();

        this.projectByPathCache = Caffeine.newBuilder()
                .expireAfterWrite(DEFAULT_CACHE_TTL_SECONDS, TimeUnit.SECONDS)
                .build();

        this.branchesCache = Caffeine.newBuilder()
                .expireAfterWrite(DEFAULT_CACHE_TTL_SECONDS, TimeUnit.SECONDS)
                .build();

        this.protectedBranchesCache = Caffeine.newBuilder()
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
    }

    /**
     * Clears all caches.
     */
    public void clearCaches() {
        log.debug("Clearing all GitLab API caches");
        projectByIdCache.invalidateAll();
        projectByPathCache.invalidateAll();
        branchesCache.invalidateAll();
        protectedBranchesCache.invalidateAll();
        mergeRequestsCache.invalidateAll();
        mergeRequestCache.invalidateAll();
    }

    @Override
    public GitLab4JClient authenticate(String gitlabUrl, String accessToken) throws GitLabException {
        try {
            // Validate token format
            if (!credentialService.isValidToken(accessToken)) {
                throw new GitLabException("Invalid GitLab token format");
            }

            // Use token from environment variable if available
            String token = credentialService.getGitLabToken(accessToken);
            String url = credentialService.getGitLabUrl(gitlabUrl);

            // Validate GitLab URL
            if (!inputValidator.isValidGitLabUrl(url)) {
                throw new GitLabException("Invalid GitLab URL format: " + url);
            }

            this.gitLabApi = new GitLabApi(url, token);

            // Test authentication by getting current user
            var currentUser = gitLabApi.getUserApi().getCurrentUser();
            log.info("Successfully authenticated as user: {} ({})", currentUser.getUsername(), currentUser.getName());

            return this;
        } catch (GitLabApiException e) {
            // Sanitize error message to remove token if present
            String sanitizedMessage = credentialService.sanitizeErrorMessage(e.getMessage(), accessToken);
            throw new GitLabException("Failed to authenticate with GitLab: " + sanitizedMessage, e);
        }
    }

    @Override
    public Project getProject(Long projectId) throws GitLabException {
        try {
            return projectByIdCache.get(projectId, wrap(gitLabApi.getProjectApi()::getProject));
        } catch (RuntimeException e) {
            throw new GitLabException("Failed to get project with ID: " + projectId, e);
        }
    }

    @Override
    public Project getProject(String projectPath) throws GitLabException {
        try {
            return projectByPathCache.get(projectPath, wrap(gitLabApi.getProjectApi()::getProject));
        } catch (RuntimeException e) {
            throw new GitLabException("Failed to get project with path: " + projectPath, e);
        }
    }

    @Override
    public List<Branch> getBranches(Long projectId) throws GitLabException {
        try {
            return branchesCache.get(projectId, wrap(gitLabApi.getRepositoryApi()::getBranches));
        } catch (RuntimeException e) {
            throw new GitLabException("Failed to get branches for project: " + projectId, e);
        }
    }

    @Override
    public List<String> getProtectedBranches(Long projectId) throws GitLabException {
        try {
            return protectedBranchesCache.get(
                    projectId,
                    wrap(gitLabApi.getProtectedBranchesApi()::getProtectedBranches)
                            .andThen(mapCollection(ProtectedBranch::getName))
            );
        } catch (RuntimeException e) {
            throw new GitLabException("Failed to get protected branches for project: " + projectId, e);
        }
    }

    @Override
    public Optional<Branch> getBranch(Long projectId, String branchName) throws GitLabException {
        try {
            // Validate inputs
            if (branchName == null || branchName.trim().isEmpty()) {
                throw new GitLabException("Branch name cannot be empty");
            }

            // Validate branch name format
            if (!inputValidator.isValidBranchName(branchName)) {
                throw new GitLabException("Invalid branch name format: " + branchName);
            }

            // Sanitize branch name for API call
            var sanitizedBranchName = inputValidator.escapeForGitLabApi(branchName);

            var branch = gitLabApi.getRepositoryApi().getBranch(projectId, sanitizedBranchName);
            return Optional.ofNullable(branch);
        } catch (GitLabApiException e) {
            if (e.getHttpStatus() == 404) {
                return Optional.empty();
            }
            throw new GitLabException("Failed to get branch '" + branchName + "' for project: " + projectId, e);
        }
    }

    @Override
    public List<MergeRequest> getMergeRequests(Long projectId, String state) throws GitLabException {
        try {
            var filter = new MergeRequestFilter().withProjectId(projectId).withState(MergeRequestState.forValue(state));
            return mergeRequestsCache.get(
                    state + projectId,
                    wrap(id -> gitLabApi.getMergeRequestApi().getMergeRequests(filter))
            );
        } catch (RuntimeException e) {
            throw new GitLabException("Failed to get merge requests for project: " + projectId, e);
        }
    }

    @Override
    public MergeRequest getMergeRequest(Long projectId, Long mergeRequestIid) throws GitLabException {
        try {
            return mergeRequestCache.get(
                    getMrCacheKey(projectId, mergeRequestIid),
                    wrap(id -> gitLabApi.getMergeRequestApi().getMergeRequest(projectId, mergeRequestIid))
            );
        } catch (RuntimeException e) {
            throw new GitLabException("Failed to get merge request " + mergeRequestIid +
                    " for project: " + projectId, e);
        }
    }

    @Override
    public void updateMergeRequestStatus(Long projectId, Long mergeRequestIid, boolean hasConflicts)
            throws GitLabException {
        try {
            // Note: GitLab doesn't allow direct status updates via API for conflict detection
            // This would typically be handled by GitLab's built-in conflict detection
            // We can add labels or update description instead

            // Get merge request (will use cache if available)
            var mr = getMergeRequest(projectId, mergeRequestIid);
            var labels = mr.getLabels();
            boolean labelsChanged = false;

            if (hasConflicts && !labels.contains("conflicts")) {
                labels.add("conflicts");
                labelsChanged = true;
            } else if (!hasConflicts && labels.contains("conflicts")) {
                labels.remove("conflicts");
                labelsChanged = true;
            }

            // Only update if labels have changed
            if (labelsChanged) {
                log.debug("Updating labels for merge request IID: {} of project ID: {}", mergeRequestIid, projectId);

                // Update the merge request with new labels
                var updatedMr = gitLabApi.getMergeRequestApi().updateMergeRequest(projectId, mergeRequestIid,
                        null, null, null, null, null, String.join(",", labels), null, null, null, null, null);

                // Update cache with the updated merge request
                mergeRequestCache.put(getMrCacheKey(projectId, mergeRequestIid), updatedMr);
                log.debug("Updated MR {} labels based on conflict status", mergeRequestIid);
            } else {
                log.debug("No label changes needed for merge request IID: {} of project ID: {}", mergeRequestIid, projectId);
            }

        } catch (GitLabApiException e) {
            throw new GitLabException("Failed to update merge request status", e);
        }
    }

    @Override
    public boolean hasProjectAccess(Long projectId) throws GitLabException {
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
    public String getDefaultBranch(Long projectId) throws GitLabException {
        try {
            return getProject(projectId).getDefaultBranch();
        } catch (Exception e) {
            throw new GitLabException("Failed to get default branch for project: " + projectId, e);
        }
    }

    @Override
    public boolean isBranchProtected(Long projectId, String branchName) throws GitLabException {
        try {
            return getProtectedBranches(projectId).contains(branchName);
        } catch (Exception e) {
            throw new GitLabException("Failed to check if branch is protected: " + branchName, e);
        }
    }

    @Override
    public List<Diff> getMergeRequestChanges(Long projectId, Long mergeRequestIid) throws GitLabException {
        try {
            return mergeRequestChangesCache.get(
                    getMrCacheKey(projectId, mergeRequestIid),
                    wrap(id -> gitLabApi.getMergeRequestApi()
                            .getMergeRequestChanges(projectId, mergeRequestIid)
                            .getChanges())
            );
        } catch (RuntimeException e) {
            throw new GitLabException("Failed to get changes for MR " + mergeRequestIid, e);
        }
    }

    @Override
    public void createMergeRequestNote(Long projectId, Long mergeRequestIid, String noteContent) throws GitLabException {
        try {
            // Validate inputs
            if (mergeRequestIid == null || mergeRequestIid <= 0) {
                throw new GitLabException("Invalid merge request IID: " + mergeRequestIid);
            }

            if (noteContent == null || noteContent.trim().isEmpty()) {
                throw new GitLabException("Note content cannot be empty");
            }

            // Sanitize note content to prevent injection attacks
            gitLabApi.getNotesApi()
                    .createMergeRequestNote(projectId, mergeRequestIid, inputValidator.sanitizeInput(noteContent));
        } catch (GitLabApiException e) {
            throw new GitLabException("Failed to create note for MR " + mergeRequestIid, e);
        }
    }

    @NotNull
    private static String getMrCacheKey(Long projectId, Long mergeRequestIid) {
        return projectId + "_" + mergeRequestIid;
    }
}
