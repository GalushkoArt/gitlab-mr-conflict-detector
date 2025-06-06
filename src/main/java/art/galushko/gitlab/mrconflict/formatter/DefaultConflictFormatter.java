package art.galushko.gitlab.mrconflict.formatter;

import art.galushko.gitlab.mrconflict.model.MergeRequestConflict;
import art.galushko.gitlab.mrconflict.model.MergeRequestInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
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

    @Override
    public String formatConflict(MergeRequestConflict conflict) {
        String firstTitle = truncateTitle(conflict.firstMr().title());
        String secondTitle = truncateTitle(conflict.secondMr().title());

        return String.format("\"%s\" vs \"%s\"%n- Issue: %s",
                firstTitle, secondTitle, formatConflictDescription(conflict));
    }

    @Override
    public String formatConflicts(List<MergeRequestConflict> conflicts) {
        if (conflicts.isEmpty()) {
            return "No conflicts detected.";
        }

        return conflicts.stream()
                .map(this::formatConflict)
                .collect(Collectors.joining("\n"));
    }

    @Override
    public String formatConflictNote(List<MergeRequestConflict> conflicts, Long mergeRequestIid) {
        StringBuilder note = new StringBuilder();
        note.append("## Merge Request Conflict Analysis\n\n");
        note.append("This merge request has conflicts with the following merge requests:\n\n");

        for (MergeRequestConflict conflict : conflicts) {
            MergeRequestInfo otherMr = conflict.firstMr().id() == mergeRequestIid ?
                    conflict.secondMr() : conflict.firstMr();

            note.append("### Conflict with MR !").append(otherMr.id())
                    .append(" (").append(otherMr.title()).append(")\n");
            note.append("- **Source branch:** ").append(otherMr.sourceBranch()).append("\n");
            note.append("- **Target branch:** ").append(otherMr.targetBranch()).append("\n");
            note.append("- **Conflict reason:** ").append(conflict.reason()).append("\n");
            note.append("- **Conflicting files:** ").append(conflict.conflictingFiles().size()).append("\n");

            // List conflicting files (limit to MAX_FILES_IN_NOTE to avoid huge comments)
            int fileLimit = Math.min(conflict.conflictingFiles().size(), MAX_FILES_IN_NOTE);
            List<String> filesList = new ArrayList<>(conflict.conflictingFiles());
            for (int i = 0; i < fileLimit; i++) {
                note.append("  - `").append(filesList.get(i)).append("`\n");
            }

            if (conflict.conflictingFiles().size() > fileLimit) {
                note.append("  - ... and ").append(conflict.conflictingFiles().size() - fileLimit)
                        .append(" more files\n");
            }

            note.append("\n");
        }

        note.append("Please resolve these conflicts before merging.\n");
        return note.toString();
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
                            .collect(Collectors.toList())));
        }
    }

    /**
     * Truncates merge request titles to a reasonable length for display.
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
