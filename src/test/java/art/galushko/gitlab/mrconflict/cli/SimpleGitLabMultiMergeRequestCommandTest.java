package art.galushko.gitlab.mrconflict.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.parallel.Isolated;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for SimpleGitLabMultiMergeRequestCommand.
 * These tests require a GitLab instance to be available and configured via environment variables.
 * They will be skipped if the required environment variables are not set.
 */
class SimpleGitLabMultiMergeRequestCommandTest {

    private static final String ENV_GITLAB_URL = "GITLAB_TEST_URL";
    private static final String ENV_GITLAB_TOKEN = "GITLAB_TEST_TOKEN";
    private static final String ENV_GITLAB_PROJECT_ID = "GITLAB_TEST_PROJECT_ID";

    @Test
    @DisplayName("Should display help information")
    void shouldDisplayHelpInformation() {
        // Given
        StringWriter outWriter = new StringWriter();
        PrintWriter outPrintWriter = new PrintWriter(outWriter);

        // When
        var commandLine = new CommandLine(new SimpleGitLabMultiMergeRequestCommand());
        commandLine.setOut(outPrintWriter);
        int exitCode = commandLine.execute("--help");

        // Then
        String output = outWriter.toString();
        assertThat(output).contains("Usage:");
        assertThat(output).contains("gitlab-multi-mr-conflicts");
        assertThat(output).contains("--gitlab-url");
        assertThat(output).contains("--gitlab-token");
        assertThat(output).contains("--project-id");
        assertThat(exitCode).isEqualTo(0);
    }

    // Integration test moved to SimpleGitLabMultiMergeRequestCommandIntegrationTest

    @Test
    @DisplayName("Should fail with missing required arguments")
    void shouldFailWithMissingRequiredArguments() {
        // Given
        StringWriter errWriter = new StringWriter();
        PrintWriter errPrintWriter = new PrintWriter(errWriter);

        // When
        var commandLine = new CommandLine(new SimpleGitLabMultiMergeRequestCommand());
        commandLine.setErr(errPrintWriter);
        int exitCode = commandLine.execute();

        // Then
        String output = errWriter.toString();
        assertThat(output).contains("Missing required option");
        assertThat(exitCode).isNotEqualTo(0);
    }
}
