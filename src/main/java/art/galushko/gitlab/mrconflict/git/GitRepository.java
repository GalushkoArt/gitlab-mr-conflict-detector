package art.galushko.gitlab.mrconflict.git;

import art.galushko.gitlab.mrconflict.model.MergeResult;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Interface for Git repository operations needed for conflict detection.
 */
public interface GitRepository {
    
    /**
     * Checks if the given path is a valid Git repository.
     *
     * @param repositoryPath path to check
     * @return true if it's a valid Git repository
     */
    boolean isValidRepository(Path repositoryPath);
    
    /**
     * Opens a Git repository at the specified path.
     *
     * @param repositoryPath path to the repository
     * @throws GitOperationException if repository cannot be opened
     */
    void openRepository(Path repositoryPath) throws GitOperationException;
    
    /**
     * Gets the current branch name.
     *
     * @return current branch name
     * @throws GitOperationException if operation fails
     */
    String getCurrentBranch() throws GitOperationException;
    
    /**
     * Gets all local branches.
     *
     * @return list of local branch names
     * @throws GitOperationException if operation fails
     */
    List<String> getLocalBranches() throws GitOperationException;
    
    /**
     * Gets all remote branches.
     *
     * @return list of remote branch names
     * @throws GitOperationException if operation fails
     */
    List<String> getRemoteBranches() throws GitOperationException;
    
    /**
     * Checks if a branch exists (local or remote).
     *
     * @param branchName name of the branch to check
     * @return true if branch exists
     * @throws GitOperationException if operation fails
     */
    boolean branchExists(String branchName) throws GitOperationException;
    
    /**
     * Gets the commit hash for a given branch or commit reference.
     *
     * @param ref branch name or commit reference
     * @return commit hash
     * @throws GitOperationException if reference doesn't exist or operation fails
     */
    String getCommitHash(String ref) throws GitOperationException;
    
    /**
     * Fetches latest changes from remote repository.
     *
     * @throws GitOperationException if fetch operation fails
     */
    void fetch() throws GitOperationException;
    
    /**
     * Performs a dry-run merge to detect conflicts without actually merging.
     *
     * @param sourceBranch source branch to merge from
     * @param targetBranch target branch to merge into
     * @return merge result with conflict information
     * @throws GitOperationException if merge operation fails
     */
    MergeResult detectConflicts(String sourceBranch, String targetBranch) throws GitOperationException;
    
    /**
     * Performs a three-way merge analysis to detect conflicts.
     *
     * @param sourceCommit source commit hash
     * @param targetCommit target commit hash
     * @param baseCommit common base commit hash
     * @return merge result with conflict information
     * @throws GitOperationException if merge analysis fails
     */
    MergeResult analyzeThreeWayMerge(String sourceCommit, String targetCommit, String baseCommit) 
            throws GitOperationException;
    
    /**
     * Finds the merge base (common ancestor) between two commits.
     *
     * @param commit1 first commit
     * @param commit2 second commit
     * @return merge base commit hash if found
     * @throws GitOperationException if operation fails
     */
    Optional<String> findMergeBase(String commit1, String commit2) throws GitOperationException;
    
    /**
     * Gets the list of files that would be affected by a merge.
     *
     * @param sourceBranch source branch
     * @param targetBranch target branch
     * @return list of file paths that would be affected
     * @throws GitOperationException if operation fails
     */
    List<String> getAffectedFiles(String sourceBranch, String targetBranch) throws GitOperationException;
    
    /**
     * Checks if the repository has any uncommitted changes.
     *
     * @return true if there are uncommitted changes
     * @throws GitOperationException if operation fails
     */
    boolean hasUncommittedChanges() throws GitOperationException;
    
    /**
     * Creates a temporary branch for conflict detection operations.
     *
     * @param baseBranch base branch to create from
     * @return name of the created temporary branch
     * @throws GitOperationException if branch creation fails
     */
    String createTemporaryBranch(String baseBranch) throws GitOperationException;
    
    /**
     * Deletes a temporary branch.
     *
     * @param branchName name of the branch to delete
     * @throws GitOperationException if branch deletion fails
     */
    void deleteTemporaryBranch(String branchName) throws GitOperationException;
    
    /**
     * Closes the repository and releases resources.
     */
    void close();
}

