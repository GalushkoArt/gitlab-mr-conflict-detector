package art.galushko.gitlab.mrconflict.config;

import lombok.Setter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigLoaderTest {

    private final TestableConfigLoader configLoader = new TestableConfigLoader();

    // Subclass of ConfigLoader that allows us to override getEnvironmentVariables for testing
    @Setter
    static class TestableConfigLoader extends ConfigLoader {
        private Map<String, String> environmentVariables = Collections.emptyMap();

        @Override
        protected Map<String, String> getEnvironmentVariables() {
            return environmentVariables;
        }
    }

    @Test
    @DisplayName("loadFromYaml should return null when file is null")
    void loadFromYamlShouldReturnNullWhenFileIsNull() {
        // When
        AppConfig result = configLoader.loadFromYaml(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("loadFromYaml should return null when file does not exist")
    void loadFromYamlShouldReturnNullWhenFileDoesNotExist() {
        // Given
        File nonExistentFile = new File("non-existent-file.yml");

        // When
        AppConfig result = configLoader.loadFromYaml(nonExistentFile);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("loadFromYaml should return null when file is not a file")
    void loadFromYamlShouldReturnNullWhenFileIsNotAFile(@TempDir Path tempDir) {
        // Given
        File directory = tempDir.toFile();

        // When
        AppConfig result = configLoader.loadFromYaml(directory);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("loadFromYaml should return null when file is not valid YAML")
    void loadFromYamlShouldReturnNullWhenFileIsNotValidYaml(@TempDir Path tempDir) throws IOException {
        // Given
        File invalidYamlFile = tempDir.resolve("invalid.yml").toFile();
        try (FileWriter writer = new FileWriter(invalidYamlFile)) {
            writer.write("This is not valid YAML content");
        }

        // When
        AppConfig result = configLoader.loadFromYaml(invalidYamlFile);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("loadFromYaml should load configuration from valid YAML file")
    void loadFromYamlShouldLoadConfigurationFromValidYamlFile(@TempDir Path tempDir) throws IOException {
        // Given
        File validYamlFile = tempDir.resolve("valid.yml").toFile();
        try (FileWriter writer = new FileWriter(validYamlFile)) {
            writer.write("gitlabUrl: https://gitlab.com\n");
            writer.write("gitlabToken: abcdef1234567890\n");
            writer.write("projectId: 123\n");
            writer.write("mergeRequestIids: [456]\n");
            writer.write("createGitlabNote: true\n");
            writer.write("updateMrStatus: false\n");
            writer.write("dryRun: true\n");
            writer.write("verbose: false\n");
            writer.write("includeDraftMrs: true\n");
            writer.write("ignorePatterns:\n");
            writer.write("  - pattern1\n");
            writer.write("  - pattern2\n");
        }

        // When
        AppConfig result = configLoader.loadFromYaml(validYamlFile);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getGitlabUrl()).isEqualTo("https://gitlab.com");
        assertThat(result.getGitlabToken()).isEqualTo("abcdef1234567890");
        assertThat(result.getProjectId()).isEqualTo(123L);
        assertThat(result.getMergeRequestIids()).isEqualTo(List.of(456L));
        assertThat(result.getCreateGitlabNote()).isTrue();
        assertThat(result.getUpdateMrStatus()).isFalse();
        assertThat(result.getDryRun()).isTrue();
        assertThat(result.getVerbose()).isFalse();
        assertThat(result.getIncludeDraftMrs()).isTrue();
        assertThat(result.getIgnorePatterns()).containsExactly("pattern1", "pattern2");
    }

    @Test
    @DisplayName("loadFromEnvironment should load configuration from environment variables")
    void loadFromEnvironmentShouldLoadConfigurationFromEnvironmentVariables() {
        // Given
        Map<String, String> mockEnvVars = new HashMap<>();
        mockEnvVars.put("GITLAB_MR_GITLAB_URL", "https://gitlab.example.com");
        mockEnvVars.put("GITLAB_MR_GITLAB_TOKEN", "token12345");
        mockEnvVars.put("GITLAB_MR_PROJECT_ID", "789");
        mockEnvVars.put("GITLAB_MR_MERGE_REQUEST_IID", "101");
        mockEnvVars.put("GITLAB_MR_CREATE_GITLAB_NOTE", "true");
        mockEnvVars.put("GITLAB_MR_UPDATE_MR_STATUS", "true");
        mockEnvVars.put("GITLAB_MR_DRY_RUN", "false");
        mockEnvVars.put("GITLAB_MR_VERBOSE", "true");
        mockEnvVars.put("GITLAB_MR_INCLUDE_DRAFT_MRS", "false");
        mockEnvVars.put("OTHER_VAR", "should be ignored");
        configLoader.setEnvironmentVariables(mockEnvVars);

        // When
        AppConfig result = configLoader.loadFromEnvironment();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getGitlabUrl()).isEqualTo("https://gitlab.example.com");
        assertThat(result.getGitlabToken()).isEqualTo("token12345");
        assertThat(result.getProjectId()).isEqualTo(789L);
        assertThat(result.getMergeRequestIids()).isEqualTo(List.of(101L));
        assertThat(result.getCreateGitlabNote()).isTrue();
        assertThat(result.getUpdateMrStatus()).isTrue();
        assertThat(result.getDryRun()).isFalse();
        assertThat(result.getVerbose()).isTrue();
        assertThat(result.getIncludeDraftMrs()).isFalse();
    }

    @ParameterizedTest
    @DisplayName("loadFromEnvironment should handle invalid numeric values")
    @MethodSource("invalidNumericValuesProvider")
    void loadFromEnvironmentShouldHandleInvalidNumericValues(String projectId, String mergeRequestIid, Long expectedProjectId, List<Long> expectedMergeRequestIid) {
        // Given
        Map<String, String> mockEnvVars = new HashMap<>();
        mockEnvVars.put("GITLAB_MR_PROJECT_ID", projectId);
        mockEnvVars.put("GITLAB_MR_MERGE_REQUEST_IID", mergeRequestIid);
        configLoader.setEnvironmentVariables(mockEnvVars);

        // When
        AppConfig result = configLoader.loadFromEnvironment();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProjectId()).isEqualTo(expectedProjectId);
        assertThat(result.getMergeRequestIids()).isEqualTo(expectedMergeRequestIid);
    }

    static Stream<Arguments> invalidNumericValuesProvider() {
        return Stream.of(
            Arguments.of("not-a-number", "456", null, List.of(456L)),
            Arguments.of("123", "not-a-number", 123L, null),
            Arguments.of("not-a-number", "not-a-number", null, null),
            Arguments.of("", "", null, null),
            Arguments.of(null, null, null, null)
        );
    }

    @ParameterizedTest
    @DisplayName("loadFromEnvironment should handle boolean values correctly")
    @MethodSource("booleanValuesProvider")
    void loadFromEnvironmentShouldHandleBooleanValuesCorrectly(
            String createGitlabNote, 
            String updateMrStatus, 
            String dryRun, 
            String verbose, 
            String includeDraftMrs,
            boolean expectedCreateGitlabNote,
            boolean expectedUpdateMrStatus,
            boolean expectedDryRun,
            boolean expectedVerbose,
            boolean expectedIncludeDraftMrs) {

        // Given
        Map<String, String> mockEnvVars = new HashMap<>();
        mockEnvVars.put("GITLAB_MR_CREATE_GITLAB_NOTE", createGitlabNote);
        mockEnvVars.put("GITLAB_MR_UPDATE_MR_STATUS", updateMrStatus);
        mockEnvVars.put("GITLAB_MR_DRY_RUN", dryRun);
        mockEnvVars.put("GITLAB_MR_VERBOSE", verbose);
        mockEnvVars.put("GITLAB_MR_INCLUDE_DRAFT_MRS", includeDraftMrs);
        configLoader.setEnvironmentVariables(mockEnvVars);

        // When
        AppConfig result = configLoader.loadFromEnvironment();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCreateGitlabNote()).isEqualTo(expectedCreateGitlabNote);
        assertThat(result.getUpdateMrStatus()).isEqualTo(expectedUpdateMrStatus);
        assertThat(result.getDryRun()).isEqualTo(expectedDryRun);
        assertThat(result.getVerbose()).isEqualTo(expectedVerbose);
        assertThat(result.getIncludeDraftMrs()).isEqualTo(expectedIncludeDraftMrs);
    }

    static Stream<Arguments> booleanValuesProvider() {
        return Stream.of(
            Arguments.of("true", "true", "true", "true", "true", true, true, true, true, true),
            Arguments.of("false", "false", "false", "false", "false", false, false, false, false, false),
            Arguments.of("TRUE", "TRUE", "TRUE", "TRUE", "TRUE", true, true, true, true, true),
            Arguments.of("FALSE", "FALSE", "FALSE", "FALSE", "FALSE", false, false, false, false, false),
            Arguments.of("True", "True", "True", "True", "True", true, true, true, true, true),
            Arguments.of("False", "False", "False", "False", "False", false, false, false, false, false),
            Arguments.of("yes", "no", "1", "0", "anything", false, false, false, false, false)
        );
    }

    @Test
    @DisplayName("loadFromEnvironment should handle empty environment")
    void loadFromEnvironmentShouldHandleEmptyEnvironment() {
        // Given
        Map<String, String> emptyEnvVars = Collections.emptyMap();
        configLoader.setEnvironmentVariables(emptyEnvVars);

        // When
        AppConfig result = configLoader.loadFromEnvironment();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getGitlabUrl()).isNull();
        assertThat(result.getGitlabToken()).isNull();
        assertThat(result.getProjectId()).isNull();
        assertThat(result.getMergeRequestIids()).isNull();
        assertThat(result.getCreateGitlabNote()).isNull();
        assertThat(result.getUpdateMrStatus()).isNull();
        assertThat(result.getDryRun()).isNull();
        assertThat(result.getVerbose()).isNull();
        assertThat(result.getIncludeDraftMrs()).isNull();
    }
}
