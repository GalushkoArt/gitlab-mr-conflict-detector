package art.galushko.gitlab.mrconflict.core;

import art.galushko.gitlab.mrconflict.config.IgnorePatternMatcher;
import art.galushko.gitlab.mrconflict.model.MergeRequestConflict;
import art.galushko.gitlab.mrconflict.model.MergeRequestInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for MultiMergeRequestConflictDetector with a variety of merge request configurations.
 * These tests simulate property-based testing by using a variety of inputs.
 */
class MultiMergeRequestConflictDetectorVarietyTest {

    private final IgnorePatternMatcher ignorePatternMatcher = new IgnorePatternMatcher();
    private final ConflictDetector detector = new MultiMergeRequestConflictDetector(ignorePatternMatcher);

    @ParameterizedTest
    @MethodSource("mergeRequestPairsProvider")
    @DisplayName("Should detect conflicts correctly for various merge request pairs")
    void shouldDetectConflictsCorrectlyForVariousMergeRequestPairs(
            MergeRequestInfo mr1, MergeRequestInfo mr2, boolean shouldHaveConflict) {
        // Given
        List<MergeRequestInfo> mergeRequests = Arrays.asList(mr1, mr2);
        List<String> ignorePatterns = List.of();

        // When
        List<MergeRequestConflict> conflicts = detector.detectConflicts(mergeRequests, ignorePatterns);

        // Then
        if (shouldHaveConflict) {
            assertThat(conflicts).hasSize(1);
            MergeRequestConflict conflict = conflicts.getFirst();
            assertThat(conflict.firstMr()).isEqualTo(mr1);
            assertThat(conflict.secondMr()).isEqualTo(mr2);
        } else {
            assertThat(conflicts).isEmpty();
        }
    }

    static Stream<Arguments> mergeRequestPairsProvider() {
        return Stream.of(
                // Direct conflict - same target branch, common files
                Arguments.of(
                        createMergeRequest(1, "feature-1", "main", "file1.txt", "file2.txt"),
                        createMergeRequest(2, "feature-2", "main", "file1.txt", "file3.txt"),
                        true
                ),
                // No conflict - same target branch, no common files
                Arguments.of(
                        createMergeRequest(1, "feature-1", "main", "file1.txt", "file2.txt"),
                        createMergeRequest(2, "feature-2", "main", "file3.txt", "file4.txt"),
                        false
                ),
                // No conflict - dependency relationship (mr2 source is mr1 target)
                Arguments.of(
                        createMergeRequest(1, "feature-1", "main", "file1.txt"),
                        createMergeRequest(2, "main", "release", "file1.txt"),
                        false
                ),
                // No conflict - dependency relationship (mr1 source is mr2 target)
                Arguments.of(
                        createMergeRequest(1, "develop", "feature-1", "file1.txt"),
                        createMergeRequest(2, "feature-1", "main", "file1.txt"),
                        false
                ),
                // Cross-branch conflict - different target branches, common files
                Arguments.of(
                        createMergeRequest(1, "feature-1", "develop", "file1.txt"),
                        createMergeRequest(2, "feature-2", "main", "file1.txt"),
                        true
                ),
                // Edge case - empty file lists
                Arguments.of(
                        createMergeRequest(1, "feature-1", "main"),
                        createMergeRequest(2, "feature-2", "main"),
                        false
                )
        );
    }

    @Test
    @DisplayName("Should handle multiple merge requests correctly")
    void shouldHandleMultipleMergeRequestsCorrectly() {
        // Given
        List<MergeRequestInfo> mergeRequests = Arrays.asList(
                createMergeRequest(1, "feature-1", "main", "file1.txt", "file2.txt"),
                createMergeRequest(2, "feature-2", "main", "file1.txt", "file3.txt"),
                createMergeRequest(3, "feature-3", "main", "file4.txt", "file5.txt"),
                createMergeRequest(4, "feature-4", "develop", "file1.txt", "file6.txt")
        );
        List<String> ignorePatterns = List.of();

        // When
        List<MergeRequestConflict> conflicts = detector.detectConflicts(mergeRequests, ignorePatterns);

        // Then
        // Expected conflicts: (1,2), (1,4), (2,4)
        assertThat(conflicts).hasSize(3);

        // Verify conflicting MR IDs
        Set<Long> conflictingIds = detector.getConflictingMergeRequestIds(conflicts);
        assertThat(conflictingIds.contains(1L)).isTrue();
        assertThat(conflictingIds.contains(2L)).isTrue();
        assertThat(conflictingIds.contains(4L)).isTrue();
        assertThat(conflictingIds.contains(3L)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("ignorePatternProvider")
    @DisplayName("Should respect ignore patterns")
    void shouldRespectIgnorePatterns(List<String> ignorePatterns, boolean shouldHaveConflict) {
        // Given
        MergeRequestInfo mr1 = createMergeRequest(1, "feature-1", "main", "src/file1.txt", "temp/file2.txt");
        MergeRequestInfo mr2 = createMergeRequest(2, "feature-2", "main", "src/file1.txt", "docs/file3.txt");
        List<MergeRequestInfo> mergeRequests = Arrays.asList(mr1, mr2);

        // When
        List<MergeRequestConflict> conflicts = detector.detectConflicts(mergeRequests, ignorePatterns);

        // Then
        if (shouldHaveConflict) {
            assertThat(conflicts).hasSize(1);
        } else {
            assertThat(conflicts).isEmpty();
        }
    }

    static Stream<Arguments> ignorePatternProvider() {
        return Stream.of(
                // No ignore patterns - should have conflict
                Arguments.of(List.of(), true),
                // Ignore the conflicting file - should not have conflict
                Arguments.of(List.of("src/file1.txt"), false),
                // Ignore a directory containing the conflicting file - should not have conflict
                Arguments.of(List.of("src/"), false),
                // Ignore a pattern matching the conflicting file - should not have conflict
                Arguments.of(List.of("src/*.txt"), false),
                // Ignore a non-matching pattern - should have conflict
                Arguments.of(List.of("other/*.txt"), true),
                // Multiple patterns including one that matches - should not have conflict
                Arguments.of(List.of("other/*.txt", "src/*.txt"), false)
        );
    }

    @Test
    @DisplayName("Should handle empty merge request list")
    void shouldHandleEmptyMergeRequestList() {
        // Given
        List<MergeRequestInfo> mergeRequests = new ArrayList<>();
        List<String> ignorePatterns = List.of();

        // When
        List<MergeRequestConflict> conflicts = detector.detectConflicts(mergeRequests, ignorePatterns);

        // Then
        assertThat(conflicts).isEmpty();
    }

    @Test
    @DisplayName("Should handle single merge request")
    void shouldHandleSingleMergeRequest() {
        // Given
        List<MergeRequestInfo> mergeRequests = List.of(
                createMergeRequest(1, "feature-1", "main", "file1.txt", "file2.txt")
        );
        List<String> ignorePatterns = List.of();

        // When
        List<MergeRequestConflict> conflicts = detector.detectConflicts(mergeRequests, ignorePatterns);

        // Then
        assertThat(conflicts).isEmpty();
    }

    /**
     * Helper method to create a merge request with the given parameters.
     */
    private static MergeRequestInfo createMergeRequest(int id, String sourceBranch, String targetBranch, String... changedFiles) {
        return MergeRequestInfo.builder()
                .id(id)
                .title("MR" + id)
                .sourceBranch(sourceBranch)
                .targetBranch(targetBranch)
                .changedFiles(new HashSet<>(Arrays.asList(changedFiles)))
                .build();
    }
}
