package art.galushko.gitlab.mrconflict.core;

import art.galushko.gitlab.mrconflict.git.GitOperationException;
import art.galushko.gitlab.mrconflict.git.GitRepository;
import art.galushko.gitlab.mrconflict.model.MergeResult;
import art.galushko.gitlab.mrconflict.model.MergeStatus;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Core conflict detection engine that orchestrates the conflict detection process.
 */
@Slf4j
public class ConflictDetector {
    private final GitRepository gitRepository;
    private final ConflictDetectionConfig config;
    
    public ConflictDetector(GitRepository gitRepository, ConflictDetectionConfig config) {
        this.gitRepository = Objects.requireNonNull(gitRepository, "Git repository cannot be null");
        this.config = Objects.requireNonNull(config, "Configuration cannot be null");
    }
    
    /**
     * Detects conflicts for a merge request against specified target branches.
     *
     * @param repositoryPath path to the Git repository
     * @param sourceBranch source branch of the merge request
     * @param targetBranches list of target branches to check against
     * @return list of merge results for each target branch
     * @throws ConflictDetectionException if detection fails
     */
    public List<MergeResult> detectConflicts(Path repositoryPath, String sourceBranch, 
                                           List<String> targetBranches) throws ConflictDetectionException {
        log.info("Starting conflict detection for source branch '{}' against targets: {}",
                   sourceBranch, targetBranches);
        
        try {
            validateRepository(repositoryPath);
            gitRepository.openRepository(repositoryPath);
            
            validateBranches(sourceBranch, targetBranches);
            
            if (config.shouldFetchBeforeDetection()) {
                log.info("Fetching latest changes from remote");
                gitRepository.fetch();
            }
            
            return targetBranches.stream()
                    .map(targetBranch -> detectConflictsForTarget(sourceBranch, targetBranch))
                    .toList();
                    
        } catch (GitOperationException e) {
            throw new ConflictDetectionException("Git operation failed during conflict detection", e);
        } finally {
            gitRepository.close();
        }
    }
    
    /**
     * Detects conflicts between a source and target branch.
     *
     * @param sourceBranch source branch
     * @param targetBranch target branch
     * @return merge result with conflict information
     */
    private MergeResult detectConflictsForTarget(String sourceBranch, String targetBranch) {
        log.debug("Detecting conflicts between '{}' and '{}'", sourceBranch, targetBranch);
        
        try {
            MergeResult result = gitRepository.detectConflicts(sourceBranch, targetBranch);
            
            if (config.isForcePassEnabled()) {
                log.info("Force pass is enabled - overriding conflict detection result");
                return createForcePassResult(result);
            }
            
            logConflictSummary(result);
            return result;
            
        } catch (GitOperationException e) {
            log.error("Failed to detect conflicts between '{}' and '{}'", sourceBranch, targetBranch, e);
            return createFailedResult(sourceBranch, targetBranch, e.getMessage());
        }
    }
    
    /**
     * Validates that the repository path is valid.
     */
    private void validateRepository(Path repositoryPath) throws ConflictDetectionException {
        if (!gitRepository.isValidRepository(repositoryPath)) {
            throw new ConflictDetectionException("Invalid Git repository: " + repositoryPath);
        }
        
        log.debug("Repository validation passed: {}", repositoryPath);
    }
    
    /**
     * Validates that all specified branches exist.
     */
    private void validateBranches(String sourceBranch, List<String> targetBranches) 
            throws ConflictDetectionException {
        try {
            if (!gitRepository.branchExists(sourceBranch)) {
                throw new ConflictDetectionException("Source branch does not exist: " + sourceBranch);
            }
            
            for (String targetBranch : targetBranches) {
                if (!gitRepository.branchExists(targetBranch)) {
                    throw new ConflictDetectionException("Target branch does not exist: " + targetBranch);
                }
            }
            
            log.debug("Branch validation passed for source '{}' and targets: {}",
                        sourceBranch, targetBranches);
                        
        } catch (GitOperationException e) {
            throw new ConflictDetectionException("Failed to validate branches", e);
        }
    }
    
    /**
     * Creates a force pass result that overrides the actual conflict detection.
     */
    private MergeResult createForcePassResult(MergeResult originalResult) {
        return new MergeResult(
                originalResult.getSourceBranch(),
                originalResult.getTargetBranch(),
                originalResult.getSourceCommit(),
                originalResult.getTargetCommit(),
                List.of(), // Empty conflicts list
                MergeStatus.CLEAN,
                "Force pass enabled - conflicts ignored"
        );
    }
    
    /**
     * Creates a failed result when conflict detection fails.
     */
    private MergeResult createFailedResult(String sourceBranch, String targetBranch, String errorMessage) {
        return new MergeResult(
                sourceBranch,
                targetBranch,
                null,
                null,
                List.of(),
                MergeStatus.FAILED,
                "Conflict detection failed: " + errorMessage
        );
    }
    
    /**
     * Logs a summary of the conflict detection result.
     */
    private void logConflictSummary(MergeResult result) {
        if (result.hasConflicts()) {
            log.warn("Conflicts detected between '{}' and '{}': {} conflicts in {} files",
                       result.getSourceBranch(), result.getTargetBranch(),
                       result.getTotalConflictSections(), result.getConflictCount());
        } else {
            log.info("No conflicts detected between '{}' and '{}'",
                       result.getSourceBranch(), result.getTargetBranch());
        }
    }
    
    /**
     * Checks if the repository has uncommitted changes that might affect detection.
     *
     * @return true if there are uncommitted changes
     * @throws ConflictDetectionException if check fails
     */
    public boolean hasUncommittedChanges(Path repositoryPath) throws ConflictDetectionException {
        try {
            gitRepository.openRepository(repositoryPath);
            return gitRepository.hasUncommittedChanges();
        } catch (GitOperationException e) {
            throw new ConflictDetectionException("Failed to check for uncommitted changes", e);
        } finally {
            gitRepository.close();
        }
    }
    
    /**
     * Gets the list of files that would be affected by merging the source branch into target branches.
     *
     * @param repositoryPath path to the Git repository
     * @param sourceBranch source branch
     * @param targetBranch target branch
     * @return list of affected file paths
     * @throws ConflictDetectionException if operation fails
     */
    public List<String> getAffectedFiles(Path repositoryPath, String sourceBranch, String targetBranch) 
            throws ConflictDetectionException {
        try {
            gitRepository.openRepository(repositoryPath);
            return gitRepository.getAffectedFiles(sourceBranch, targetBranch);
        } catch (GitOperationException e) {
            throw new ConflictDetectionException("Failed to get affected files", e);
        } finally {
            gitRepository.close();
        }
    }
}

