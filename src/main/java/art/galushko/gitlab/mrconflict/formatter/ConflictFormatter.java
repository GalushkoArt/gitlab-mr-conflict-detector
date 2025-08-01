package art.galushko.gitlab.mrconflict.formatter;

import art.galushko.gitlab.mrconflict.model.MergeRequestConflict;
import org.gitlab4j.api.models.MergeRequest;

import java.util.List;

/**
 * Interface for formatting conflict information.
 */
public interface ConflictFormatter {

    /**
     * Formats a single conflict.
     *
     * @param conflict the conflict to format
     * @return formatted string representation of the conflict
     */
    String formatConflict(MergeRequestConflict conflict);

    /**
     * Formats a list of conflicts.
     *
     * @param conflicts the list of conflicts to format
     * @return formatted string representation of the conflicts
     */
    String formatConflicts(List<MergeRequestConflict> conflicts);

    /**
     * Formats a note with conflict information for a specific merge request.
     *
     * @param conflicts           list of conflicts
     * @param mergeRequestIid     merge request IID
     * @param resolvedConflictMrs list of merge requests that previously had conflicts but are now resolved
     * @return formatted note
     */
    String formatConflictNote(List<MergeRequestConflict> conflicts, Long mergeRequestIid, List<MergeRequest> resolvedConflictMrs);
}
