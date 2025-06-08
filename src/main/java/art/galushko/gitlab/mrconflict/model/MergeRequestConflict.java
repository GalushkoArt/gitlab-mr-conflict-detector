package art.galushko.gitlab.mrconflict.model;

import lombok.Builder;

import java.util.Objects;
import java.util.Set;

/**
 * Represents a conflict between two merge requests.
 * This record encapsulates all the information about a conflict between two merge requests,
 * including the merge requests involved, the conflicting files, and the reason for the conflict.
 * 
 * @param firstMr the first merge request involved in the conflict
 * @param secondMr the second merge request involved in the conflict
 * @param conflictingFiles the set of files that are in conflict between the two merge requests
 * @param reason the reason for the conflict (e.g., DIRECT_CONFLICT, INDIRECT_CONFLICT, etc.)
 */
@Builder
public record MergeRequestConflict(MergeRequestInfo firstMr, MergeRequestInfo secondMr, Set<String> conflictingFiles,
                                   ConflictReason reason) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MergeRequestConflict that = (MergeRequestConflict) o;
        return Objects.equals(firstMr, that.firstMr) &&
                Objects.equals(secondMr, that.secondMr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstMr, secondMr);
    }

    @Override
    public String toString() {
        return "MergeRequestConflict{" +
                "firstMr=" + firstMr +
                ", secondMr=" + secondMr +
                ", conflictingFiles=" + conflictingFiles.size() +
                ", reason=" + reason +
                '}';
    }
}
