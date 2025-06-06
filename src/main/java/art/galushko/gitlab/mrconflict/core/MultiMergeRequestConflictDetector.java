package art.galushko.gitlab.mrconflict.core;

import art.galushko.gitlab.mrconflict.config.PatternMatcher;
import art.galushko.gitlab.mrconflict.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for detecting conflicts between multiple merge requests.
 */
@Slf4j
@RequiredArgsConstructor
public class MultiMergeRequestConflictDetector implements ConflictDetector {

    private final PatternMatcher ignorePatternMatcher;

    /**
     * Detects conflicts between multiple merge requests.
     *
     * @param mergeRequests list of merge requests to analyze
     * @param ignorePatterns patterns for files/directories to ignore
     * @return list of detected conflicts
     */
    public List<MergeRequestConflict> detectConflicts(List<MergeRequestInfo> mergeRequests,
                                                      List<String> ignorePatterns) {
        log.info("Detecting conflicts between {} merge requests", mergeRequests.size());

        List<MergeRequestConflict> conflicts = new ArrayList<>();

        // Check all pairs of merge requests
        for (int i = 0; i < mergeRequests.size(); i++) {
            for (int j = i + 1; j < mergeRequests.size(); j++) {
                MergeRequestInfo mr1 = mergeRequests.get(i);
                MergeRequestInfo mr2 = mergeRequests.get(j);

                Optional<MergeRequestConflict> conflict = detectConflictBetween(mr1, mr2, ignorePatterns);
                conflict.ifPresent(conflicts::add);
            }
        }

        log.info("Detected {} conflicts", conflicts.size());
        return conflicts;
    }

    /**
     * Detects conflict between two specific merge requests.
     *
     * @param mr1 first merge request
     * @param mr2 second merge request
     * @param ignorePatterns patterns for files/directories to ignore
     * @return optional conflict if detected
     */
    private Optional<MergeRequestConflict> detectConflictBetween(MergeRequestInfo mr1,
                                                                 MergeRequestInfo mr2,
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
     * Formats the conflicts into the required output format.
     *
     * @param conflicts list of detected conflicts
     * @return formatted output string
     */
    public String formatConflicts(List<MergeRequestConflict> conflicts) {
        if (conflicts.isEmpty()) {
            return "No conflicts detected.";
        }

        return conflicts.stream()
                .map(MergeRequestConflict::getFormattedOutput)
                .collect(Collectors.joining("\n"));
    }

    /**
     * Gets the list of merge request IDs that have conflicts.
     *
     * @param conflicts list of detected conflicts
     * @return set of MR IDs that have conflicts
     */
    public Set<Integer> getConflictingMergeRequestIds(List<MergeRequestConflict> conflicts) {
        return conflicts.stream()
                .flatMap(conflict -> Stream.of(conflict.firstMr().id(), conflict.secondMr().id()))
                .collect(Collectors.toSet());
    }
}
