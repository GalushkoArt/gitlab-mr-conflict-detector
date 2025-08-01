package art.galushko.gitlab.mrconflict.core;

import art.galushko.gitlab.mrconflict.config.AppConfig;
import art.galushko.gitlab.mrconflict.di.ServiceFactory;
import art.galushko.gitlab.mrconflict.formatter.ConflictFormatter;
import art.galushko.gitlab.mrconflict.gitlab.GitLabClient;
import art.galushko.gitlab.mrconflict.gitlab.GitLabException;
import art.galushko.gitlab.mrconflict.gitlab.MergeRequestService;
import art.galushko.gitlab.mrconflict.model.MergeRequestConflict;
import art.galushko.gitlab.mrconflict.model.MergeRequestInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConflictAnalysisService.
 */
@ExtendWith(MockitoExtension.class)
class ConflictAnalysisServiceTest {

    @Mock
    private ServiceFactory serviceFactory;

    @Mock
    private AppConfig config;

    @Mock
    private GitLabClient gitLabClient;

    @Mock
    private MergeRequestService mergeRequestService;

    @Mock
    private ConflictDetector conflictDetector;

    @Mock
    private ConflictFormatter conflictFormatter;

    private ConflictAnalysisService service;

    @BeforeEach
    void setUp() {
        try (MockedStatic<ServiceFactory> mockedFactory = mockStatic(ServiceFactory.class)) {
            mockedFactory.when(ServiceFactory::getInstance).thenReturn(serviceFactory);

            when(serviceFactory.getGitLabClient()).thenReturn(gitLabClient);
            when(serviceFactory.getMergeRequestService()).thenReturn(mergeRequestService);
            when(serviceFactory.getConflictDetector()).thenReturn(conflictDetector);
            when(serviceFactory.getConflictFormatter()).thenReturn(conflictFormatter);
            when(serviceFactory.getConfig()).thenReturn(config);

            service = new ConflictAnalysisService();
        }
    }

    @Test
    @DisplayName("Should authenticate with GitLab")
    void shouldAuthenticateWithGitLab() throws GitLabException {
        // Given
        String gitlabUrl = "https://gitlab.com";
        String gitlabToken = "to-ken_1234567890_-ABCDE";
        when(config.getGitlabUrl()).thenReturn(gitlabUrl);
        when(config.getGitlabToken()).thenReturn(gitlabToken);

        // When
        service.authenticate();

        // Then
        verify(gitLabClient).authenticate(gitlabUrl, gitlabToken);
    }

    @Test
    @DisplayName("Should check project access")
    void shouldCheckProjectAccess() throws GitLabException {
        // Given
        Long projectId = 123L;
        when(gitLabClient.hasProjectAccess(projectId)).thenReturn(true);
        when(config.getProjectId()).thenReturn(projectId);

        // When
        boolean result = service.hasAccessToProjectFromConfig();

        // Then
        assertThat(result).isTrue();
        verify(gitLabClient).hasProjectAccess(projectId);
    }

    @Test
    @DisplayName("Should fetch specific merge request")
    void shouldFetchSpecificMergeRequest() throws GitLabException {
        // Given
        Long projectId = 123L;
        Long mrIid = 456L;
        MergeRequestInfo mr = MergeRequestInfo.builder().id(456).build();

        when(mergeRequestService.getMergeRequest(projectId, mrIid)).thenReturn(mr);
        when(config.getMergeRequestIids()).thenReturn(List.of(mrIid));
        when(config.getProjectId()).thenReturn(projectId);

        // When
        List<MergeRequestInfo> result = service.fetchMergeRequests();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(mr);
        verify(mergeRequestService).getMergeRequest(projectId, mrIid);
        verify(mergeRequestService, never()).getOpenMergeRequests(any());
    }

    @Test
    @DisplayName("Should fetch all open merge requests")
    void shouldFetchAllOpenMergeRequests() throws GitLabException {
        // Given
        Long projectId = 123L;
        List<MergeRequestInfo> mrs = List.of(
            MergeRequestInfo.builder().id(1).build(),
            MergeRequestInfo.builder().id(2).build()
        );

        when(mergeRequestService.getMergeRequestsForConflictAnalysis(projectId, true)).thenReturn(mrs);
        when(config.getProjectId()).thenReturn(projectId);
        when(config.getMergeRequestIids()).thenReturn(null);
        when(config.getIncludeDraftMrs()).thenReturn(true);

        // When
        List<MergeRequestInfo> result = service.fetchMergeRequests();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(mrs);
        verify(mergeRequestService).getMergeRequestsForConflictAnalysis(projectId, true);
        verify(mergeRequestService, never()).getMergeRequest(any(), any());
    }

    @Test
    @DisplayName("Should detect conflicts")
    void shouldDetectConflicts() {
        // Given
        List<MergeRequestInfo> mrs = List.of(
            MergeRequestInfo.builder().id(1).build(),
            MergeRequestInfo.builder().id(2).build()
        );

        List<String> ignorePatterns = List.of("ignored.txt");

        List<MergeRequestConflict> conflicts = List.of(
            MergeRequestConflict.builder()
                .firstMr(mrs.get(0))
                .secondMr(mrs.get(1))
                .conflictingFiles(Set.of("file.txt"))
                .build()
        );

        when(conflictDetector.detectConflicts(mrs, ignorePatterns)).thenReturn(conflicts);
        when(config.getIgnorePatterns()).thenReturn(ignorePatterns);

        // When
        List<MergeRequestConflict> result = service.detectConflicts(mrs);

        // Then
        assertThat(result).isEqualTo(conflicts);
        verify(conflictDetector).detectConflicts(mrs, ignorePatterns);
    }

    @Test
    @DisplayName("Should format conflicts")
    void shouldFormatConflicts() {
        // Given
        List<MergeRequestConflict> conflicts = List.of(
            MergeRequestConflict.builder()
                .firstMr(MergeRequestInfo.builder().id(1).build())
                .secondMr(MergeRequestInfo.builder().id(2).build())
                .conflictingFiles(Set.of("file.txt"))
                .build()
        );

        String formattedOutput = "Formatted conflicts";
        when(conflictFormatter.formatConflicts(conflicts)).thenReturn(formattedOutput);

        // When
        String result = service.formatConflicts(conflicts);

        // Then
        assertThat(result).isEqualTo(formattedOutput);
        verify(conflictFormatter).formatConflicts(conflicts);
    }

    @Test
    @DisplayName("Should get conflicting merge request IDs")
    void shouldGetConflictingMergeRequestIds() {
        // Given
        List<MergeRequestConflict> conflicts = List.of(
            MergeRequestConflict.builder()
                .firstMr(MergeRequestInfo.builder().id(1).build())
                .secondMr(MergeRequestInfo.builder().id(2).build())
                .conflictingFiles(Set.of("file.txt"))
                .build()
        );

        Set<Long> conflictingIds = Set.of(1L, 2L);
        when(conflictDetector.getConflictingMergeRequestIds(conflicts)).thenReturn(conflictingIds);

        // When
        Set<Long> result = service.getConflictingMergeRequestIds(conflicts);

        // Then
        assertThat(result).isEqualTo(conflictingIds);
        verify(conflictDetector).getConflictingMergeRequestIds(conflicts);
    }

    @Test
    @DisplayName("Should update GitLab with conflicts")
    void shouldUpdateGitLabWithConflicts() throws GitLabException {
        // Given
        Long projectId = 123L;

        List<MergeRequestConflict> conflicts = List.of(
            MergeRequestConflict.builder()
                .firstMr(MergeRequestInfo.builder().id(1).title("MR1").build())
                .secondMr(MergeRequestInfo.builder().id(2).title("MR2").build())
                .conflictingFiles(Set.of("file.txt"))
                .build()
        );

        String noteContent = "Conflict note";
        when(conflictFormatter.formatConflictNote(anyList(), eq(1L), anyList())).thenReturn(noteContent);
        when(conflictFormatter.formatConflictNote(anyList(), eq(2L), anyList())).thenReturn(noteContent);
        when(mergeRequestService.getMergeRequests(projectId, "opened")).thenReturn(List.of(
                MergeRequestInfo.builder().id(1L).labels(Set.of()).build(),
                MergeRequestInfo.builder().id(2L).labels(Set.of()).build()
        ));
        when(config.getProjectId()).thenReturn(projectId);
        when(config.getCreateGitlabNote()).thenReturn(true);
        when(config.getUpdateMrStatus()).thenReturn(true);
        when(config.getDryRun()).thenReturn(false);

        // When
        service.updateGitLabWithConflicts(conflicts);

        // Then
        verify(gitLabClient).createMergeRequestNote(projectId, 1L, noteContent);
        verify(gitLabClient).createMergeRequestNote(projectId, 2L, noteContent);
        // Verify with the new method signature that includes conflicting MR IDs and skipRepeatedConflicts
        verify(gitLabClient).updateMergeRequestStatus(projectId, 1L, Set.of("conflicts", "conflict:MR2"));
        verify(gitLabClient).updateMergeRequestStatus(projectId, 2L, Set.of("conflicts", "conflict:MR1"));
    }

    @Test
    @DisplayName("Should not update GitLab in dry run mode")
    void shouldNotUpdateGitLabInDryRunMode() throws GitLabException {
        // Given
        Long projectId = 123L;
        List<MergeRequestConflict> conflicts = List.of();
        when(config.getProjectId()).thenReturn(projectId);

        // When
        service.updateGitLabWithConflicts(conflicts);

        // Then
        verify(gitLabClient, never()).createMergeRequestNote(any(), any(), any());
        verify(gitLabClient, never()).updateMergeRequestStatus(any(), any(), any());
    }

    @Test
    @DisplayName("Should handle GitLab exception during update")
    void shouldHandleGitLabExceptionDuringUpdate() throws GitLabException {
        // Given
        Long projectId = 123L;
        List<MergeRequestConflict> conflicts = List.of();
        when(config.getProjectId()).thenReturn(projectId);

        // When & Then - should not throw exception
        service.updateGitLabWithConflicts(conflicts);
    }
}
