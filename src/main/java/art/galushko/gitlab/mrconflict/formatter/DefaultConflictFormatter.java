package art.galushko.gitlab.mrconflict.formatter;

import art.galushko.gitlab.mrconflict.model.MergeRequestConflict;
import lombok.extern.slf4j.Slf4j;
import org.gitlab4j.api.models.MergeRequest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation of ConflictFormatter that formats conflicts in a human-readable text format.
 */
@Slf4j
public class DefaultConflictFormatter implements ConflictFormatter {

    private static final int MAX_TITLE_LENGTH = 50;
    private static final int MAX_FILES_IN_NOTE = 10;

    /**
     * {@inheritDoc}
     */
    @Override
    public String formatConflict(MergeRequestConflict conflict) {
        String firstTitle = truncateTitle(conflict.firstMr().title());
        String secondTitle = truncateTitle(conflict.secondMr().title());

        return String.format("\"%s\" vs \"%s\"%n- Issue: %s",
                firstTitle, secondTitle, formatConflictDescription(conflict));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String formatConflicts(List<MergeRequestConflict> conflicts) {
        if (conflicts.isEmpty()) {
            return "No conflicts detected.";
        }

        return conflicts.stream()
                .map(this::formatConflict)
                .collect(Collectors.joining("\n"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String formatConflictNote(List<MergeRequestConflict> conflicts, Long mergeRequestIid, List<MergeRequest> resolvedConflictMrs) {
        var note = new StringBuilder();
        note.append("## Merge Request Conflict Analysis\n\n");

        appendInfoAboutResolvedConflicts(resolvedConflictMrs, note);

        if (conflicts.isEmpty()) {
            return note.append("No more conflicts detected. All conflicts are resolved!\n").toString();
        }

        note.append("This merge request has conflicts with the following merge requests:\n\n");

        conflicts.forEach(conflict ->
                appendMergeRequestConflictInfo(mergeRequestIid, conflict, note)
        );

        note.append("Please resolve these conflicts before merging.\n");
        return note.toString();
    }

    /**
     * Appends information about resolved conflicts to the note.
     *
     * @param resolvedConflictMrs list of merge requests that previously had conflicts but are now resolved
     * @param note the StringBuilder to append the information to
     */
    private void appendInfoAboutResolvedConflicts(List<MergeRequest> resolvedConflictMrs, StringBuilder note) {
        if (resolvedConflictMrs.isEmpty()) {
            return;
        }
        note.append("#### Resolved conflicts\n\n");
        resolvedConflictMrs.forEach(resolvedMr -> {
            note.append("- **Conflict with MR !").append(resolvedMr.getIid())
                    .append(" (").append(resolvedMr.getTitle()).append(")** due to ");
            switch (resolvedMr.getState()) {
                case "merged" -> note.append("merge. Please check merge request to verify changes.\n");
                case "closed" -> note.append("close. Changes were declined.\n");
                case "opened" -> note.append("open. No more conflicts detected.\n");
                default -> log.error("Unknown MR {} state: {}", resolvedMr.getIid(), resolvedMr.getState());
            }
        });
        note.append("\n\n");
    }

    /**
     * Appends information about a specific merge request conflict to the note.
     *
     * @param mergeRequestIid the ID of the current merge request
     * @param conflict the conflict to append information about
     * @param note the StringBuilder to append the information to
     */
    private void appendMergeRequestConflictInfo(Long mergeRequestIid, MergeRequestConflict conflict, StringBuilder note) {
        var otherMr = conflict.firstMr().id() == mergeRequestIid ?
                conflict.secondMr() : conflict.firstMr();

        note.append("### Conflict with MR !").append(otherMr.id())
                .append(" (").append(otherMr.title()).append(")\n");
        note.append("- **Source branch:** ").append(otherMr.sourceBranch()).append("\n");
        note.append("- **Target branch:** ").append(otherMr.targetBranch()).append("\n");
        note.append("- **Conflict reason:** ").append(conflict.reason()).append("\n");
        note.append("- **Conflicting files:** ").append(conflict.conflictingFiles().size()).append("\n");

        // List conflicting files (limit to MAX_FILES_IN_NOTE to avoid huge comments)
        int fileLimit = Math.min(conflict.conflictingFiles().size(), MAX_FILES_IN_NOTE);
        conflict.conflictingFiles().stream().limit(fileLimit)
                .forEach(file -> note.append("  - `").append(file).append("`\n"));

        if (conflict.conflictingFiles().size() > fileLimit) {
            note.append("  - ... and ")
                    .append(conflict.conflictingFiles().size() - fileLimit)
                    .append(" more files\n");
        }

        note.append("\n");
    }

    /**
     * Formats the description of a conflict.
     *
     * @param conflict the conflict to describe
     * @return formatted conflict description
     */
    private String formatConflictDescription(MergeRequestConflict conflict) {
        Set<String> conflictingFiles = conflict.conflictingFiles();

        if (conflictingFiles.size() == 1) {
            return String.format("conflict in modification of `%s`", conflictingFiles.iterator().next());
        } else {
            return String.format("conflicts in modification of %d files: %s",
                    conflictingFiles.size(),
                    String.join(", ", conflictingFiles.stream()
                            .map(f -> "`" + f + "`")
                            .toList()));
        }
    }

    /**
     * Truncates merge request titles to a reasonable length for display.
     *
     * @param title the title to truncate
     * @return the truncated title, or "Untitled" if the title is null or empty
     */
    private String truncateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return "Untitled";
        }

        String trimmedTitle = title.trim();

        if (trimmedTitle.length() <= MAX_TITLE_LENGTH) {
            return trimmedTitle;
        }

        return trimmedTitle.substring(0, MAX_TITLE_LENGTH - 3) + "...";
    }
}
