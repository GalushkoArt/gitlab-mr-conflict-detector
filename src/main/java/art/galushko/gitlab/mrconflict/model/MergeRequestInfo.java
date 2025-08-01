package art.galushko.gitlab.mrconflict.model;

import lombok.Builder;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a merge request with its metadata for conflict detection.
 * This record encapsulates all the necessary information about a merge request
 * that is needed for conflict detection and analysis.
 * 
 * @param id the unique identifier of the merge request (IID in GitLab terminology)
 * @param title the title of the merge request
 * @param sourceBranch the source branch of the merge request
 * @param targetBranch the target branch of the merge request
 * @param changedFiles the set of files changed in this merge request
 * @param labels the set of labels applied to this merge request
 * @param draft indicates whether this is a draft/WIP merge request
 */
@Builder
public record MergeRequestInfo(long id, String title, String sourceBranch, String targetBranch,
                               Set<String> changedFiles, Set<String> labels, boolean draft) {

    /**
     * Gets the common files between this MR and another MR.
     *
     * @param other the other merge request
     * @return list of common files
     */
    public List<String> getCommonFiles(MergeRequestInfo other) {
        // Create a new set to avoid modifying the original
        Set<String> intersection = new LinkedHashSet<>(this.changedFiles);
        // Retain only elements that are in both sets (intersection)
        intersection.retainAll(other.changedFiles);
        // Convert back to list for compatibility with existing code
        return intersection.stream().toList();
    }

    /**
     * Checks if this MR has any files in common with another MR.
     * This is more efficient than getting all common files when we just need to know if there are any.
     *
     * @param other the other merge request
     * @return true if there are any common files
     */
    public boolean hasCommonFiles(MergeRequestInfo other) {
        // Early termination if either set is empty
        if (this.changedFiles.isEmpty() || other.changedFiles.isEmpty()) {
            return false;
        }

        // Use the smaller set for iteration to improve performance
        if (this.changedFiles.size() < other.changedFiles.size()) {
            return this.changedFiles.stream().anyMatch(other.changedFiles::contains);
        } else {
            return other.changedFiles.stream().anyMatch(this.changedFiles::contains);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MergeRequestInfo that = (MergeRequestInfo) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "MR" + id + "{" +
                "source='" + sourceBranch + '\'' +
                ", target='" + targetBranch + '\'' +
                ", files=" + changedFiles.size() +
                '}';
    }
}
