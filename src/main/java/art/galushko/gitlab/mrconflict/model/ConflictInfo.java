package art.galushko.gitlab.mrconflict.model;

import java.util.List;
import java.util.Objects;

/**
 * Represents a merge conflict detected in a file.
 */
public class ConflictInfo {
    private final String filePath;
    private final ConflictType type;
    private final List<ConflictSection> sections;
    private final String sourceCommit;
    private final String targetCommit;

    public ConflictInfo(String filePath, ConflictType type, List<ConflictSection> sections,
                       String sourceCommit, String targetCommit) {
        this.filePath = Objects.requireNonNull(filePath, "File path cannot be null");
        this.type = Objects.requireNonNull(type, "Conflict type cannot be null");
        this.sections = List.copyOf(Objects.requireNonNull(sections, "Sections cannot be null"));
        this.sourceCommit = sourceCommit;
        this.targetCommit = targetCommit;
    }

    public String getFilePath() {
        return filePath;
    }

    public ConflictType getType() {
        return type;
    }

    public List<ConflictSection> getSections() {
        return sections;
    }

    public String getSourceCommit() {
        return sourceCommit;
    }

    public String getTargetCommit() {
        return targetCommit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConflictInfo that = (ConflictInfo) o;
        return Objects.equals(filePath, that.filePath) &&
               type == that.type &&
               Objects.equals(sections, that.sections) &&
               Objects.equals(sourceCommit, that.sourceCommit) &&
               Objects.equals(targetCommit, that.targetCommit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePath, type, sections, sourceCommit, targetCommit);
    }

    @Override
    public String toString() {
        return "ConflictInfo{" +
               "filePath='" + filePath + '\'' +
               ", type=" + type +
               ", sections=" + sections.size() +
               ", sourceCommit='" + sourceCommit + '\'' +
               ", targetCommit='" + targetCommit + '\'' +
               '}';
    }
}

