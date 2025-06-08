package art.galushko.gitlab.mrconflict.cli;

import art.galushko.gitlab.mrconflict.core.ConflictAnalysisService;
import art.galushko.gitlab.mrconflict.gitlab.GitLabException;
import art.galushko.gitlab.mrconflict.model.MergeRequestConflict;
import art.galushko.gitlab.mrconflict.model.MergeRequestInfo;
import art.galushko.gitlab.mrconflict.security.CredentialService;
import art.galushko.gitlab.mrconflict.security.InputValidator;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Simplified command for multi-MR conflict detection with GitLab integration.
 */
@Slf4j
@CommandLine.Command(
        name = "gitlab-multi-mr-conflicts",
        description = "Detect conflicts between multiple merge requests using GitLab API",
        mixinStandardHelpOptions = true,
        version = "1.1.0"
)
public class SimpleGitLabMultiMergeRequestCommand implements Callable<Integer> {

    private final ConflictAnalysisService conflictAnalysisService;
    private final CredentialService credentialService;
    private final InputValidator inputValidator;

    @CommandLine.Option(
            names = {"--gitlab-url"},
            description = "GitLab instance URL (can also be set via GITLAB_URL environment variable)",
            required = false
    )
    private String gitlabUrl;

    @CommandLine.Option(
            names = {"--gitlab-token"},
            description = "GitLab personal access token (can also be set via GITLAB_TOKEN environment variable)",
            required = false
    )
    private String gitlabToken;

    @CommandLine.Option(
            names = {"--project-id"},
            description = "GitLab project ID",
            required = true
    )
    private Long projectId;

    @CommandLine.Option(
            names = {"--mr-iid"},
            description = "Specific merge request IID to analyze (optional)",
            required = false
    )
    private Long mergeRequestIid;

    @CommandLine.Option(
            names = {"--create-gitlab-note"},
            description = "Create notes on merge requests with conflict results",
            defaultValue = "false"
    )
    private boolean createGitlabNote;

    @CommandLine.Option(
            names = {"--update-mr-status"},
            description = "Update merge request status based on conflicts",
            defaultValue = "false"
    )
    private boolean updateMrStatus;

    @CommandLine.Option(
            names = {"--dry-run"},
            description = "Perform dry run without making changes to GitLab",
            defaultValue = "false"
    )
    private boolean dryRun;

    @CommandLine.Option(
            names = {"--verbose", "-v"},
            description = "Enable verbose logging",
            defaultValue = "false"
    )
    private boolean verbose;

    @CommandLine.Option(
            names = {"--include-draft-mrs"},
            description = "Include draft/WIP merge requests in conflict analysis",
            defaultValue = "false"
    )
    private boolean includeDraftMrs;

    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_CONFLICTS_DETECTED = 1;
    private static final int EXIT_ERROR = 2;

    public SimpleGitLabMultiMergeRequestCommand() {
        this.conflictAnalysisService = new ConflictAnalysisService();
        this.credentialService = new CredentialService();
        this.inputValidator = new InputValidator();
    }

    /**
     * Executes the command to detect conflicts between merge requests.
     * This method is called by the Picocli framework when the command is run.
     *
     * @return EXIT_SUCCESS (0) if no conflicts are found,
     *         EXIT_CONFLICTS_DETECTED (1) if conflicts are found,
     *         EXIT_ERROR (2) if an error occurs
     */
    @Override
    public Integer call() {
        try {
            configureLogging(verbose);

            log.info("Starting GitLab Multi-MR Conflict Detection");

            // Validate inputs
            validateInputs();

            // Authenticate with GitLab
            conflictAnalysisService.authenticate(gitlabUrl, gitlabToken);

            // Validate GitLab connection
            if (!conflictAnalysisService.hasProjectAccess(projectId)) {
                throw new GitLabException("No access to project " + projectId +
                        ". Check your GitLab token permissions.");
            }

            log.info("Analyzing merge requests for project ID: {}", projectId);

            // Fetch merge requests from GitLab
            List<MergeRequestInfo> mergeRequests = conflictAnalysisService.fetchMergeRequests(projectId, mergeRequestIid, includeDraftMrs);

            if (mergeRequests.isEmpty()) {
                log.info("No merge requests found for analysis");
                System.out.println("No merge requests found for conflict analysis.");
                return EXIT_SUCCESS;
            }

            log.info("Found {} merge requests for analysis", mergeRequests.size());

            // Get to ignore patterns (empty for now, can be extended)
            List<String> ignorePatterns = List.of("**/ignored.txt", "ignored_dir/*");

            // Perform conflict detection
            List<MergeRequestConflict> conflicts = conflictAnalysisService.detectConflicts(mergeRequests, ignorePatterns);

            // Generate and display output
            String output = conflictAnalysisService.formatConflicts(conflicts);
            System.out.println(output);

            // Log conflicting MR IDs
            var conflictingMrIds = conflictAnalysisService.getConflictingMergeRequestIds(conflicts);
            if (!conflictingMrIds.isEmpty()) {
                log.info("Merge requests with conflicts: {}", conflictingMrIds);
                log.info("These MRs should be marked with 'conflict' label");
            } else {
                log.info("No conflicts found at this moment");
            }
            // Optionally update GitLab with conflict information
            if (createGitlabNote || updateMrStatus) {
                conflictAnalysisService.updateGitLabWithConflicts(
                        projectId, conflicts, createGitlabNote, updateMrStatus, dryRun
                );
            }

            // Determine exit code
            return conflicts.isEmpty() ? EXIT_SUCCESS : EXIT_CONFLICTS_DETECTED;

        } catch (Exception e) {
            log.error("Application failed", e);
            System.err.println("Error: " + e.getMessage());
            return EXIT_ERROR;
        }
    }

    private void configureLogging(boolean verbose) {
        var rootLogger =
                (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

        if (verbose) {
            rootLogger.setLevel(ch.qos.logback.classic.Level.DEBUG);
        } else {
            rootLogger.setLevel(ch.qos.logback.classic.Level.INFO);
        }
    }

    /**
     * Validates command-line inputs before processing.
     * 
     * @throws GitLabException if any input is invalid
     */
    private void validateInputs() throws GitLabException {
        // Get credentials from environment variables if available
        String token = credentialService.getGitLabToken(gitlabToken);
        String url = credentialService.getGitLabUrl(gitlabUrl);

        // Check if we have a token (either from CLI or environment)
        if (token == null || token.trim().isEmpty()) {
            throw new GitLabException("GitLab token is required. Provide it with --gitlab-token or set GITLAB_TOKEN environment variable.");
        }

        // Validate token format
        if (!credentialService.isValidToken(token)) {
            throw new GitLabException("Invalid GitLab token format");
        }

        // Check if we have a URL (either from CLI or environment)
        if (url == null || url.trim().isEmpty()) {
            throw new GitLabException("GitLab URL is required. Provide it with --gitlab-url or set GITLAB_URL environment variable.");
        }

        // Validate URL format
        if (!inputValidator.isValidGitLabUrl(url)) {
            throw new GitLabException("Invalid GitLab URL format: " + url);
        }

        // Validate project ID
        if (projectId == null || projectId <= 0) {
            throw new GitLabException("Invalid project ID: " + projectId);
        }

        // Validate project ID format
        if (!inputValidator.isValidProjectId(projectId.toString())) {
            throw new GitLabException("Invalid project ID format: " + projectId);
        }

        // Validate merge request IID if provided
        if (mergeRequestIid != null) {
            if (mergeRequestIid <= 0) {
                throw new GitLabException("Invalid merge request IID: " + mergeRequestIid);
            }

            if (!inputValidator.isValidMergeRequestIid(mergeRequestIid.toString())) {
                throw new GitLabException("Invalid merge request IID format: " + mergeRequestIid);
            }
        }
    }

    /**
     * The main entry point for the application.
     * Creates a new command instance and executes it with the provided arguments.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new SimpleGitLabMultiMergeRequestCommand()).execute(args);
        System.exit(exitCode);
    }
}
