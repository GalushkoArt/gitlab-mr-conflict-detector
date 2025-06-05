package art.galushko.gitlab.mrconflict.core;

import art.galushko.gitlab.mrconflict.config.IgnorePatternMatcher;
import art.galushko.gitlab.mrconflict.model.MergeRequestInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MultiMergeRequestConflictDetector.
 */
class MultiMergeRequestConflictDetectorTest {

    private MultiMergeRequestConflictDetector detector;

    @BeforeEach
    void setUp() {
        detector = new MultiMergeRequestConflictDetector(new IgnorePatternMatcher());
    }

    @Test
    @DisplayName("Should detect direct conflicts between MRs targeting same branch")
    void shouldDetectDirectConflicts() {
        // Given
        var mr1 = MergeRequestInfo.builder()
                .id(1)
                .sourceBranch("feature-auth")
                .targetBranch("main")
                .changedFiles(List.of("src/app.js", "tests/unit.test.js"))
                .build();

        var mr2 = MergeRequestInfo.builder()
                .id(2)
                .sourceBranch("feature-ui")
                .targetBranch("main")
                .changedFiles(List.of("src/app.js", "makefile"))
                .build();

        var mergeRequests = List.of(mr1, mr2);
        var ignorePatterns = List.of("temp/", "makefile");

        // When
        var conflicts = detector.detectConflicts(mergeRequests, ignorePatterns);

        // Then
        assertThat(conflicts).hasSize(1);
        var conflict = conflicts.get(0);
        assertThat(conflict.firstMr().id()).isEqualTo(1);
        assertThat(conflict.secondMr().id()).isEqualTo(2);
        assertThat(conflict.conflictingFiles()).containsExactly("src/app.js");
    }

    @Test
    @DisplayName("Should not detect conflicts when MRs have dependency relationship")
    void shouldNotDetectConflictsForDependencyRelationship() {
        // Given
        var mr1 = MergeRequestInfo.builder()
                .id(1)
                .sourceBranch("feature-auth")
                .targetBranch("main")
                .changedFiles(List.of("tests/unit.test.js"))
                .build();

        var mr3 = MergeRequestInfo.builder()
                .id(3)
                .sourceBranch("hotfix")
                .targetBranch("feature-auth")
                .changedFiles(List.of("tests/unit.test.js"))
                .build();

        var mergeRequests = List.of(mr1, mr3);
        var ignorePatterns = List.<String>of();

        // When
        var conflicts = detector.detectConflicts(mergeRequests, ignorePatterns);

        // Then
        assertThat(conflicts).isEmpty();
    }

    @Test
    @DisplayName("Should ignore conflicts in files matching ignore patterns")
    void shouldIgnoreConflictsInIgnoredFiles() {
        // Given
        var mr2 = MergeRequestInfo.builder()
                .id(2)
                .sourceBranch("feature-ui")
                .targetBranch("main")
                .changedFiles(List.of("makefile"))
                .build();

        var mr7 = MergeRequestInfo.builder()
                .id(7)
                .sourceBranch("ignore-update")
                .targetBranch("main")
                .changedFiles(List.of("makefile"))
                .build();

        var mergeRequests = List.of(mr2, mr7);
        var ignorePatterns = List.of("makefile");

        // When
        var conflicts = detector.detectConflicts(mergeRequests, ignorePatterns);

        // Then
        assertThat(conflicts).isEmpty();
    }

    @Test
    @DisplayName("Should detect cross-branch conflicts")
    void shouldDetectCrossBranchConflicts() {
        // Given
        var mr5 = MergeRequestInfo.builder()
                .id(5)
                .sourceBranch("new-values")
                .targetBranch("feature-auth")
                .changedFiles(List.of("src/consts.js"))
                .build();

        var mr6 = MergeRequestInfo.builder()
                .id(6)
                .sourceBranch("const-update")
                .targetBranch("main")
                .changedFiles(List.of("src/consts.js"))
                .build();

        var mergeRequests = List.of(mr5, mr6);
        var ignorePatterns = List.<String>of();

        // When
        var conflicts = detector.detectConflicts(mergeRequests, ignorePatterns);

        // Then
        assertThat(conflicts).hasSize(1);
        var conflict = conflicts.get(0);
        assertThat(conflict.firstMr().id()).isEqualTo(5);
        assertThat(conflict.secondMr().id()).isEqualTo(6);
        assertThat(conflict.conflictingFiles()).containsExactly("src/consts.js");
    }

    @Test
    @DisplayName("Should format conflicts correctly")
    void shouldFormatConflictsCorrectly() {
        // Given
        var mr1 = MergeRequestInfo.builder()
                .id(1)
                .title("MR1")
                .sourceBranch("feature-auth")
                .targetBranch("main")
                .changedFiles(List.of("src/app.js"))
                .build();

        var mr2 = MergeRequestInfo.builder()
                .id(2)
                .title("MR2")
                .sourceBranch("feature-ui")
                .targetBranch("main")
                .changedFiles(List.of("src/app.js"))
                .build();

        var mergeRequests = List.of(mr1, mr2);
        var conflicts = detector.detectConflicts(mergeRequests, List.<String>of());

        // When
        var formatted = detector.formatConflicts(conflicts);

        // Then
        assertThat(formatted).isEqualToNormalizingNewlines("""
                "MR1" vs "MR2"
                - Issue: conflict in modification of `src/app.js`""");
    }

    @Test
    @DisplayName("Should return conflicting MR IDs correctly")
    void shouldReturnConflictingMrIds() {
        // Given
        var mr1 = MergeRequestInfo.builder()
                .id(1)
                .sourceBranch("feature-auth")
                .targetBranch("main")
                .changedFiles(List.of("src/app.js"))
                .build();

        var mr2 = MergeRequestInfo.builder()
                .id(2)
                .sourceBranch("feature-ui")
                .targetBranch("main")
                .changedFiles(List.of("src/app.js"))
                .build();

        var mr3 = MergeRequestInfo.builder()
                .id(3)
                .sourceBranch("feature-other")
                .targetBranch("main")
                .changedFiles(List.of("other.js"))
                .build();

        var mergeRequests = List.of(mr1, mr2, mr3);
        var conflicts = detector.detectConflicts(mergeRequests, List.<String>of());

        // When
        var conflictingIds = detector.getConflictingMergeRequestIds(conflicts);

        // Then
        assertThat(conflictingIds).containsExactlyInAnyOrder(1, 2);
        assertThat(conflictingIds).doesNotContain(3);
    }
}

