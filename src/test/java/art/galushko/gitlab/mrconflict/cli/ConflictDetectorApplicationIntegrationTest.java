package art.galushko.gitlab.mrconflict.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for ConflictDetectorApplication that require a GitLab instance.
 * These tests are separated from the basic tests to avoid interference.
 */
class ConflictDetectorApplicationIntegrationTest {

    private static final String ENV_GITLAB_URL = "GITLAB_TEST_URL";
    private static final String ENV_GITLAB_TOKEN = "GITLAB_TEST_TOKEN";
    private static final String ENV_GITLAB_PROJECT_ID = "GITLAB_TEST_PROJECT_ID";

    @Test
    @DisplayName("Should execute command with valid arguments")
    @EnabledIfEnvironmentVariable(named = ENV_GITLAB_URL, matches = ".+")
    @EnabledIfEnvironmentVariable(named = ENV_GITLAB_TOKEN, matches = ".+")
    @EnabledIfEnvironmentVariable(named = ENV_GITLAB_PROJECT_ID, matches = ".+")
    void shouldExecuteCommandWithValidArguments() {
        // Get configuration from environment variables
        String gitlabUrl = System.getenv(ENV_GITLAB_URL);
        String gitlabToken = System.getenv(ENV_GITLAB_TOKEN);
        String projectId = System.getenv(ENV_GITLAB_PROJECT_ID);

        // Skip test if environment variables are not set
        assumeTrue(gitlabUrl != null && !gitlabUrl.isEmpty(),
                "GitLab URL environment variable not set: " + ENV_GITLAB_URL);
        assumeTrue(gitlabToken != null && !gitlabToken.isEmpty(),
                "GitLab token environment variable not set: " + ENV_GITLAB_TOKEN);
        assumeTrue(projectId != null && !projectId.isEmpty(),
                "GitLab project ID environment variable not set: " + ENV_GITLAB_PROJECT_ID);

        // Given
        StringWriter outWriter = new StringWriter();
        PrintWriter outPrintWriter = new PrintWriter(outWriter);

        // When
        var commandLine = new CommandLine(new ConflictDetectorApplication());
        commandLine.setOut(outPrintWriter);
        commandLine.setErr(outPrintWriter);

        int exitCode = commandLine.execute(
                "--gitlab-url", gitlabUrl,
                "--gitlab-token", gitlabToken,
                "--project-id", projectId,
                "--dry-run"
        );

        // Then
        // The output will depend on the state of the GitLab project
        // We just check that the command executed without errors
        assertThat(exitCode).isIn(0, 1); // 0 = no conflicts, 1 = conflicts detected
    }
}