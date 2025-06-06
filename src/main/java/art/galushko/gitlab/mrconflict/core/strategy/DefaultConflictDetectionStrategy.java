package art.galushko.gitlab.mrconflict.core.strategy;

import art.galushko.gitlab.mrconflict.config.PatternMatcher;
import art.galushko.gitlab.mrconflict.model.ConflictReason;
import art.galushko.gitlab.mrconflict.model.MergeRequestConflict;
import art.galushko.gitlab.mrconflict.model.MergeRequestInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

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

        // Check if MRs have dependency relationship (indirect conflict rule)
        if (hasDependencyRelationship(mr1, mr2)) {
            log.debug("MR{} and MR{} have dependency relationship - no conflict", mr1.id(), mr2.id());
            return Optional.empty();
        }

        // Find common files between the two MRs
        List<String> commonFiles = mr1.getCommonFiles(mr2);

        if (commonFiles.isEmpty()) {
            log.debug("No common files between MR{} and MR{}", mr1.id(), mr2.id());
            return Optional.empty();
        }

        // Filter out ignored files
        List<String> conflictingFiles = commonFiles.stream()
                .filter(file -> !isFileIgnored(file, ignorePatterns))
                .toList();

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
}