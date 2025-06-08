package art.galushko.gitlab.mrconflict.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ConflictDetectorApplication.
 * These tests require a GitLab instance to be available and configured via environment variables.
 * They will be skipped if the required environment variables are not set.
 */
class ConflictDetectorApplicationTest {

    @Test
    @DisplayName("Should display help information")
    void shouldDisplayHelpInformation() {
        // Given
        StringWriter outWriter = new StringWriter();
        PrintWriter outPrintWriter = new PrintWriter(outWriter);

        // When
        var commandLine = new CommandLine(new ConflictDetectorApplication());
        commandLine.setOut(outPrintWriter);
        int exitCode = commandLine.execute("--help");

        // Then
        String output = outWriter.toString();
        assertThat(output).contains("Usage:");
        assertThat(output).contains("gitlab-multi-mr-conflicts");
        assertThat(output).contains("--gitlab-url");
        assertThat(output).contains("--gitlab-token");
        assertThat(output).contains("--project-id");
        assertThat(output).contains("--ignore-patterns");
        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    @DisplayName("Should validate GitLab token is required")
    void shouldValidateGitLabTokenIsRequired() {
        // Given
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errContent));

        // When
        var commandLine = new CommandLine(new ConflictDetectorApplication());
        int exitCode = commandLine.execute(
                "--gitlab-url", "https://gitlab.com",
                "--project-id", "123"
        );

        // Then
        assertThat(exitCode).isEqualTo(2); // EXIT_ERROR
        assertThat(errContent.toString()).contains("GitLab token is required");
    }

    @Test
    @DisplayName("Should validate GitLab token format")
    void shouldValidateGitLabTokenFormat() {
        // Given
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errContent));

        // When
        var commandLine = new CommandLine(new ConflictDetectorApplication());
        int exitCode = commandLine.execute(
                "--gitlab-url", "https://gitlab.com",
                "--gitlab-token", "invalid!@#$%^&*()", // Invalid characters in token
                "--project-id", "123"
        );

        // Then
        assertThat(exitCode).isEqualTo(2); // EXIT_ERROR
        assertThat(errContent.toString()).contains("Invalid GitLab token format");
    }

    @Test
    @DisplayName("Should validate GitLab URL is required")
    void shouldValidateGitLabUrlIsRequired() {
        // Given
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errContent));

        // When
        var commandLine = new CommandLine(new ConflictDetectorApplication());
        int exitCode = commandLine.execute(
                "--gitlab-token", "glpat-valid-token-format",
                "--project-id", "123"
        );

        // Then
        assertThat(exitCode).isEqualTo(2); // EXIT_ERROR
        assertThat(errContent.toString()).contains("GitLab URL is required");
    }

    @Test
    @DisplayName("Should validate GitLab URL format")
    void shouldValidateGitLabUrlFormat() {
        // Given
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errContent));

        // When
        var commandLine = new CommandLine(new ConflictDetectorApplication());
        int exitCode = commandLine.execute(
                "--gitlab-url", "invalid-url",
                "--gitlab-token", "glpat-valid-token-format",
                "--project-id", "123"
        );

        // Then
        assertThat(exitCode).isEqualTo(2); // EXIT_ERROR
        assertThat(errContent.toString()).contains("Invalid GitLab URL format");
    }

    @Test
    @DisplayName("Should validate project ID is valid")
    void shouldValidateProjectIdIsValid() {
        // Given
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errContent));

        // When
        var commandLine = new CommandLine(new ConflictDetectorApplication());
        int exitCode = commandLine.execute(
                "--gitlab-url", "https://gitlab.com",
                "--gitlab-token", "glpat-valid-token-format",
                "--project-id", "-1"
        );

        // Then
        assertThat(exitCode).isEqualTo(2); // EXIT_ERROR
        assertThat(errContent.toString()).contains("Invalid project ID");
    }

    @Test
    @DisplayName("Should validate merge request IID is valid")
    void shouldValidateMergeRequestIidIsValid() {
        // Given
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errContent));

        // When
        var commandLine = new CommandLine(new ConflictDetectorApplication());
        int exitCode = commandLine.execute(
                "--gitlab-url", "https://gitlab.com",
                "--gitlab-token", "glpat-valid-token-format",
                "--project-id", "123",
                "--mr-iids", "-1"
        );

        // Then
        assertThat(exitCode).isEqualTo(2); // EXIT_ERROR
        assertThat(errContent.toString()).contains("Invalid merge request IID");
    }
}
