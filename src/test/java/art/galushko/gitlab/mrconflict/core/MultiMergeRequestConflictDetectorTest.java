package art.galushko.gitlab.mrconflict.core;

import art.galushko.gitlab.mrconflict.di.ServiceFactory;
import art.galushko.gitlab.mrconflict.formatter.ConflictFormatter;
import art.galushko.gitlab.mrconflict.model.MergeRequestInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MultiMergeRequestConflictDetector.
 */
class MultiMergeRequestConflictDetectorTest {

    private final ConflictDetector detector = ServiceFactory.getInstance().getConflictDetector();
    private final ConflictFormatter formatter = ServiceFactory.getInstance().getConflictFormatter();

    @Test
    @DisplayName("Should detect direct conflicts between MRs targeting same branch")
    void shouldDetectDirectConflicts() {
        // Given
        var mr1 = MergeRequestInfo.builder()
                .id(1)
                .sourceBranch("feature-auth")
                .targetBranch("main")
                .changedFiles(Set.of("src/app.js", "tests/unit.test.js"))
                .build();

        var mr2 = MergeRequestInfo.builder()
                .id(2)
                .sourceBranch("feature-ui")
                .targetBranch("main")
                .changedFiles(Set.of("src/app.js", "makefile"))
                .build();

        var mergeRequests = List.of(mr1, mr2);
        var ignorePatterns = List.of("temp/", "makefile");

        // When
        var conflicts = detector.detectConflicts(mergeRequests, ignorePatterns);

        // Then
        assertThat(conflicts).hasSize(1);
        var conflict = conflicts.getFirst();
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
                .changedFiles(Set.of("tests/unit.test.js"))
                .build();

        var mr3 = MergeRequestInfo.builder()
                .id(3)
                .sourceBranch("hotfix")
                .targetBranch("feature-auth")
                .changedFiles(Set.of("tests/unit.test.js"))
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
                .changedFiles(Set.of("makefile"))
                .build();

        var mr7 = MergeRequestInfo.builder()
                .id(7)
                .sourceBranch("ignore-update")
                .targetBranch("main")
                .changedFiles(Set.of("makefile"))
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
                .changedFiles(Set.of("src/consts.js"))
                .build();

        var mr6 = MergeRequestInfo.builder()
                .id(6)
                .sourceBranch("const-update")
                .targetBranch("main")
                .changedFiles(Set.of("src/consts.js"))
                .build();

        var mergeRequests = List.of(mr5, mr6);
        var ignorePatterns = List.<String>of();

        // When
        var conflicts = detector.detectConflicts(mergeRequests, ignorePatterns);

        // Then
        assertThat(conflicts).hasSize(1);
        var conflict = conflicts.getFirst();
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
                .changedFiles(Set.of("src/app.js"))
                .build();

        var mr2 = MergeRequestInfo.builder()
                .id(2)
                .title("MR2")
                .sourceBranch("feature-ui")
                .targetBranch("main")
                .changedFiles(Set.of("src/app.js"))
                .build();

        var mergeRequests = List.of(mr1, mr2);
        var conflicts = detector.detectConflicts(mergeRequests, List.<String>of());

        // When
        var formatted = formatter.formatConflicts(conflicts);

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
                .changedFiles(Set.of("src/app.js"))
                .build();

        var mr2 = MergeRequestInfo.builder()
                .id(2)
                .sourceBranch("feature-ui")
                .targetBranch("main")
                .changedFiles(Set.of("src/app.js"))
                .build();

        var mr3 = MergeRequestInfo.builder()
                .id(3)
                .sourceBranch("feature-other")
                .targetBranch("main")
                .changedFiles(Set.of("other.js"))
                .build();

        var mergeRequests = List.of(mr1, mr2, mr3);
        var conflicts = detector.detectConflicts(mergeRequests, List.<String>of());

        // When
        var conflictingIds = detector.getConflictingMergeRequestIds(conflicts);

        // Then
        assertThat(conflictingIds).containsExactlyInAnyOrder(1L, 2L);
        assertThat(conflictingIds).doesNotContain(3L);
    }
}
