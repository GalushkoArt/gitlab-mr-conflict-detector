package art.galushko.gitlab.mrconflict.gitlab;

import art.galushko.gitlab.mrconflict.model.ConflictInfo;
import art.galushko.gitlab.mrconflict.model.MergeResult;
import art.galushko.gitlab.mrconflict.model.MergeStatus;
import lombok.extern.slf4j.Slf4j;
import org.gitlab4j.api.Constants;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * GitLab4J-based implementation of GitLabClient interface.
 */
@Slf4j
public class GitLab4JClient implements GitLabClient {
    private GitLabApi gitLabApi;
    
    @Override
    public GitLab4JClient authenticate(String gitlabUrl, String accessToken) throws GitLabException {
        try {
            this.gitLabApi = new GitLabApi(gitlabUrl, accessToken);
            
            // Test authentication by getting current user
            var currentUser = gitLabApi.getUserApi().getCurrentUser();
            log.info("Successfully authenticated as user: {} ({})",currentUser.getUsername(), currentUser.getName());

            return this;
        } catch (GitLabApiException e) {
            throw new GitLabException("Failed to authenticate with GitLab: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Project getProject(Long projectId) throws GitLabException {
        try {
            return gitLabApi.getProjectApi().getProject(projectId);
        } catch (GitLabApiException e) {
            throw new GitLabException("Failed to get project with ID: " + projectId, e);
        }
    }
    
    @Override
    public Project getProject(String projectPath) throws GitLabException {
        try {
            return gitLabApi.getProjectApi().getProject(projectPath);
        } catch (GitLabApiException e) {
            throw new GitLabException("Failed to get project with path: " + projectPath, e);
        }
    }
    
    @Override
    public List<Branch> getBranches(Long projectId) throws GitLabException {
        try {
            return gitLabApi.getRepositoryApi().getBranches(projectId);
        } catch (GitLabApiException e) {
            throw new GitLabException("Failed to get branches for project: " + projectId, e);
        }
    }
    
    @Override
    public List<String> getProtectedBranches(Long projectId) throws GitLabException {
        try {
            List<ProtectedBranch> protectedBranches = gitLabApi.getProtectedBranchesApi()
                    .getProtectedBranches(projectId);
            
            return protectedBranches.stream()
                    .map(ProtectedBranch::getName)
                    .collect(Collectors.toList());
                    
        } catch (GitLabApiException e) {
            throw new GitLabException("Failed to get protected branches for project: " + projectId, e);
        }
    }
    
    @Override
    public Optional<Branch> getBranch(Long projectId, String branchName) throws GitLabException {
        try {
            Branch branch = gitLabApi.getRepositoryApi().getBranch(projectId, branchName);
            return Optional.ofNullable(branch);
        } catch (GitLabApiException e) {
            if (e.getHttpStatus() == 404) {
                return Optional.empty();
            }
            throw new GitLabException("Failed to get branch '" + branchName + 
                                    "' for project: " + projectId, e);
        }
    }
    
    @Override
    public List<MergeRequest> getMergeRequests(Long projectId, String state) throws GitLabException {
        try {
            MergeRequestFilter filter = new MergeRequestFilter()
                    .withProjectId(projectId)
                    .withState(Constants.MergeRequestState.forValue(state));
                    
            return gitLabApi.getMergeRequestApi().getMergeRequests(filter);
        } catch (GitLabApiException e) {
            throw new GitLabException("Failed to get merge requests for project: " + projectId, e);
        }
    }
    
    @Override
    public MergeRequest getMergeRequest(Long projectId, Long mergeRequestIid) throws GitLabException {
        try {
            return gitLabApi.getMergeRequestApi().getMergeRequest(projectId, mergeRequestIid);
        } catch (GitLabApiException e) {
            throw new GitLabException("Failed to get merge request " + mergeRequestIid + 
                                    " for project: " + projectId, e);
        }
    }
    
    @Override
    public void createConflictNote(Long projectId, Long mergeRequestIid, List<MergeResult> mergeResults) 
            throws GitLabException {
        try {
            String noteContent = formatConflictNote(mergeResults);
            
            gitLabApi.getNotesApi().createMergeRequestNote(projectId, mergeRequestIid, noteContent);
            
            log.info("Created conflict detection note for MR {} in project {}",
                       mergeRequestIid, projectId);
                       
        } catch (GitLabApiException e) {
            throw new GitLabException("Failed to create conflict note for MR " + mergeRequestIid, e);
        }
    }
    
    /**
     * Formats merge results into a readable note content.
     */
    private String formatConflictNote(List<MergeResult> mergeResults) {
        StringBuilder note = new StringBuilder();
        note.append("## 🔍 Merge Conflict Detection Report\n\n");
        
        boolean hasAnyConflicts = mergeResults.stream().anyMatch(MergeResult::hasConflicts);
        
        if (!hasAnyConflicts) {
            note.append("✅ **No conflicts detected** - All target branches can be merged cleanly.\n\n");
        } else {
            note.append("⚠️ **Conflicts detected** - Please review and resolve before merging.\n\n");
        }
        
        for (MergeResult result : mergeResults) {
            note.append("### Target Branch: `").append(result.getTargetBranch()).append("`\n");
            
            if (result.getStatus() == MergeStatus.CLEAN) {
                note.append("✅ No conflicts\n\n");
            } else if (result.getStatus() == MergeStatus.CONFLICTED) {
                note.append("❌ **").append(result.getConflictCount()).append(" conflicted files**\n");
                
                for (ConflictInfo conflict : result.getConflicts()) {
                    note.append("- `").append(conflict.getFilePath()).append("` (")
                        .append(conflict.getType().name().toLowerCase()).append(")\n");
                }
                note.append("\n");
            } else {
                note.append("⚠️ Detection failed: ").append(result.getMessage()).append("\n\n");
            }
        }
        
        note.append("---\n");
        note.append("*Generated by GitLab MR Conflict Detector at ")
             .append(java.time.Instant.now()).append("*");
        
        return note.toString();
    }
    
    @Override
    public void updateMergeRequestStatus(Long projectId, Long mergeRequestIid, boolean hasConflicts) 
            throws GitLabException {
        try {
            // Note: GitLab doesn't allow direct status updates via API for conflict detection
            // This would typically be handled by GitLab's built-in conflict detection
            // We can add labels or update description instead
            
            MergeRequest mr = getMergeRequest(projectId, mergeRequestIid);
            List<String> labels = mr.getLabels();
            
            if (hasConflicts) {
                if (!labels.contains("conflicts")) {
                    labels.add("conflicts");
                }
                labels.remove("no-conflicts");
            } else {
                if (!labels.contains("no-conflicts")) {
                    labels.add("no-conflicts");
                }
                labels.remove("conflicts");
            }
            
            gitLabApi.getMergeRequestApi().updateMergeRequest(projectId, mergeRequestIid, 
                    null, null, null, null, null, String.join(",", labels), null, null, null, null, null);
                    
            log.debug("Updated MR {} labels based on conflict status", mergeRequestIid);
            
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
            Project project = getProject(projectId);
            return project.getDefaultBranch();
        } catch (Exception e) {
            throw new GitLabException("Failed to get default branch for project: " + projectId, e);
        }
    }
    
    @Override
    public boolean isBranchProtected(Long projectId, String branchName) throws GitLabException {
        try {
            List<String> protectedBranches = getProtectedBranches(projectId);
            return protectedBranches.contains(branchName);
        } catch (Exception e) {
            throw new GitLabException("Failed to check if branch is protected: " + branchName, e);
        }
    }
    
    /**
     * Closes the GitLab API client and releases resources.
     */
    public void close() {
        if (gitLabApi != null) {
            gitLabApi.close();
            log.debug("Closed GitLab API client");
        }
    }
}

