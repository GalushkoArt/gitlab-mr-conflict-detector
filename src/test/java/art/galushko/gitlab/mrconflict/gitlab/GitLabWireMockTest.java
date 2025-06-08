package art.galushko.gitlab.mrconflict.gitlab;

import art.galushko.gitlab.mrconflict.security.CredentialService;
import art.galushko.gitlab.mrconflict.security.InputValidator;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Project;
import org.junit.jupiter.api.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for GitLab API integration using WireMock.
 * These tests simulate a GitLab API server to test both read and write operations
 * without requiring a real GitLab instance.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GitLabWireMockTest {

    private WireMockServer wireMockServer;
    private GitLabClient gitLabClient;
    private final Long PROJECT_ID = 123L;
    private final Long MERGE_REQUEST_IID = 456L;

    @BeforeAll
    void setUp() {
        // Start WireMock server
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort()
                .notifier(new ConsoleNotifier(true)));
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());


        stubFor(get(urlEqualTo("/api/v4/user"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 1, \"username\": \"test-user\", \"name\": \"Test User\"}")));

        // Create GitLab client pointing to our mock server
        gitLabClient = new GitLab4JClient(new CredentialService(), new InputValidator()).authenticate(
                "http://localhost:" + wireMockServer.port(),
                "some-very-prod-like-mock-token"
        );
        verify(getRequestedFor(urlEqualTo("/api/v4/user")));
    }

    @AfterAll
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    @DisplayName("Should get project by ID")
    void shouldGetProjectById() {
        // Given
        stubFor(get(urlEqualTo("/api/v4/projects/" + PROJECT_ID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": " + PROJECT_ID + ", \"name\": \"Test Project\", \"path\": \"test-project\", \"default_branch\": \"main\"}")));

        // When
        Project project = gitLabClient.getProject(PROJECT_ID);

        // Then
        assertThat(project).isNotNull();
        assertThat(project.getId()).isEqualTo(PROJECT_ID);
        assertThat(project.getName()).isEqualTo("Test Project");
        verify(getRequestedFor(urlEqualTo("/api/v4/projects/" + PROJECT_ID)));
    }

    @Test
    @DisplayName("Should get merge requests")
    void shouldGetMergeRequests() {
        // Given
        String state = "opened";
        stubFor(get(urlPathMatching("/api/v4/projects/" + PROJECT_ID + "/merge_requests.*"))
                .withQueryParam("state", equalTo(state))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"id\": 1, \"iid\": 1, \"title\": \"Test MR 1\"}, {\"id\": 2, \"iid\": 2, \"title\": \"Test MR 2\"}]")));

        // When
        List<MergeRequest> mergeRequests = gitLabClient.getMergeRequests(PROJECT_ID, state);

        // Then
        assertThat(mergeRequests).isNotNull();
        assertThat(mergeRequests).hasSize(2);
        assertThat(mergeRequests.get(0).getTitle()).isEqualTo("Test MR 1");
        assertThat(mergeRequests.get(1).getTitle()).isEqualTo("Test MR 2");
        verify(getRequestedFor(urlPathMatching("/api/v4/projects/" + PROJECT_ID + "/merge_requests.*"))
                .withQueryParam("state", equalTo(state)));
    }

    @Test
    @DisplayName("Should get merge request")
    void shouldGetMergeRequest() {
        // Given
        Long mergeRequestIid = 123456L;
        stubFor(get(urlEqualTo("/api/v4/projects/" + PROJECT_ID + "/merge_requests/" + mergeRequestIid))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 789, \"iid\": " + mergeRequestIid + ", \"title\": \"Test MR\"}")));

        // When
        MergeRequest mergeRequest = gitLabClient.getMergeRequest(PROJECT_ID, mergeRequestIid);

        // Then
        assertThat(mergeRequest).isNotNull();
        assertThat(mergeRequest.getIid()).isEqualTo(mergeRequestIid);
        assertThat(mergeRequest.getTitle()).isEqualTo("Test MR");
        verify(getRequestedFor(urlEqualTo("/api/v4/projects/" + PROJECT_ID + "/merge_requests/" + mergeRequestIid)));
    }

    @Test
    @DisplayName("Should update merge request status")
    void shouldUpdateMergeRequestStatus() {
        // Given
        Set<String> labels = new LinkedHashSet<>();
        labels.add("conflict");
        labels.add("conflict:MR1");

        stubFor(put(urlEqualTo("/api/v4/projects/" + PROJECT_ID + "/merge_requests/" + MERGE_REQUEST_IID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 789, \"iid\": " + MERGE_REQUEST_IID + ", \"labels\": [\"conflict\", \"conflict:MR1\"]}")));

        // When
        gitLabClient.updateMergeRequestStatus(PROJECT_ID, MERGE_REQUEST_IID, labels);

        // Then
        verify(putRequestedFor(urlEqualTo("/api/v4/projects/" + PROJECT_ID + "/merge_requests/" + MERGE_REQUEST_IID))
                .withRequestBody(containing("labels=conflict%2Cconflict%3AMR1")));
    }

    @Test
    @DisplayName("Should create merge request note")
    void shouldCreateMergeRequestNote() {
        // Given
        String noteContent = "This is a test note";

        stubFor(post(urlEqualTo("/api/v4/projects/" + PROJECT_ID + "/merge_requests/" + MERGE_REQUEST_IID + "/notes"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 1, \"body\": \"" + noteContent + "\"}")));

        // When
        gitLabClient.createMergeRequestNote(PROJECT_ID, MERGE_REQUEST_IID, noteContent);

        // Then
        verify(postRequestedFor(urlEqualTo("/api/v4/projects/" + PROJECT_ID + "/merge_requests/" + MERGE_REQUEST_IID + "/notes"))
                .withRequestBody(containing("body=" + noteContent.replace(" ", "+"))));
    }

    @Test
    @DisplayName("Should get merge request changes")
    void shouldGetMergeRequestChanges() {
        // Given
        stubFor(get(urlEqualTo("/api/v4/projects/" + PROJECT_ID + "/merge_requests/" + MERGE_REQUEST_IID + "/changes"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"changes\": [{\"old_path\": \"file1.txt\", \"new_path\": \"file1.txt\"}, {\"old_path\": \"file2.txt\", \"new_path\": \"file2.txt\"}]}")));

        // When
        var changes = gitLabClient.getMergeRequestChanges(PROJECT_ID, MERGE_REQUEST_IID);

        // Then
        assertThat(changes).isNotNull();
        assertThat(changes).hasSize(2);
        verify(getRequestedFor(urlEqualTo("/api/v4/projects/" + PROJECT_ID + "/merge_requests/" + MERGE_REQUEST_IID + "/changes")));
    }
}