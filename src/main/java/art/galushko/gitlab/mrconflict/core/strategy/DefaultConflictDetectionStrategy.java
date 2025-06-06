package art.galushko.gitlab.mrconflict.core.strategy;

import art.galushko.gitlab.mrconflict.config.PatternMatcher;
import art.galushko.gitlab.mrconflict.model.ConflictReason;
import art.galushko.gitlab.mrconflict.model.MergeRequestConflict;
import art.galushko.gitlab.mrconflict.model.MergeRequestInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation of the ConflictDetectionStrategy interface.
 * This strategy detects conflicts based on common files between merge requests.
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultConflictDetectionStrategy implements ConflictDetectionStrategy {

    private final PatternMatcher ignorePatternMatcher;

    @Override
    public Optional<MergeRequestConflict> detectConflict(MergeRequestInfo mr1, MergeRequestInfo mr2,
                                                        List<String> ignorePatterns) {
        log.debug("Checking conflict between MR{} and MR{}", mr1.id(), mr2.id());

        // Early termination: Check if MRs have dependency relationship (indirect conflict rule)
        if (hasDependencyRelationship(mr1, mr2)) {
            log.debug("MR{} and MR{} have dependency relationship - no conflict", mr1.id(), mr2.id());
            return Optional.empty();
        }

        // Early termination: Check if either MR has no changed files
        if (mr1.changedFiles().isEmpty() || mr2.changedFiles().isEmpty()) {
            log.debug("Either MR{} or MR{} has no changed files - no conflict", mr1.id(), mr2.id());
            return Optional.empty();
        }

        // Early termination: Check if MRs target completely different parts of the codebase
        // This is a heuristic check that can be refined based on project structure
        if (!hasOverlappingCodePaths(mr1, mr2)) {
            log.debug("MR{} and MR{} target different parts of the codebase - no conflict", mr1.id(), mr2.id());
            return Optional.empty();
        }

        // Fast check for common files using the optimized method
        if (!mr1.hasCommonFiles(mr2)) {
            log.debug("No common files between MR{} and MR{}", mr1.id(), mr2.id());
            return Optional.empty();
        }

        // If we get here, we know there are common files, so get the full list
        List<String> commonFiles = mr1.getCommonFiles(mr2);

        // Filter out ignored files
        Set<String> conflictingFiles = commonFiles.stream()
                .filter(file -> !isFileIgnored(file, ignorePatterns))
                .collect(Collectors.toSet());

        if (conflictingFiles.isEmpty()) {
            log.debug("All common files between MR{} and MR{} are ignored", mr1.id(), mr2.id());
            return Optional.empty();
        }

        // Determine conflict reason
        ConflictReason reason = determineConflictReason(mr1, mr2);

        log.debug("Conflict detected between MR{} and MR{}: {} files",
                mr1.id(), mr2.id(), conflictingFiles.size());

        return Optional.of(MergeRequestConflict.builder()
                .firstMr(mr1)
                .secondMr(mr2)
                .conflictingFiles(conflictingFiles)
                .reason(reason)
                .build());
    }

    @Override
    public String getStrategyName() {
        return "Default";
    }

    /**
     * Checks if two merge requests have a dependency relationship.
     * According to the requirements: "MR1 and MR3 is not detected because
     * source branch of MR3 will be merged into source branch of MR1"
     *
     * @param mr1 first merge request
     * @param mr2 second merge request
     * @return true if they have dependency relationship
     */
    private boolean hasDependencyRelationship(MergeRequestInfo mr1, MergeRequestInfo mr2) {
        // Check if one MR's source branch is the target of another
        // This indicates a dependency chain where one MR will be merged into another
        return mr1.targetBranch().equals(mr2.sourceBranch()) ||
                mr2.targetBranch().equals(mr1.sourceBranch());
    }

    /**
     * Determines the reason for the conflict between two merge requests.
     *
     * @param mr1 first merge request
     * @param mr2 second merge request
     * @return conflict reason
     */
    private ConflictReason determineConflictReason(MergeRequestInfo mr1, MergeRequestInfo mr2) {
        // Direct conflict: both target the same branch
        if (mr1.targetBranch().equals(mr2.targetBranch())) {
            return ConflictReason.DIRECT_CONFLICT;
        }

        // For now, treat all other conflicts as cross-branch conflicts
        // In a more sophisticated implementation, we could analyze the branch graph
        // to determine if there are indirect conflicts through dependency chains
        return ConflictReason.CROSS_BRANCH_CONFLICT;
    }

    /**
     * Checks if a file should be ignored based on the ignore patterns.
     *
     * @param filePath the file path to check
     * @param ignorePatterns list of ignore patterns
     * @return true if the file should be ignored
     */
    private boolean isFileIgnored(String filePath, List<String> ignorePatterns) {
        if (ignorePatterns == null || ignorePatterns.isEmpty()) {
            return false;
        }

        return ignorePatterns.stream()
                .anyMatch(pattern -> ignorePatternMatcher.matches(pattern, filePath));
    }


    /**
     * Checks if two merge requests have overlapping code paths.
     * This is a heuristic method that can be used for early termination
     * when merge requests clearly target different parts of the codebase.
     *
     * @param mr1 first merge request
     * @param mr2 second merge request
     * @return true if the merge requests have overlapping code paths
     */
    private boolean hasOverlappingCodePaths(MergeRequestInfo mr1, MergeRequestInfo mr2) {
        // If either MR has no files, they can't overlap
        if (mr1.changedFiles().isEmpty() || mr2.changedFiles().isEmpty()) {
            return false;
        }

        // Extract top-level directories from file paths
        Set<String> mr1TopDirs = getTopLevelDirectories(mr1.changedFiles());
        Set<String> mr2TopDirs = getTopLevelDirectories(mr2.changedFiles());

        // Check if there's any overlap in top-level directories
        for (String dir : mr1TopDirs) {
            if (mr2TopDirs.contains(dir)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Extracts the top-level directories from a set of file paths.
     *
     * @param filePaths set of file paths
     * @return set of top-level directories
     */
    private Set<String> getTopLevelDirectories(Set<String> filePaths) {
        return filePaths.stream()
                .map(this::extractTopLevelDirectory)
                .filter(dir -> !dir.isEmpty())
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Extracts the top-level directory from a file path.
     *
     * @param filePath file path
     * @return top-level directory or empty string if none
     */
    private String extractTopLevelDirectory(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }

        // Normalize path separators
        String normalizedPath = filePath.replace('\\', '/');

        // Extract first directory component
        int firstSlash = normalizedPath.indexOf('/');
        if (firstSlash > 0) {
            return normalizedPath.substring(0, firstSlash);
        }

        // If no slash, return the whole path (could be a file in root)
        return normalizedPath;
    }
}
