package art.galushko.gitlab.mrconflict.gitlab;

import lombok.extern.slf4j.Slf4j;
import org.gitlab4j.api.models.Branch;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages branch operations and prioritization for conflict detection.
 */
@Slf4j
public class BranchManager {
    private final GitLabClient gitLabClient;
    
    public BranchManager(GitLabClient gitLabClient) {
        this.gitLabClient = Objects.requireNonNull(gitLabClient, "GitLab client cannot be null");
    }
    
    /**
     * Gets target branches for conflict detection, prioritizing protected branches.
     *
     * @param projectId GitLab project ID
     * @param specifiedTargets user-specified target branches (can be null/empty)
     * @return ordered list of target branches with protected branches first
     * @throws GitLabException if branch information cannot be retrieved
     */
    public List<String> getTargetBranches(Long projectId, List<String> specifiedTargets) 
            throws GitLabException {
        
        List<String> protectedBranches = gitLabClient.getProtectedBranches(projectId);
        log.debug("Found {} protected branches for project {}", protectedBranches.size(), projectId);
        
        if (specifiedTargets == null || specifiedTargets.isEmpty()) {
            // If no targets specified, use protected branches and default branch
            Set<String> targets = new LinkedHashSet<>(protectedBranches);
            
            String defaultBranch = gitLabClient.getDefaultBranch(projectId);
            if (defaultBranch != null) {
                targets.add(defaultBranch);
            }
            
            List<String> result = new ArrayList<>(targets);
            log.info("Using auto-detected target branches: {}", result);
            return result;
        }
        
        // Validate specified targets exist
        validateBranchesExist(projectId, specifiedTargets);
        
        // Prioritize protected branches
        return prioritizeProtectedBranches(specifiedTargets, protectedBranches);
    }
    
    /**
     * Prioritizes protected branches in the target list.
     */
    private List<String> prioritizeProtectedBranches(List<String> targets, List<String> protectedBranches) {
        Set<String> protectedSet = new HashSet<>(protectedBranches);
        
        List<String> prioritized = new ArrayList<>();
        
        // Add protected branches first
        targets.stream()
                .filter(protectedSet::contains)
                .forEach(prioritized::add);
        
        // Add non-protected branches
        targets.stream()
                .filter(branch -> !protectedSet.contains(branch))
                .forEach(prioritized::add);
        
        log.debug("Prioritized branches: protected={}, non-protected={}",
                    prioritized.stream().filter(protectedSet::contains).count(),
                    prioritized.stream().filter(branch -> !protectedSet.contains(branch)).count());
        
        return prioritized;
    }
    
    /**
     * Validates that all specified branches exist in the project.
     */
    private void validateBranchesExist(Long projectId, List<String> branchNames) throws GitLabException {
        List<String> nonExistentBranches = new ArrayList<>();
        
        for (String branchName : branchNames) {
            Optional<Branch> branch = gitLabClient.getBranch(projectId, branchName);
            if (branch.isEmpty()) {
                nonExistentBranches.add(branchName);
            }
        }
        
        if (!nonExistentBranches.isEmpty()) {
            throw new GitLabException("The following branches do not exist: " + 
                                    String.join(", ", nonExistentBranches));
        }
        
        log.debug("All specified branches exist: {}", branchNames);
    }
    
    /**
     * Gets branch information for a specific branch.
     *
     * @param projectId GitLab project ID
     * @param branchName name of the branch
     * @return branch information
     * @throws GitLabException if branch doesn't exist or cannot be retrieved
     */
    public Branch getBranchInfo(Long projectId, String branchName) throws GitLabException {
        Optional<Branch> branch = gitLabClient.getBranch(projectId, branchName);
        return branch.orElseThrow(() -> new GitLabException("Branch not found: " + branchName));
    }
    
    /**
     * Checks if a branch is protected.
     *
     * @param projectId GitLab project ID
     * @param branchName name of the branch to check
     * @return true if branch is protected
     * @throws GitLabException if protection status cannot be determined
     */
    public boolean isBranchProtected(Long projectId, String branchName) throws GitLabException {
        return gitLabClient.isBranchProtected(projectId, branchName);
    }
    
    /**
     * Gets all available branches for a project.
     *
     * @param projectId GitLab project ID
     * @return list of all branch names
     * @throws GitLabException if branches cannot be retrieved
     */
    public List<String> getAllBranches(Long projectId) throws GitLabException {
        List<Branch> branches = gitLabClient.getBranches(projectId);
        return branches.stream()
                .map(Branch::getName)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets the default branch for a project.
     *
     * @param projectId GitLab project ID
     * @return default branch name
     * @throws GitLabException if default branch cannot be determined
     */
    public String getDefaultBranch(Long projectId) throws GitLabException {
        return gitLabClient.getDefaultBranch(projectId);
    }
    
    /**
     * Filters branches based on patterns (for future enhancement).
     *
     * @param branches list of branch names
     * @param includePatterns patterns to include (glob style)
     * @param excludePatterns patterns to exclude (glob style)
     * @return filtered list of branches
     */
    public List<String> filterBranches(List<String> branches, List<String> includePatterns, 
                                     List<String> excludePatterns) {
        // This is a placeholder for future pattern matching implementation
        // For now, return all branches
        log.debug("Branch filtering not yet implemented, returning all {} branches", branches.size());
        return new ArrayList<>(branches);
    }
}

