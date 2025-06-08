package art.galushko.gitlab.mrconflict.formatter;

import art.galushko.gitlab.mrconflict.model.ConflictReason;
import art.galushko.gitlab.mrconflict.model.MergeRequestConflict;
import art.galushko.gitlab.mrconflict.model.MergeRequestInfo;
import org.gitlab4j.api.models.MergeRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultConflictFormatterTest {

    private final DefaultConflictFormatter formatter = new DefaultConflictFormatter();

    @Test
    @DisplayName("Should format a single conflict correctly")
    void shouldFormatSingleConflict() {
        // Given
        MergeRequestInfo mr1 = createMergeRequestInfo(1, "Feature A implementation", "feature/a", "main", Set.of("file1.java"));
        MergeRequestInfo mr2 = createMergeRequestInfo(2, "Feature B implementation", "feature/b", "main", Set.of("file1.java"));

        MergeRequestConflict conflict = MergeRequestConflict.builder()
                .firstMr(mr1)
                .secondMr(mr2)
                .conflictingFiles(Set.of("file1.java"))
                .reason(ConflictReason.DIRECT_CONFLICT)
                .build();

        // When
        String result = formatter.formatConflict(conflict);

        // Then
        assertThat(result).contains("\"Feature A implementation\" vs \"Feature B implementation\"");
        assertThat(result).contains("conflict in modification of `file1.java`");
    }

    @Test
    @DisplayName("Should truncate long titles when formatting conflicts")
    void shouldTruncateLongTitles() {
        // Given
        String longTitle1 = "This is an extremely long title that should be truncated in the formatted output because it exceeds the maximum length";
        String longTitle2 = "Another very long title that will also be truncated in the formatted output due to length restrictions";

        MergeRequestInfo mr1 = createMergeRequestInfo(1, longTitle1, "feature/a", "main", Set.of("file1.java"));
        MergeRequestInfo mr2 = createMergeRequestInfo(2, longTitle2, "feature/b", "main", Set.of("file1.java"));

        MergeRequestConflict conflict = MergeRequestConflict.builder()
                .firstMr(mr1)
                .secondMr(mr2)
                .conflictingFiles(Set.of("file1.java"))
                .reason(ConflictReason.DIRECT_CONFLICT)
                .build();

        // When
        String result = formatter.formatConflict(conflict);

        // Then
        assertThat(result).contains("\"This is an extremely long title that should be ...");
        assertThat(result).contains("\"Another very long title that will also be trunc...");
    }

    @Test
    @DisplayName("Should handle empty or null titles")
    void shouldHandleEmptyOrNullTitles() {
        // Given
        MergeRequestInfo mr1 = createMergeRequestInfo(1, null, "feature/a", "main", Set.of("file1.java"));
        MergeRequestInfo mr2 = createMergeRequestInfo(2, "", "feature/b", "main", Set.of("file1.java"));

        MergeRequestConflict conflict = MergeRequestConflict.builder()
                .firstMr(mr1)
                .secondMr(mr2)
                .conflictingFiles(Set.of("file1.java"))
                .reason(ConflictReason.DIRECT_CONFLICT)
                .build();

        // When
        String result = formatter.formatConflict(conflict);

        // Then
        assertThat(result).contains("\"Untitled\" vs \"Untitled\"");
    }

    @Test
    @DisplayName("Should format multiple conflicting files correctly")
    void shouldFormatMultipleConflictingFiles() {
        // Given
        Set<String> conflictingFiles = Set.of("file1.java", "file2.java", "file3.java");

        MergeRequestInfo mr1 = createMergeRequestInfo(1, "Feature A", "feature/a", "main", conflictingFiles);
        MergeRequestInfo mr2 = createMergeRequestInfo(2, "Feature B", "feature/b", "main", conflictingFiles);

        MergeRequestConflict conflict = MergeRequestConflict.builder()
                .firstMr(mr1)
                .secondMr(mr2)
                .conflictingFiles(conflictingFiles)
                .reason(ConflictReason.DIRECT_CONFLICT)
                .build();

        // When
        String result = formatter.formatConflict(conflict);

        // Then
        assertThat(result).contains("conflicts in modification of 3 files");
        assertThat(result).contains("`file1.java`");
        assertThat(result).contains("`file2.java`");
        assertThat(result).contains("`file3.java`");
    }

    @Test
    @DisplayName("Should format empty conflicts list correctly")
    void shouldFormatEmptyConflictsList() {
        // Given
        List<MergeRequestConflict> conflicts = Collections.emptyList();

        // When
        String result = formatter.formatConflicts(conflicts);

        // Then
        assertThat(result).isEqualTo("No conflicts detected.");
    }

    @Test
    @DisplayName("Should format multiple conflicts correctly")
    void shouldFormatMultipleConflicts() {
        // Given
        MergeRequestInfo mr1 = createMergeRequestInfo(1, "Feature A", "feature/a", "main", Set.of("file1.java"));
        MergeRequestInfo mr2 = createMergeRequestInfo(2, "Feature B", "feature/b", "main", Set.of("file1.java"));
        MergeRequestInfo mr3 = createMergeRequestInfo(3, "Feature C", "feature/c", "main", Set.of("file2.java"));

        MergeRequestConflict conflict1 = MergeRequestConflict.builder()
                .firstMr(mr1)
                .secondMr(mr2)
                .conflictingFiles(Set.of("file1.java"))
                .reason(ConflictReason.DIRECT_CONFLICT)
                .build();

        MergeRequestConflict conflict2 = MergeRequestConflict.builder()
                .firstMr(mr1)
                .secondMr(mr3)
                .conflictingFiles(Set.of("file2.java"))
                .reason(ConflictReason.CROSS_BRANCH_CONFLICT)
                .build();

        List<MergeRequestConflict> conflicts = Arrays.asList(conflict1, conflict2);

        // When
        String result = formatter.formatConflicts(conflicts);

        // Then
        assertThat(result).contains("\"Feature A\" vs \"Feature B\"");
        assertThat(result).contains("\"Feature A\" vs \"Feature C\"");
        assertThat(result).contains("conflict in modification of `file1.java`");
        assertThat(result).contains("conflict in modification of `file2.java`");
    }

    @Test
    @DisplayName("Should format conflict note with no conflicts correctly")
    void shouldFormatConflictNoteWithNoConflicts() {
        // Given
        List<MergeRequestConflict> conflicts = Collections.emptyList();
        Long mergeRequestIid = 1L;
        List<MergeRequest> resolvedConflictMrs = Collections.emptyList();

        // When
        String result = formatter.formatConflictNote(conflicts, mergeRequestIid, resolvedConflictMrs);

        // Then
        assertThat(result).contains("## Merge Request Conflict Analysis");
        assertThat(result).contains("No more conflicts detected. All conflicts are resolved!");
    }

    @Test
    @DisplayName("Should format conflict note with conflicts correctly")
    void shouldFormatConflictNoteWithConflicts() {
        // Given
        var mr1 = createMergeRequestInfo(1, "Feature A", "feature/a", "main", Set.of("file1.java", "file2.java"));
        var mr2 = createMergeRequestInfo(2, "Feature B", "feature/b", "main", Set.of("file1.java"));

        MergeRequestConflict conflict = MergeRequestConflict.builder()
                .firstMr(mr1)
                .secondMr(mr2)
                .conflictingFiles(Set.of("file1.java"))
                .reason(ConflictReason.DIRECT_CONFLICT)
                .build();

        List<MergeRequestConflict> conflicts = List.of(conflict);
        Long mergeRequestIid = 1L;
        List<MergeRequest> resolvedConflictMrs = List.of();

        // When
        String result = formatter.formatConflictNote(conflicts, mergeRequestIid, resolvedConflictMrs);

        // Then
        assertThat(result).isEqualTo("""
                ## Merge Request Conflict Analysis
                
                This merge request has conflicts with the following merge requests:
                
                ### Conflict with MR !2 (Feature B)
                - **Source branch:** feature/b
                - **Target branch:** main
                - **Conflict reason:** DIRECT_CONFLICT
                - **Conflicting files:** 1
                  - `file1.java`
                
                Please resolve these conflicts before merging.
                """);
    }

    @Test
    @DisplayName("Should format conflict note with resolved conflicts correctly")
    void shouldFormatConflictNoteWithResolvedConflicts() {
        // Given
        List<MergeRequestConflict> conflicts = Collections.emptyList();
        Long mergeRequestIid = 1L;

        // Create mock MergeRequest objects for resolved conflicts
        var mergedMr = new MergeRequest();
        mergedMr.setIid(2L);
        mergedMr.setTitle("Feature B");
        mergedMr.setState("merged");

        var closedMr = new MergeRequest();
        closedMr.setIid(3L);
        closedMr.setTitle("Feature C");
        closedMr.setState("closed");

        var openedMr = new MergeRequest();
        openedMr.setIid(4L);
        openedMr.setTitle("Feature D");
        openedMr.setState("opened");

        List<MergeRequest> resolvedConflictMrs = Arrays.asList(mergedMr, closedMr, openedMr);

        // When
        String result = formatter.formatConflictNote(conflicts, mergeRequestIid, resolvedConflictMrs);

        // Then
        assertThat(result).isEqualTo("""
                ## Merge Request Conflict Analysis
                
                #### Resolved conflicts
                
                - **Conflict with MR !2 (Feature B)** due to merge. Please check merge request to verify changes.
                - **Conflict with MR !3 (Feature C)** due to close. Changes were declined.
                - **Conflict with MR !4 (Feature D)** due to open. No more conflicts detected.
                
                
                No more conflicts detected. All conflicts are resolved!
                """);
    }

    @Test
    @DisplayName("Should format conflict note with many conflicting files correctly")
    void shouldFormatConflictNoteWithManyConflictingFiles() {
        // Given
        Set<String> manyFiles = new HashSet<>();
        for (int i = 1; i <= 15; i++) {
            manyFiles.add("file" + i + ".java");
        }

        MergeRequestInfo mr1 = createMergeRequestInfo(1, "Feature A", "feature/a", "main", manyFiles);
        MergeRequestInfo mr2 = createMergeRequestInfo(2, "Feature B", "feature/b", "main", manyFiles);

        MergeRequestConflict conflict = MergeRequestConflict.builder()
                .firstMr(mr1)
                .secondMr(mr2)
                .conflictingFiles(manyFiles)
                .reason(ConflictReason.DIRECT_CONFLICT)
                .build();

        List<MergeRequestConflict> conflicts = Collections.singletonList(conflict);
        Long mergeRequestIid = 1L;
        List<MergeRequest> resolvedConflictMrs = Collections.emptyList();

        // When
        String result = formatter.formatConflictNote(conflicts, mergeRequestIid, resolvedConflictMrs);

        // Then
        assertThat(result).contains("**Conflicting files:** 15");

        // Count the number of file entries in the result
        int fileCount = 0;
        for (int i = 1; i <= 15; i++) {
            if (result.contains("`file" + i + ".java`")) {
                fileCount++;
            }
        }

        // Should show exactly 10 files (the limit defined in DefaultConflictFormatter)
        assertThat(fileCount).isEqualTo(10);

        // Should indicate there are more files
        assertThat(result).contains("... and 5 more files");
    }

    @Test
    @DisplayName("Should handle conflict note for the second MR in a conflict")
    void shouldHandleConflictNoteForSecondMr() {
        // Given
        MergeRequestInfo mr1 = createMergeRequestInfo(1, "Feature A", "feature/a", "main", Set.of("file1.java"));
        MergeRequestInfo mr2 = createMergeRequestInfo(2, "Feature B", "feature/b", "main", Set.of("file1.java"));

        MergeRequestConflict conflict = MergeRequestConflict.builder()
                .firstMr(mr1)
                .secondMr(mr2)
                .conflictingFiles(Set.of("file1.java"))
                .reason(ConflictReason.DIRECT_CONFLICT)
                .build();

        List<MergeRequestConflict> conflicts = Collections.singletonList(conflict);
        Long mergeRequestIid = 2L; // Note this is the second MR's ID
        List<MergeRequest> resolvedConflictMrs = Collections.emptyList();

        // When
        String result = formatter.formatConflictNote(conflicts, mergeRequestIid, resolvedConflictMrs);

        // Then
        assertThat(result).contains("### Conflict with MR !1");
        assertThat(result).contains("(Feature A)");
        assertThat(result).contains("**Source branch:** feature/a");
    }

    // Helper method to create MergeRequestInfo objects
    private MergeRequestInfo createMergeRequestInfo(long id, String title, String sourceBranch, String targetBranch, Set<String> changedFiles) {
        return MergeRequestInfo.builder()
                .id(id)
                .title(title)
                .sourceBranch(sourceBranch)
                .targetBranch(targetBranch)
                .changedFiles(changedFiles)
                .labels(new HashSet<>())
                .draft(false)
                .build();
    }
}
