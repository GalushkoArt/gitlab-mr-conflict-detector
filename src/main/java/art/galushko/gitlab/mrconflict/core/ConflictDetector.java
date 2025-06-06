package art.galushko.gitlab.mrconflict.core;

import art.galushko.gitlab.mrconflict.model.MergeRequestConflict;
import art.galushko.gitlab.mrconflict.model.MergeRequestInfo;

import java.util.List;
import java.util.Set;

/**
 * Interface for detecting conflicts between merge requests.
 */
public interface ConflictDetector {

    /**
     * Detects conflicts between multiple merge requests.
     *
     * @param mergeRequests list of merge requests to analyze
     * @param ignorePatterns patterns for files/directories to ignore
     * @return list of detected conflicts
     */
    List<MergeRequestConflict> detectConflicts(List<MergeRequestInfo> mergeRequests,
                                              List<String> ignorePatterns);

    /**
     * Formats the conflicts into the required output format.
     *
     * @param conflicts list of detected conflicts
     * @return formatted output string
     */
    String formatConflicts(List<MergeRequestConflict> conflicts);

    /**
     * Gets the list of merge request IDs that have conflicts.
     *
     * @param conflicts list of detected conflicts
     * @return set of MR IDs that have conflicts
     */
    Set<Integer> getConflictingMergeRequestIds(List<MergeRequestConflict> conflicts);
}