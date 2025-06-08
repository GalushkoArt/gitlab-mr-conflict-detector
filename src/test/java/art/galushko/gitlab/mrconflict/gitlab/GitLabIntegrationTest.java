package art.galushko.gitlab.mrconflict.gitlab;

import art.galushko.gitlab.mrconflict.di.ServiceFactory;
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
@EnabledIfEnvironmentVariable(named = "GITLAB_TEST_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "GITLAB_TEST_TOKEN", matches = ".+")
@EnabledIfEnvironmentVariable(named = "GITLAB_TEST_PROJECT_ID", matches = ".+")
class GitLabIntegrationTest {

    private static final String ENV_GITLAB_URL = "GITLAB_TEST_URL";
    private static final String ENV_GITLAB_TOKEN = "GITLAB_TEST_TOKEN";
    private static final String ENV_GITLAB_PROJECT_ID = "GITLAB_TEST_PROJECT_ID";

    private Long projectId;
    private GitLabClient gitLabClient;

    @BeforeEach
    void setUp() throws GitLabException {
        // Get configuration from environment variables
        String gitlabUrl = System.getenv(ENV_GITLAB_URL);
        String gitlabToken = System.getenv(ENV_GITLAB_TOKEN);
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
        gitLabClient = ServiceFactory.getInstance().getGitLabClient().authenticate(gitlabUrl, gitlabToken);
    }

    @Test
    @DisplayName("Should authenticate and access GitLab API")
    void shouldAuthenticateAndAccessGitLabApi() throws GitLabException {
        // When
        boolean hasAccess = gitLabClient.hasProjectAccess(projectId);

        // Then
        assertThat(hasAccess).isTrue();
    }

    @Test
    @DisplayName("Should get project information")
    void shouldGetProjectInformation() throws GitLabException {
        // When
        var project = gitLabClient.getProject(projectId);

        // Then
        assertThat(project).isNotNull();
        assertThat(project.getId()).isEqualTo(projectId);
    }

    @Test
    @DisplayName("Should get merge requests")
    void shouldGetMergeRequests() throws GitLabException {
        // When
        var mergeRequests = gitLabClient.getMergeRequests(projectId, "opened");

        // Then
        assertThat(mergeRequests).isNotNull();
        // Note: The project might not have any open merge requests
    }

    @Test
    @DisplayName("Should get merge request by iid")
    void shouldGetMergeRequest() throws GitLabException {
        // When
        var mergeRequests = gitLabClient.getMergeRequests(projectId, "opened");

        // Then
        assertThat(mergeRequests).as("Please setup test repo. Some opened MR is expected").isNotEmpty();
        // Note: The project might not have any open merge requests

        var mergeRequest = mergeRequests.getFirst();
        var mergeRequestInfo = gitLabClient.getMergeRequest(projectId, mergeRequest.getIid());
        assertThat(mergeRequestInfo).isNotNull();
    }

    @Test
    @DisplayName("Should get merge request changes by iid")
    void shouldGetMergeRequestChanges() throws GitLabException {
        // When
        var mergeRequests = gitLabClient.getMergeRequests(projectId, "opened");

        // Then
        assertThat(mergeRequests).as("Please setup test repo. Some opened MR is expected").isNotEmpty();
        // Note: The project might not have any open merge requests

        var mergeRequest = mergeRequests.getFirst();
        var mrChanges = gitLabClient.getMergeRequestChanges(projectId, mergeRequest.getIid());
        assertThat(mrChanges).isNotEmpty();
    }
}
