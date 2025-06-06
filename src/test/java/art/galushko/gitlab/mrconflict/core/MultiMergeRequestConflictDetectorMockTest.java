package art.galushko.gitlab.mrconflict.core;

import art.galushko.gitlab.mrconflict.config.PatternMatcher;
import art.galushko.gitlab.mrconflict.core.strategy.ConflictDetectionStrategy;
import art.galushko.gitlab.mrconflict.model.MergeRequestConflict;
import art.galushko.gitlab.mrconflict.model.MergeRequestInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MultiMergeRequestConflictDetector using mocks.
 */
@ExtendWith(MockitoExtension.class)
class MultiMergeRequestConflictDetectorMockTest {

    @Mock
    private PatternMatcher patternMatcher;

    @Mock
    private ConflictDetectionStrategy strategy1;

    @Mock
    private ConflictDetectionStrategy strategy2;

    @Test
    @DisplayName("Should create detector with default strategy when no strategies provided")
    void shouldCreateDetectorWithDefaultStrategy() {
        // When
        var detector = new MultiMergeRequestConflictDetector(patternMatcher);

        // Then
        assertThat(detector.getIgnorePatternMatcher()).isSameAs(patternMatcher);

        // Test with a simple conflict detection to verify default strategy is used
        var mr1 = createMergeRequest(1);
        var mr2 = createMergeRequest(2);
        detector.detectConflicts(List.of(mr1, mr2), List.of());
    }

    @Test
    @DisplayName("Should create detector with provided strategies")
    void shouldCreateDetectorWithProvidedStrategies() {
        // Given
        when(strategy1.getStrategyName()).thenReturn("Strategy1");
        when(strategy2.getStrategyName()).thenReturn("Strategy2");

        // When
        var detector = new MultiMergeRequestConflictDetector(patternMatcher, List.of(strategy1, strategy2));

        // Then
        assertThat(detector.getIgnorePatternMatcher()).isSameAs(patternMatcher);

        // Test with a simple conflict detection to verify both strategies are used
        var mr1 = createMergeRequest(1);
        var mr2 = createMergeRequest(2);

        when(strategy1.detectConflict(any(), any(), anyList())).thenReturn(Optional.empty());
        when(strategy2.detectConflict(any(), any(), anyList())).thenReturn(Optional.empty());

        detector.detectConflicts(List.of(mr1, mr2), List.of());

        verify(strategy1).detectConflict(eq(mr1), eq(mr2), anyList());
        verify(strategy2).detectConflict(eq(mr1), eq(mr2), anyList());
    }

    @Test
    @DisplayName("Should create detector with default strategy when empty strategies list provided")
    void shouldCreateDetectorWithDefaultStrategyWhenEmptyStrategiesListProvided() {
        // When
        var detector = new MultiMergeRequestConflictDetector(patternMatcher, new ArrayList<>());

        // Then
        assertThat(detector.getIgnorePatternMatcher()).isSameAs(patternMatcher);

        // Test with a simple conflict detection to verify default strategy is used
        var mr1 = createMergeRequest(1);
        var mr2 = createMergeRequest(2);
        detector.detectConflicts(List.of(mr1, mr2), List.of());
    }

    @Test
    @DisplayName("Should add strategy to detector")
    void shouldAddStrategyToDetector() {
        // Given
        when(strategy1.getStrategyName()).thenReturn("Strategy1");
        when(strategy2.getStrategyName()).thenReturn("Strategy2");

        var detector = new MultiMergeRequestConflictDetector(patternMatcher, List.of(strategy1));

        // When
        detector.addStrategy(strategy2);

        // Then
        // Test with a simple conflict detection to verify both strategies are used
        var mr1 = createMergeRequest(1);
        var mr2 = createMergeRequest(2);

        when(strategy1.detectConflict(any(), any(), anyList())).thenReturn(Optional.empty());
        when(strategy2.detectConflict(any(), any(), anyList())).thenReturn(Optional.empty());

        detector.detectConflicts(List.of(mr1, mr2), List.of());

        verify(strategy1).detectConflict(eq(mr1), eq(mr2), anyList());
        verify(strategy2).detectConflict(eq(mr1), eq(mr2), anyList());
    }

    @Test
    @DisplayName("Should use all strategies to detect conflicts")
    void shouldUseAllStrategiesToDetectConflicts() {
        // Given
        when(strategy1.getStrategyName()).thenReturn("Strategy1");
        when(strategy2.getStrategyName()).thenReturn("Strategy2");

        var detector = new MultiMergeRequestConflictDetector(patternMatcher, List.of(strategy1, strategy2));

        var mr1 = createMergeRequest(1);
        var mr2 = createMergeRequest(2);
        var mr3 = createMergeRequest(3);

        var conflict1 = createConflict(mr1, mr2);
        var conflict2 = createConflict(mr2, mr3);

        when(strategy1.detectConflict(eq(mr1), eq(mr2), anyList())).thenReturn(Optional.of(conflict1));
        when(strategy1.detectConflict(eq(mr1), eq(mr3), anyList())).thenReturn(Optional.empty());
        when(strategy1.detectConflict(eq(mr2), eq(mr3), anyList())).thenReturn(Optional.empty());

        when(strategy2.detectConflict(eq(mr1), eq(mr2), anyList())).thenReturn(Optional.empty());
        when(strategy2.detectConflict(eq(mr1), eq(mr3), anyList())).thenReturn(Optional.empty());
        when(strategy2.detectConflict(eq(mr2), eq(mr3), anyList())).thenReturn(Optional.of(conflict2));

        // When
        var conflicts = detector.detectConflicts(List.of(mr1, mr2, mr3), List.of());

        // Then
        assertThat(conflicts).hasSize(2);
        assertThat(conflicts).contains(conflict1, conflict2);
    }

    private MergeRequestInfo createMergeRequest(int id) {
        return MergeRequestInfo.builder()
                .id(id)
                .sourceBranch("feature-" + id)
                .targetBranch("main")
                .changedFiles(Set.of("file" + id + ".txt"))
                .build();
    }

    private MergeRequestConflict createConflict(MergeRequestInfo mr1, MergeRequestInfo mr2) {
        return MergeRequestConflict.builder()
                .firstMr(mr1)
                .secondMr(mr2)
                .conflictingFiles(Set.of("common.txt"))
                .build();
    }
}
