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
    /**
     * Gets a formatted description of the conflict.
     *
     * @return formatted conflict description
     */
    public String getDescription() {
        if (conflictingFiles.size() == 1) {
            return String.format("conflict in modification of `%s`", conflictingFiles.get(0));
        } else {
            return String.format("conflicts in modification of %d files: %s",
                    conflictingFiles.size(),
                    String.join(", ", conflictingFiles.stream()
                            .map(f -> "`" + f + "`")
                            .toList()));
        }
    }

    /**
     * Gets the formatted output string for this conflict using merge request titles.
     *
     * @return formatted output string
     */
    public String getFormattedOutput() {
        String firstTitle = truncateTitle(firstMr.title());
        String secondTitle = truncateTitle(secondMr.title());

        return String.format("\"%s\" vs \"%s\"%n- Issue: %s",
                firstTitle, secondTitle, getDescription());
    }

    /**
     * Truncates merge request titles to a reasonable length for display.
     */
    private String truncateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return "Untitled";
        }

        final int MAX_TITLE_LENGTH = 50;
        String trimmedTitle = title.trim();

        if (trimmedTitle.length() <= MAX_TITLE_LENGTH) {
            return trimmedTitle;
        }

        return trimmedTitle.substring(0, MAX_TITLE_LENGTH - 3) + "...";
    }

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
        String firstTitle = truncateTitle(firstMr.title());
        String secondTitle = truncateTitle(secondMr.title());

        return String.format("\"%s\" vs \"%s\" (%d files)",
                firstTitle, secondTitle, conflictingFiles.size());
    }
}

