package art.galushko.gitlab.mrconflict.core.strategy;

import art.galushko.gitlab.mrconflict.model.MergeRequestConflict;
import art.galushko.gitlab.mrconflict.model.MergeRequestInfo;

import java.util.List;
import java.util.Optional;

/**
 * Strategy interface for detecting conflicts between merge requests.
 * This interface allows for different conflict detection algorithms to be implemented
 * and used interchangeably, following the Open/Closed Principle.
 */
public interface ConflictDetectionStrategy {
    
    /**
     * Detects if there is a conflict between two merge requests.
     *
     * @param mr1 first merge request
     * @param mr2 second merge request
     * @param ignorePatterns patterns for files/directories to ignore
     * @return optional conflict if detected
     */
    Optional<MergeRequestConflict> detectConflict(MergeRequestInfo mr1, MergeRequestInfo mr2, 
                                                 List<String> ignorePatterns);
    
    /**
     * Gets the name of this strategy.
     *
     * @return the strategy name
     */
    String getStrategyName();
}