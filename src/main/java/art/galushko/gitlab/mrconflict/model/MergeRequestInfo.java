package art.galushko.gitlab.mrconflict.model;

import lombok.Builder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Represents a merge request with its metadata for conflict detection.
 */
@Builder
public record MergeRequestInfo(int id, String title, String sourceBranch, String targetBranch,
                               List<String> changedFiles) {

    /**
     * Gets the common files between this MR and another MR.
     *
     * @param other the other merge request
     * @return list of common files
     */
    public List<String> getCommonFiles(MergeRequestInfo other) {
        return changedFiles.stream()
                .filter(other.changedFiles::contains)
                .toList();
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

    @NotNull
    @Override
    public String toString() {
        return "MR" + id + "{" +
                "source='" + sourceBranch + '\'' +
                ", target='" + targetBranch + '\'' +
                ", files=" + changedFiles.size() +
                '}';
    }
}

