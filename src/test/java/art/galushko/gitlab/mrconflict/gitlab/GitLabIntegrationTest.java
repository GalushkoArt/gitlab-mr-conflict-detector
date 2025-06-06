package art.galushko.gitlab.mrconflict.gitlab;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for GitLab API integration.
 * These tests require a GitLab instance to be available and configured via environment variables.
 * They will be skipped if the required environment variables are not set.
 */
class GitLabIntegrationTest {

    private static final String ENV_GITLAB_URL = "GITLAB_TEST_URL";
    private static final String ENV_GITLAB_TOKEN = "GITLAB_TEST_TOKEN";
    private static final String ENV_GITLAB_PROJECT_ID = "GITLAB_TEST_PROJECT_ID";

    private String gitlabUrl;
    private String gitlabToken;
    private Long projectId;
    private GitLabClient gitLabClient;

    @BeforeEach
    void setUp() throws GitLabException {
        // Get configuration from environment variables
        gitlabUrl = System.getenv(ENV_GITLAB_URL);
        gitlabToken = System.getenv(ENV_GITLAB_TOKEN);
        String projectIdStr = System.getenv(ENV_GITLAB_PROJECT_ID);

        // Skip tests if environment variables are not set
        assumeTrue(gitlabUrl != null && !gitlabUrl.isEmpty(), 
                "GitLab URL environment variable not set: " + ENV_GITLAB_URL);
        assumeTrue(gitlabToken != null && !gitlabToken.isEmpty(), 
                "GitLab token environment variable not set: " + ENV_GITLAB_TOKEN);
        assumeTrue(projectIdStr != null && !projectIdStr.isEmpty(), 
                "GitLab project ID environment variable not set: " + ENV_GITLAB_PROJECT_ID);

        projectId = Long.parseLong(projectIdStr);

        // Create and authenticate the GitLab client
        gitLabClient = new GitLab4JClient().authenticate(gitlabUrl, gitlabToken);
    }

    @Test
    @DisplayName("Should authenticate and access GitLab API")
    @EnabledIfEnvironmentVariable(named = ENV_GITLAB_URL, matches = ".+")
    @EnabledIfEnvironmentVariable(named = ENV_GITLAB_TOKEN, matches = ".+")
    void shouldAuthenticateAndAccessGitLabApi() throws GitLabException {
        // When
        boolean hasAccess = gitLabClient.hasProjectAccess(projectId);

        // Then
        assertThat(hasAccess).isTrue();
    }

    @Test
    @DisplayName("Should get project information")
    @EnabledIfEnvironmentVariable(named = ENV_GITLAB_URL, matches = ".+")
    @EnabledIfEnvironmentVariable(named = ENV_GITLAB_TOKEN, matches = ".+")
    @EnabledIfEnvironmentVariable(named = ENV_GITLAB_PROJECT_ID, matches = ".+")
    void shouldGetProjectInformation() throws GitLabException {
        // When
        var project = gitLabClient.getProject(projectId);

        // Then
        assertThat(project).isNotNull();
        assertThat(project.getId()).isEqualTo(projectId);
    }

    @Test
    @DisplayName("Should get branches")
    @EnabledIfEnvironmentVariable(named = ENV_GITLAB_URL, matches = ".+")
    @EnabledIfEnvironmentVariable(named = ENV_GITLAB_TOKEN, matches = ".+")
    @EnabledIfEnvironmentVariable(named = ENV_GITLAB_PROJECT_ID, matches = ".+")
    void shouldGetBranches() throws GitLabException {
        // When
        var branches = gitLabClient.getBranches(projectId);

        // Then
        assertThat(branches).isNotNull();
        assertThat(branches).isNotEmpty();
    }

    @Test
    @DisplayName("Should get default branch")
    @EnabledIfEnvironmentVariable(named = ENV_GITLAB_URL, matches = ".+")
    @EnabledIfEnvironmentVariable(named = ENV_GITLAB_TOKEN, matches = ".+")
    @EnabledIfEnvironmentVariable(named = ENV_GITLAB_PROJECT_ID, matches = ".+")
    void shouldGetDefaultBranch() throws GitLabException {
        // When
        var defaultBranch = gitLabClient.getDefaultBranch(projectId);

        // Then
        assertThat(defaultBranch).isNotNull();
        assertThat(defaultBranch).isNotEmpty();
    }

    @Test
    @DisplayName("Should get merge requests")
    @EnabledIfEnvironmentVariable(named = ENV_GITLAB_URL, matches = ".+")
    @EnabledIfEnvironmentVariable(named = ENV_GITLAB_TOKEN, matches = ".+")
    @EnabledIfEnvironmentVariable(named = ENV_GITLAB_PROJECT_ID, matches = ".+")
    void shouldGetMergeRequests() throws GitLabException {
        // When
        var mergeRequests = gitLabClient.getMergeRequests(projectId, "opened");

        // Then
        assertThat(mergeRequests).isNotNull();
        // Note: The project might not have any open merge requests
    }
}
