package art.galushko.gitlab.mrconflict.core.strategy;

import art.galushko.gitlab.mrconflict.config.PatternMatcher;
import art.galushko.gitlab.mrconflict.model.ConflictReason;
import art.galushko.gitlab.mrconflict.model.MergeRequestConflict;
import art.galushko.gitlab.mrconflict.model.MergeRequestInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DefaultConflictDetectionStrategy.
 */
@ExtendWith(MockitoExtension.class)
class DefaultConflictDetectionStrategyTest {

    @Mock
    private PatternMatcher patternMatcher;

    private DefaultConflictDetectionStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new DefaultConflictDetectionStrategy(patternMatcher);
    }

    @Test
    @DisplayName("Should return strategy name")
    void shouldReturnStrategyName() {
        assertThat(strategy.getStrategyName()).isEqualTo("Default");
    }

    @Test
    @DisplayName("Should detect direct conflict when MRs target same branch and have common files")
    void shouldDetectDirectConflictWhenMRsTargetSameBranchAndHaveCommonFiles() {
        // Given
        var mr1 = MergeRequestInfo.builder()
                .id(1)
                .sourceBranch("feature-1")
                .targetBranch("main")
                .changedFiles(Set.of("src/app.js", "src/utils.js"))
                .build();

        var mr2 = MergeRequestInfo.builder()
                .id(2)
                .sourceBranch("feature-2")
                .targetBranch("main")
                .changedFiles(Set.of("src/app.js", "src/config.js"))
                .build();

        var ignorePatterns = List.<String>of();

        // When
        var result = strategy.detectConflict(mr1, mr2, ignorePatterns);

        // Then
        assertThat(result).isPresent();
        var conflict = result.get();
        assertThat(conflict.firstMr()).isEqualTo(mr1);
        assertThat(conflict.secondMr()).isEqualTo(mr2);
        assertThat(conflict.conflictingFiles()).containsExactly("src/app.js");
        assertThat(conflict.reason()).isEqualTo(ConflictReason.DIRECT_CONFLICT);
    }

    @Test
    @DisplayName("Should detect cross-branch conflict when MRs target different branches and have common files")
    void shouldDetectCrossBranchConflictWhenMRsTargetDifferentBranchesAndHaveCommonFiles() {
        // Given
        var mr1 = MergeRequestInfo.builder()
                .id(1)
                .sourceBranch("feature-1")
                .targetBranch("develop")
                .changedFiles(Set.of("src/app.js", "src/utils.js"))
                .build();

        var mr2 = MergeRequestInfo.builder()
                .id(2)
                .sourceBranch("feature-2")
                .targetBranch("main")
                .changedFiles(Set.of("src/app.js", "src/config.js"))
                .build();

        var ignorePatterns = List.<String>of();

        // When
        var result = strategy.detectConflict(mr1, mr2, ignorePatterns);

        // Then
        assertThat(result).isPresent();
        var conflict = result.get();
        assertThat(conflict.firstMr()).isEqualTo(mr1);
        assertThat(conflict.secondMr()).isEqualTo(mr2);
        assertThat(conflict.conflictingFiles()).containsExactly("src/app.js");
        assertThat(conflict.reason()).isEqualTo(ConflictReason.CROSS_BRANCH_CONFLICT);
    }

    @Test
    @DisplayName("Should not detect conflict when MRs have dependency relationship")
    void shouldNotDetectConflictWhenMRsHaveDependencyRelationship() {
        // Given
        var mr1 = MergeRequestInfo.builder()
                .id(1)
                .sourceBranch("feature-1")
                .targetBranch("main")
                .changedFiles(Set.of("src/app.js"))
                .build();

        var mr2 = MergeRequestInfo.builder()
                .id(2)
                .sourceBranch("main")  // mr2 source is mr1 target
                .targetBranch("release")
                .changedFiles(Set.of("src/app.js"))
                .build();

        var ignorePatterns = List.<String>of();

        // When
        var result = strategy.detectConflict(mr1, mr2, ignorePatterns);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should not detect conflict when MRs have no common files")
    void shouldNotDetectConflictWhenMRsHaveNoCommonFiles() {
        // Given
        var mr1 = MergeRequestInfo.builder()
                .id(1)
                .sourceBranch("feature-1")
                .targetBranch("main")
                .changedFiles(Set.of("src/app.js"))
                .build();

        var mr2 = MergeRequestInfo.builder()
                .id(2)
                .sourceBranch("feature-2")
                .targetBranch("main")
                .changedFiles(Set.of("src/utils.js"))
                .build();

        var ignorePatterns = List.<String>of();

        // When
        var result = strategy.detectConflict(mr1, mr2, ignorePatterns);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should not detect conflict when common files are ignored")
    void shouldNotDetectConflictWhenCommonFilesAreIgnored() {
        // Given
        var mr1 = MergeRequestInfo.builder()
                .id(1)
                .sourceBranch("feature-1")
                .targetBranch("main")
                .changedFiles(Set.of("src/app.js"))
                .build();

        var mr2 = MergeRequestInfo.builder()
                .id(2)
                .sourceBranch("feature-2")
                .targetBranch("main")
                .changedFiles(Set.of("src/app.js"))
                .build();

        var ignorePatterns = List.of("src/app.js");

        // Configure mock
        when(patternMatcher.matches(eq("src/app.js"), anyString())).thenReturn(true);

        // When
        var result = strategy.detectConflict(mr1, mr2, ignorePatterns);

        // Then
        assertThat(result).isEmpty();
    }
}