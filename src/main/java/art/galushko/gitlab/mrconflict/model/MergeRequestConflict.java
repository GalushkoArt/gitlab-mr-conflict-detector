package art.galushko.gitlab.mrconflict.model;

import lombok.Builder;

import java.util.List;
import java.util.Objects;

/**
 * Represents a conflict between two merge requests.
 */
@Builder
public record MergeRequestConflict(MergeRequestInfo firstMr, MergeRequestInfo secondMr, List<String> conflictingFiles,
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
