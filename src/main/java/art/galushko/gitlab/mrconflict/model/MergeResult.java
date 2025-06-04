package art.galushko.gitlab.mrconflict.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Represents the result of a merge conflict detection operation.
 */
public class MergeResult {
    private final String sourceBranch;
    private final String targetBranch;
    private final String sourceCommit;
    private final String targetCommit;
    private final List<ConflictInfo> conflicts;
    private final MergeStatus status;
    private final Instant timestamp;
    private final String message;

    public MergeResult(String sourceBranch, String targetBranch, String sourceCommit, 
                      String targetCommit, List<ConflictInfo> conflicts, MergeStatus status, 
                      String message) {
        this.sourceBranch = Objects.requireNonNull(sourceBranch, "Source branch cannot be null");
        this.targetBranch = Objects.requireNonNull(targetBranch, "Target branch cannot be null");
        this.sourceCommit = sourceCommit;
        this.targetCommit = targetCommit;
        this.conflicts = List.copyOf(Objects.requireNonNull(conflicts, "Conflicts cannot be null"));
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.timestamp = Instant.now();
        this.message = message;
    }

    public String getSourceBranch() {
        return sourceBranch;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public String getSourceCommit() {
        return sourceCommit;
    }

    public String getTargetCommit() {
        return targetCommit;
    }

    public List<ConflictInfo> getConflicts() {
        return conflicts;
    }

    public MergeStatus getStatus() {
        return status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    public boolean hasConflicts() {
        return !conflicts.isEmpty();
    }

    public int getConflictCount() {
        return conflicts.size();
    }

    public int getTotalConflictSections() {
        return conflicts.stream()
                .mapToInt(conflict -> conflict.getSections().size())
                .sum();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MergeResult that = (MergeResult) o;
        return Objects.equals(sourceBranch, that.sourceBranch) &&
               Objects.equals(targetBranch, that.targetBranch) &&
               Objects.equals(sourceCommit, that.sourceCommit) &&
               Objects.equals(targetCommit, that.targetCommit) &&
               Objects.equals(conflicts, that.conflicts) &&
               status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceBranch, targetBranch, sourceCommit, targetCommit, conflicts, status);
    }

    @Override
    public String toString() {
        return "MergeResult{" +
               "sourceBranch='" + sourceBranch + '\'' +
               ", targetBranch='" + targetBranch + '\'' +
               ", status=" + status +
               ", conflicts=" + conflicts.size() +
               ", timestamp=" + timestamp +
               '}';
    }
}

