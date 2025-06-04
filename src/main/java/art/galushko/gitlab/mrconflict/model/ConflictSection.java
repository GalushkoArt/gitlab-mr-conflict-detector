package art.galushko.gitlab.mrconflict.model;

import java.util.Objects;

/**
 * Represents a specific section of a file where a conflict occurs.
 */
public class ConflictSection {
    private final int startLine;
    private final int endLine;
    private final String sourceContent;
    private final String targetContent;
    private final String baseContent;

    public ConflictSection(int startLine, int endLine, String sourceContent,
                          String targetContent, String baseContent) {
        this.startLine = startLine;
        this.endLine = endLine;
        this.sourceContent = sourceContent;
        this.targetContent = targetContent;
        this.baseContent = baseContent;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public String getSourceContent() {
        return sourceContent;
    }

    public String getTargetContent() {
        return targetContent;
    }

    public String getBaseContent() {
        return baseContent;
    }

    public int getLineCount() {
        return endLine - startLine + 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConflictSection that = (ConflictSection) o;
        return startLine == that.startLine &&
               endLine == that.endLine &&
               Objects.equals(sourceContent, that.sourceContent) &&
               Objects.equals(targetContent, that.targetContent) &&
               Objects.equals(baseContent, that.baseContent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startLine, endLine, sourceContent, targetContent, baseContent);
    }

    @Override
    public String toString() {
        return "ConflictSection{" +
               "lines=" + startLine + "-" + endLine +
               ", sourceLines=" + (sourceContent != null ? sourceContent.split("\n").length : 0) +
               ", targetLines=" + (targetContent != null ? targetContent.split("\n").length : 0) +
               '}';
    }
}

