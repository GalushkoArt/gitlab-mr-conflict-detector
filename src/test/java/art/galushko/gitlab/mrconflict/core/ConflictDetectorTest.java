package art.galushko.gitlab.mrconflict.core;

import art.galushko.gitlab.mrconflict.git.GitOperationException;
import art.galushko.gitlab.mrconflict.git.GitRepository;
import art.galushko.gitlab.mrconflict.model.MergeResult;
import art.galushko.gitlab.mrconflict.model.MergeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConflictDetectorTest {
    
    @Mock
    private GitRepository gitRepository;
    
    private ConflictDetector conflictDetector;
    private ConflictDetectionConfig config;
    private Path repositoryPath;
    
    @BeforeEach
    void setUp() {
        config = ConflictDetectionConfig.defaultConfig();
        conflictDetector = new ConflictDetector(gitRepository, config);
        repositoryPath = Path.of("/test/repo");
    }
    
    @Test
    void shouldDetectConflictsSuccessfully() throws Exception {
        // Given
        String sourceBranch = "feature-branch";
        List<String> targetBranches = List.of("main", "develop");
        
        MergeResult cleanResult = new MergeResult(sourceBranch, "main", "abc123", "def456", 
                List.of(), MergeStatus.CLEAN, "No conflicts");
        MergeResult conflictedResult = new MergeResult(sourceBranch, "develop", "abc123", "ghi789", 
                List.of(), MergeStatus.CONFLICTED, "Conflicts detected");
        
        when(gitRepository.isValidRepository(repositoryPath)).thenReturn(true);
        when(gitRepository.branchExists(sourceBranch)).thenReturn(true);
        when(gitRepository.branchExists("main")).thenReturn(true);
        when(gitRepository.branchExists("develop")).thenReturn(true);
        when(gitRepository.detectConflicts(sourceBranch, "main")).thenReturn(cleanResult);
        when(gitRepository.detectConflicts(sourceBranch, "develop")).thenReturn(conflictedResult);
        
        // When
        List<MergeResult> results = conflictDetector.detectConflicts(repositoryPath, sourceBranch, targetBranches);
        
        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getStatus()).isEqualTo(MergeStatus.CLEAN);
        assertThat(results.get(1).getStatus()).isEqualTo(MergeStatus.CONFLICTED);
        
        verify(gitRepository).openRepository(repositoryPath);
        verify(gitRepository).fetch();
        verify(gitRepository).close();
    }
    
    @Test
    void shouldThrowExceptionForInvalidRepository() {
        // Given
        when(gitRepository.isValidRepository(repositoryPath)).thenReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> conflictDetector.detectConflicts(repositoryPath, "source", List.of("target")))
                .isInstanceOf(ConflictDetectionException.class)
                .hasMessageContaining("Invalid Git repository");
    }
    
    @Test
    void shouldThrowExceptionForNonExistentSourceBranch() throws Exception {
        // Given
        String sourceBranch = "non-existent";
        List<String> targetBranches = List.of("main");
        
        when(gitRepository.isValidRepository(repositoryPath)).thenReturn(true);
        when(gitRepository.branchExists(sourceBranch)).thenReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> conflictDetector.detectConflicts(repositoryPath, sourceBranch, targetBranches))
                .isInstanceOf(ConflictDetectionException.class)
                .hasMessageContaining("Source branch does not exist");
        
        verify(gitRepository).openRepository(repositoryPath);
        verify(gitRepository).close();
    }
    
    @Test
    void shouldThrowExceptionForNonExistentTargetBranch() throws Exception {
        // Given
        String sourceBranch = "feature";
        List<String> targetBranches = List.of("non-existent");
        
        when(gitRepository.isValidRepository(repositoryPath)).thenReturn(true);
        when(gitRepository.branchExists(sourceBranch)).thenReturn(true);
        when(gitRepository.branchExists("non-existent")).thenReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> conflictDetector.detectConflicts(repositoryPath, sourceBranch, targetBranches))
                .isInstanceOf(ConflictDetectionException.class)
                .hasMessageContaining("Target branch does not exist");
        
        verify(gitRepository).openRepository(repositoryPath);
        verify(gitRepository).close();
    }
    
    @Test
    void shouldSkipFetchWhenConfigured() throws Exception {
        // Given
        ConflictDetectionConfig noFetchConfig = ConflictDetectionConfig.builder()
                .fetchBeforeDetection(false)
                .build();
        ConflictDetector detector = new ConflictDetector(gitRepository, noFetchConfig);
        
        String sourceBranch = "feature";
        List<String> targetBranches = List.of("main");
        
        MergeResult result = new MergeResult(sourceBranch, "main", "abc123", "def456", 
                List.of(), MergeStatus.CLEAN, "No conflicts");
        
        when(gitRepository.isValidRepository(repositoryPath)).thenReturn(true);
        when(gitRepository.branchExists(anyString())).thenReturn(true);
        when(gitRepository.detectConflicts(sourceBranch, "main")).thenReturn(result);
        
        // When
        detector.detectConflicts(repositoryPath, sourceBranch, targetBranches);
        
        // Then
        verify(gitRepository, never()).fetch();
    }
    
    @Test
    void shouldReturnForcePassResultWhenEnabled() throws Exception {
        // Given
        ConflictDetectionConfig forcePassConfig = ConflictDetectionConfig.builder()
                .forcePassEnabled(true)
                .build();
        ConflictDetector detector = new ConflictDetector(gitRepository, forcePassConfig);
        
        String sourceBranch = "feature";
        List<String> targetBranches = List.of("main");
        
        MergeResult conflictedResult = new MergeResult(sourceBranch, "main", "abc123", "def456", 
                List.of(), MergeStatus.CONFLICTED, "Conflicts detected");
        
        when(gitRepository.isValidRepository(repositoryPath)).thenReturn(true);
        when(gitRepository.branchExists(anyString())).thenReturn(true);
        when(gitRepository.detectConflicts(sourceBranch, "main")).thenReturn(conflictedResult);
        
        // When
        List<MergeResult> results = detector.detectConflicts(repositoryPath, sourceBranch, targetBranches);
        
        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(MergeStatus.CLEAN);
        assertThat(results.get(0).getMessage()).contains("Force pass enabled");
        assertThat(results.get(0).hasConflicts()).isFalse();
    }
    
    @Test
    void shouldHandleGitOperationException() throws Exception {
        // Given
        String sourceBranch = "feature";
        List<String> targetBranches = List.of("main");
        
        when(gitRepository.isValidRepository(repositoryPath)).thenReturn(true);
        when(gitRepository.branchExists(anyString())).thenReturn(true);
        when(gitRepository.detectConflicts(sourceBranch, "main"))
                .thenThrow(new GitOperationException("Git operation failed"));
        
        // When
        List<MergeResult> results = conflictDetector.detectConflicts(repositoryPath, sourceBranch, targetBranches);
        
        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(MergeStatus.FAILED);
        assertThat(results.get(0).getMessage()).contains("Git operation failed");
    }
    
    @Test
    void shouldCheckUncommittedChanges() throws Exception {
        // Given
        when(gitRepository.hasUncommittedChanges()).thenReturn(true);
        
        // When
        boolean hasChanges = conflictDetector.hasUncommittedChanges(repositoryPath);
        
        // Then
        assertThat(hasChanges).isTrue();
        verify(gitRepository).openRepository(repositoryPath);
        verify(gitRepository).close();
    }
    
    @Test
    void shouldGetAffectedFiles() throws Exception {
        // Given
        List<String> expectedFiles = List.of("file1.java", "file2.java");
        when(gitRepository.getAffectedFiles("feature", "main")).thenReturn(expectedFiles);
        
        // When
        List<String> affectedFiles = conflictDetector.getAffectedFiles(repositoryPath, "feature", "main");
        
        // Then
        assertThat(affectedFiles).isEqualTo(expectedFiles);
        verify(gitRepository).openRepository(repositoryPath);
        verify(gitRepository).close();
    }
}

