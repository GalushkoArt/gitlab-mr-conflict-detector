package art.galushko.gitlab.mrconflict.core;

import art.galushko.gitlab.mrconflict.core.strategy.ConflictDetectionStrategy;
import art.galushko.gitlab.mrconflict.core.strategy.DefaultConflictDetectionStrategy;
import art.galushko.gitlab.mrconflict.model.MergeRequestConflict;
import art.galushko.gitlab.mrconflict.model.MergeRequestInfo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for detecting conflicts between multiple merge requests.
 * This class uses the Strategy pattern to allow different conflict detection algorithms
 * to be used interchangeably, following the Open/Closed Principle.
 */
@Slf4j
public class MultiMergeRequestConflictDetector implements ConflictDetector {

    private final List<ConflictDetectionStrategy> strategies;

    @Getter
    private final PatternMatcher ignorePatternMatcher;

    /**
     * Creates a new MultiMergeRequestConflictDetector with the default strategy.
     *
     * @param ignorePatternMatcher the pattern matcher for ignoring files
     */
    public MultiMergeRequestConflictDetector(PatternMatcher ignorePatternMatcher) {
        this.ignorePatternMatcher = ignorePatternMatcher;
        this.strategies = new ArrayList<>();

        // Add the default strategy
        this.strategies.add(new DefaultConflictDetectionStrategy(ignorePatternMatcher));
    }

    /**
     * Creates a new MultiMergeRequestConflictDetector with custom strategies.
     *
     * @param ignorePatternMatcher the pattern matcher for ignoring files
     * @param strategies the conflict detection strategies to use
     */
    public MultiMergeRequestConflictDetector(PatternMatcher ignorePatternMatcher, 
                                            List<ConflictDetectionStrategy> strategies) {
        this.ignorePatternMatcher = ignorePatternMatcher;
        this.strategies = new ArrayList<>(strategies);

        // Ensure there's at least one strategy
        if (this.strategies.isEmpty()) {
            this.strategies.add(new DefaultConflictDetectionStrategy(ignorePatternMatcher));
        }
    }

    /**
     * Adds a conflict detection strategy.
     *
     * @param strategy the strategy to add
     */
    public void addStrategy(ConflictDetectionStrategy strategy) {
        this.strategies.add(strategy);
    }

    /**
     * Detects conflicts between multiple merge requests using all registered strategies.
     *
     * @param mergeRequests list of merge requests to analyze
     * @param ignorePatterns patterns for files/directories to ignore
     * @return list of detected conflicts
     */
    @Override
    public List<MergeRequestConflict> detectConflicts(List<MergeRequestInfo> mergeRequests,
                                                      List<String> ignorePatterns) {
        log.info("Detecting conflicts between {} merge requests using {} strategies", 
                mergeRequests.size(), strategies.size());

        Set<MergeRequestConflict> conflicts = new LinkedHashSet<>();

        // Check all pairs of merge requests with all strategies
        for (int i = 0; i < mergeRequests.size(); i++) {
            for (int j = i + 1; j < mergeRequests.size(); j++) {
                var mr1 = mergeRequests.get(i);
                var mr2 = mergeRequests.get(j);

                for (ConflictDetectionStrategy strategy : strategies) {
                    log.debug("Using strategy '{}' to detect conflicts between MR{} and MR{}", 
                            strategy.getStrategyName(), mr1.id(), mr2.id());

                    Optional<MergeRequestConflict> conflict = 
                            strategy.detectConflict(mr1, mr2, ignorePatterns);

                    conflict.ifPresent(conflicts::add);
                }
            }
        }

        List<MergeRequestConflict> result = new ArrayList<>(conflicts);
        log.info("Detected {} conflicts", result.size());
        return result;
    }

    /**
     * Gets the list of merge request IDs that have conflicts.
     *
     * @param conflicts list of detected conflicts
     * @return set of MR IDs that have conflicts
     */
    @Override
    public Set<Long> getConflictingMergeRequestIds(List<MergeRequestConflict> conflicts) {
        return conflicts.stream()
                .flatMap(conflict -> Stream.of(conflict.firstMr().id(), conflict.secondMr().id()))
                .collect(Collectors.toSet());
    }
}
