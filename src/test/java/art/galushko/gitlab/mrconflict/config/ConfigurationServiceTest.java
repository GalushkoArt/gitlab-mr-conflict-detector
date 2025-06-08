package art.galushko.gitlab.mrconflict.config;

import art.galushko.gitlab.mrconflict.gitlab.GitLabException;
import art.galushko.gitlab.mrconflict.security.InputValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigurationServiceTest {

    @Mock
    private ConfigLoader configLoader;

    @Mock
    private InputValidator inputValidator;

    @InjectMocks
    private ConfigurationService configurationService;

    private AppConfig fileConfig;
    private AppConfig cliConfig;
    private AppConfig envConfig;

    @BeforeEach
    void setUp() {
        // Create sample configurations for testing
        fileConfig = AppConfig.builder()
                .gitlabUrl("https://gitlab.file.com")
                .gitlabToken("file-token")
                .projectId(1L)
                .mergeRequestIids(List.of(2L))
                .createGitlabNote(true)
                .updateMrStatus(true)
                .dryRun(false)
                .verbose(false)
                .includeDraftMrs(false)
                .ignorePatterns(Arrays.asList("file-pattern1", "file-pattern2"))
                .build();

        cliConfig = AppConfig.builder()
                .gitlabUrl("https://gitlab.cli.com")
                .gitlabToken("cli-token")
                .projectId(3L)
                .mergeRequestIids(List.of(4L))
                .createGitlabNote(false)
                .updateMrStatus(false)
                .dryRun(true)
                .verbose(true)
                .includeDraftMrs(true)
                .ignorePatterns(Arrays.asList("cli-pattern1", "cli-pattern2"))
                .build();

        envConfig = AppConfig.builder()
                .gitlabUrl("https://gitlab.env.com")
                .gitlabToken("env-token")
                .projectId(5L)
                .mergeRequestIids(List.of(6L))
                .createGitlabNote(null)
                .updateMrStatus(null)
                .dryRun(null)
                .verbose(null)
                .includeDraftMrs(null)
                .ignorePatterns(null)
                .build();
    }

    @Test
    @DisplayName("createUnifiedConfig should merge configurations with correct precedence")
    void createUnifiedConfigShouldMergeConfigurationsWithCorrectPrecedence() {
        // Given
        File configFile = new File("config.yml");
        when(configLoader.loadFromYaml(configFile)).thenReturn(fileConfig);
        when(configLoader.loadFromEnvironment()).thenReturn(envConfig);

        // When
        AppConfig result = configurationService.createUnifiedConfig(cliConfig, configFile);

        // Then
        assertThat(result).isNotNull();
        // Environment variables have highest precedence
        assertThat(result.getGitlabUrl()).isEqualTo(envConfig.getGitlabUrl());
        assertThat(result.getGitlabToken()).isEqualTo(envConfig.getGitlabToken());
        assertThat(result.getProjectId()).isEqualTo(envConfig.getProjectId());
        assertThat(result.getMergeRequestIids()).isEqualTo(envConfig.getMergeRequestIids());
        // CLI has medium precedence, and env doesn't override these (they're null in env)
        assertThat(result.getCreateGitlabNote()).isEqualTo(cliConfig.getCreateGitlabNote());
        assertThat(result.getUpdateMrStatus()).isEqualTo(cliConfig.getUpdateMrStatus());
        assertThat(result.getDryRun()).isEqualTo(cliConfig.getDryRun());
        assertThat(result.getVerbose()).isEqualTo(cliConfig.getVerbose());
        assertThat(result.getIncludeDraftMrs()).isEqualTo(cliConfig.getIncludeDraftMrs());
        assertThat(result.getIgnorePatterns()).isEqualTo(cliConfig.getIgnorePatterns());

        // Verify interactions
        verify(configLoader).loadFromYaml(configFile);
        verify(configLoader).loadFromEnvironment();
    }

    @Test
    @DisplayName("createUnifiedConfig should handle null file config")
    void createUnifiedConfigShouldHandleNullFileConfig() {
        // Given
        File configFile = new File("non-existent.yml");
        when(configLoader.loadFromYaml(configFile)).thenReturn(null);
        when(configLoader.loadFromEnvironment()).thenReturn(envConfig);

        // When
        AppConfig result = configurationService.createUnifiedConfig(cliConfig, configFile);

        // Then
        assertThat(result).isNotNull();
        // Environment variables have highest precedence
        assertThat(result.getGitlabUrl()).isEqualTo(envConfig.getGitlabUrl());
        assertThat(result.getGitlabToken()).isEqualTo(envConfig.getGitlabToken());
        assertThat(result.getProjectId()).isEqualTo(envConfig.getProjectId());
        assertThat(result.getMergeRequestIids()).isEqualTo(envConfig.getMergeRequestIids());
        // CLI has medium precedence, and env doesn't override these (they're null in env)
        assertThat(result.getCreateGitlabNote()).isEqualTo(cliConfig.getCreateGitlabNote());
        assertThat(result.getUpdateMrStatus()).isEqualTo(cliConfig.getUpdateMrStatus());
        assertThat(result.getDryRun()).isEqualTo(cliConfig.getDryRun());
        assertThat(result.getVerbose()).isEqualTo(cliConfig.getVerbose());
        assertThat(result.getIncludeDraftMrs()).isEqualTo(cliConfig.getIncludeDraftMrs());
        assertThat(result.getIgnorePatterns()).isEqualTo(cliConfig.getIgnorePatterns());

        // Verify interactions
        verify(configLoader).loadFromYaml(configFile);
        verify(configLoader).loadFromEnvironment();
    }

    @Test
    @DisplayName("createUnifiedConfig should handle null config file")
    void createUnifiedConfigShouldHandleNullConfigFile() {
        // Given
        when(configLoader.loadFromEnvironment()).thenReturn(envConfig);

        // When
        AppConfig result = configurationService.createUnifiedConfig(cliConfig, null);

        // Then
        assertThat(result).isNotNull();
        // Environment variables have highest precedence
        assertThat(result.getGitlabUrl()).isEqualTo(envConfig.getGitlabUrl());
        assertThat(result.getGitlabToken()).isEqualTo(envConfig.getGitlabToken());
        assertThat(result.getProjectId()).isEqualTo(envConfig.getProjectId());
        assertThat(result.getMergeRequestIids()).isEqualTo(envConfig.getMergeRequestIids());
        // CLI has medium precedence, and env doesn't override these (they're null in env)
        assertThat(result.getCreateGitlabNote()).isEqualTo(cliConfig.getCreateGitlabNote());
        assertThat(result.getUpdateMrStatus()).isEqualTo(cliConfig.getUpdateMrStatus());
        assertThat(result.getDryRun()).isEqualTo(cliConfig.getDryRun());
        assertThat(result.getVerbose()).isEqualTo(cliConfig.getVerbose());
        assertThat(result.getIncludeDraftMrs()).isEqualTo(cliConfig.getIncludeDraftMrs());
        assertThat(result.getIgnorePatterns()).isEqualTo(cliConfig.getIgnorePatterns());

        // Verify interactions
        verify(configLoader, never()).loadFromYaml(any());
        verify(configLoader).loadFromEnvironment();
    }

    @Test
    @DisplayName("createUnifiedConfig should handle null CLI config")
    void createUnifiedConfigShouldHandleNullCliConfig() {
        // Given
        File configFile = new File("config.yml");
        when(configLoader.loadFromYaml(configFile)).thenReturn(fileConfig);
        when(configLoader.loadFromEnvironment()).thenReturn(envConfig);

        // When
        AppConfig result = configurationService.createUnifiedConfig(null, configFile);

        // Then
        assertThat(result).isNotNull();
        // Environment variables have highest precedence
        assertThat(result.getGitlabUrl()).isEqualTo(envConfig.getGitlabUrl());
        assertThat(result.getGitlabToken()).isEqualTo(envConfig.getGitlabToken());
        assertThat(result.getProjectId()).isEqualTo(envConfig.getProjectId());
        assertThat(result.getMergeRequestIids()).isEqualTo(envConfig.getMergeRequestIids());
        // File has lowest precedence, and env doesn't override these (they're null in env)
        assertThat(result.getCreateGitlabNote()).isEqualTo(fileConfig.getCreateGitlabNote());
        assertThat(result.getUpdateMrStatus()).isEqualTo(fileConfig.getUpdateMrStatus());
        assertThat(result.getDryRun()).isEqualTo(fileConfig.getDryRun());
        assertThat(result.getVerbose()).isEqualTo(fileConfig.getVerbose());
        assertThat(result.getIncludeDraftMrs()).isEqualTo(fileConfig.getIncludeDraftMrs());
        assertThat(result.getIgnorePatterns()).isEqualTo(fileConfig.getIgnorePatterns());

        // Verify interactions
        verify(configLoader).loadFromYaml(configFile);
        verify(configLoader).loadFromEnvironment();
    }

    @Test
    @DisplayName("validateConfig should throw exception when GitLab token is missing")
    void validateConfigShouldThrowExceptionWhenGitLabTokenIsMissing() {
        // Given
        AppConfig config = AppConfig.builder()
                .gitlabUrl("https://gitlab.com")
                .gitlabToken(null)
                .projectId(123L)
                .build();

        // When/Then
        assertThatThrownBy(() -> configurationService.validateConfig(config))
                .isInstanceOf(GitLabException.class)
                .hasMessageContaining("GitLab token is required");
    }

    @Test
    @DisplayName("validateConfig should throw exception when GitLab token is empty")
    void validateConfigShouldThrowExceptionWhenGitLabTokenIsEmpty() {
        // Given
        AppConfig config = AppConfig.builder()
                .gitlabUrl("https://gitlab.com")
                .gitlabToken("")
                .projectId(123L)
                .build();

        // When/Then
        assertThatThrownBy(() -> configurationService.validateConfig(config))
                .isInstanceOf(GitLabException.class)
                .hasMessageContaining("GitLab token is required");
    }

    @Test
    @DisplayName("validateConfig should throw exception when GitLab token is invalid")
    void validateConfigShouldThrowExceptionWhenGitLabTokenIsInvalid() {
        // Given
        AppConfig config = AppConfig.builder()
                .gitlabUrl("https://gitlab.com")
                .gitlabToken("invalid-token")
                .projectId(123L)
                .build();

        // When/Then
        assertThatThrownBy(() -> configurationService.validateConfig(config))
                .isInstanceOf(GitLabException.class)
                .hasMessageContaining("Invalid GitLab token format");
    }

    @Test
    @DisplayName("validateConfig should throw exception when GitLab URL is missing")
    void validateConfigShouldThrowExceptionWhenGitLabUrlIsMissing() {
        // Given
        AppConfig config = AppConfig.builder()
                .gitlabUrl(null)
                .gitlabToken("valid-token-12345678901234567890")
                .projectId(123L)
                .build();

        // When/Then
        assertThatThrownBy(() -> configurationService.validateConfig(config))
                .isInstanceOf(GitLabException.class)
                .hasMessageContaining("GitLab URL is required");
    }

    @Test
    @DisplayName("validateConfig should throw exception when GitLab URL is empty")
    void validateConfigShouldThrowExceptionWhenGitLabUrlIsEmpty() {
        // Given
        AppConfig config = AppConfig.builder()
                .gitlabUrl("")
                .gitlabToken("valid-token-12345678901234567890")
                .projectId(123L)
                .build();

        // When/Then
        assertThatThrownBy(() -> configurationService.validateConfig(config))
                .isInstanceOf(GitLabException.class)
                .hasMessageContaining("GitLab URL is required");
    }

    @Test
    @DisplayName("validateConfig should throw exception when GitLab URL is invalid")
    void validateConfigShouldThrowExceptionWhenGitLabUrlIsInvalid() {
        // Given
        AppConfig config = AppConfig.builder()
                .gitlabUrl("invalid-url")
                .gitlabToken("valid-token-12345678901234567890")
                .projectId(123L)
                .build();

        when(inputValidator.isValidGitLabUrl("invalid-url")).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> configurationService.validateConfig(config))
                .isInstanceOf(GitLabException.class)
                .hasMessageContaining("Invalid GitLab URL format");
    }

    @Test
    @DisplayName("validateConfig should throw exception when project ID is missing")
    void validateConfigShouldThrowExceptionWhenProjectIdIsMissing() {
        // Given
        AppConfig config = AppConfig.builder()
                .gitlabUrl("https://gitlab.com")
                .gitlabToken("valid-token-12345678901234567890")
                .projectId(null)
                .build();

        when(inputValidator.isValidGitLabUrl(anyString())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> configurationService.validateConfig(config))
                .isInstanceOf(GitLabException.class)
                .hasMessageContaining("Invalid project ID");
    }

    @Test
    @DisplayName("validateConfig should throw exception when project ID is invalid")
    void validateConfigShouldThrowExceptionWhenProjectIdIsInvalid() {
        // Given
        AppConfig config = AppConfig.builder()
                .gitlabUrl("https://gitlab.com")
                .gitlabToken("valid-token-12345678901234567890")
                .projectId(123L)
                .build();

        when(inputValidator.isValidGitLabUrl(anyString())).thenReturn(true);
        when(inputValidator.isValidProjectId("123")).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> configurationService.validateConfig(config))
                .isInstanceOf(GitLabException.class)
                .hasMessageContaining("Invalid project ID format");
    }

    @Test
    @DisplayName("validateConfig should throw exception when merge request IID is invalid")
    void validateConfigShouldThrowExceptionWhenMergeRequestIidIsInvalid() {
        // Given
        AppConfig config = AppConfig.builder()
                .gitlabUrl("https://gitlab.com")
                .gitlabToken("valid-token-12345678901234567890")
                .projectId(123L)
                .mergeRequestIids(List.of(-1L))
                .build();

        when(inputValidator.isValidGitLabUrl(anyString())).thenReturn(true);
        when(inputValidator.isValidProjectId(anyString())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> configurationService.validateConfig(config))
                .isInstanceOf(GitLabException.class)
                .hasMessageContaining("Invalid merge request IID");
    }

    @Test
    @DisplayName("validateConfig should throw exception when merge request IID format is invalid")
    void validateConfigShouldThrowExceptionWhenMergeRequestIidFormatIsInvalid() {
        // Given
        AppConfig config = AppConfig.builder()
                .gitlabUrl("https://gitlab.com")
                .gitlabToken("valid-token-12345678901234567890")
                .projectId(123L)
                .mergeRequestIids(List.of(-456L))
                .build();

        when(inputValidator.isValidGitLabUrl(anyString())).thenReturn(true);
        when(inputValidator.isValidProjectId(anyString())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> configurationService.validateConfig(config))
                .isInstanceOf(GitLabException.class)
                .hasMessageContaining("Invalid merge request IID: [-456]");
    }

    @Test
    @DisplayName("validateConfig should not validate merge request IID when it's null")
    void validateConfigShouldNotValidateMergeRequestIidWhenItsNull() {
        // Given
        AppConfig config = AppConfig.builder()
                .gitlabUrl("https://gitlab.com")
                .gitlabToken("valid-token-12345678901234567890")
                .projectId(123L)
                .mergeRequestIids(null)
                .build();

        when(inputValidator.isValidGitLabUrl(anyString())).thenReturn(true);
        when(inputValidator.isValidProjectId(anyString())).thenReturn(true);

        // When/Then
        // This should not throw an exception
        configurationService.validateConfig(config);

        // Verify interactions
        verify(inputValidator).isValidGitLabUrl("https://gitlab.com");
        verify(inputValidator).isValidProjectId("123");
    }

    @Test
    @DisplayName("validateConfig should pass for valid configuration")
    void validateConfigShouldPassForValidConfiguration() {
        // Given
        AppConfig config = AppConfig.builder()
                .gitlabUrl("https://gitlab.com")
                .gitlabToken("valid-token-12345678901234567890")
                .projectId(123L)
                .mergeRequestIids(List.of(456L))
                .build();

        when(inputValidator.isValidGitLabUrl("https://gitlab.com")).thenReturn(true);
        when(inputValidator.isValidProjectId("123")).thenReturn(true);

        // When/Then
        // This should not throw an exception
        configurationService.validateConfig(config);

        // Verify interactions
        verify(inputValidator).isValidGitLabUrl("https://gitlab.com");
        verify(inputValidator).isValidProjectId("123");
    }
}
