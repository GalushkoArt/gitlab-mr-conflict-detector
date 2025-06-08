package art.galushko.gitlab.mrconflict.cli;

import art.galushko.gitlab.mrconflict.config.AppConfig;
import art.galushko.gitlab.mrconflict.config.ConfigurationService;
import art.galushko.gitlab.mrconflict.core.ConflictAnalysisService;
import art.galushko.gitlab.mrconflict.di.ServiceFactory;
import art.galushko.gitlab.mrconflict.gitlab.GitLabException;
import art.galushko.gitlab.mrconflict.model.MergeRequestConflict;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.File;
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

    private final ConfigurationService configurationService;

    @CommandLine.Option(
            names = {"--gitlab-url"},
            description = "GitLab instance URL (can also be set via GITLAB_URL environment variable)"
    )
    private String gitlabUrl;

    @CommandLine.Option(
            names = {"--gitlab-token"},
            description = "GitLab personal access token (can also be set via GITLAB_TOKEN environment variable)"
    )
    private String gitlabToken;

    @CommandLine.Option(
            names = {"--project-id"},
            description = "GitLab project ID"
    )
    private Long projectId;

    @CommandLine.Option(
            names = {"--mr-iids"},
            description = "Specific merge requests IID to analyze (optional)",
            split = ","
    )
    private List<Long> mergeRequestIids;

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

    @CommandLine.Option(
            names = {"--ignore-patterns"},
            description = "Patterns for files/directories to ignore in conflict detection (comma-separated)",
            split = ","
    )
    private List<String> ignorePatterns;

    @CommandLine.Option(
            names = {"--config-file"},
            description = "Path to YAML configuration file"
    )
    private File configFile;

    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_CONFLICTS_DETECTED = 1;
    private static final int EXIT_ERROR = 2;

    public SimpleGitLabMultiMergeRequestCommand() {
        this.configurationService = new ConfigurationService();
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

            // Create CLI config from command-line arguments
            var cliConfig = AppConfig.builder()
                    .gitlabUrl(gitlabUrl)
                    .gitlabToken(gitlabToken)
                    .projectId(projectId)
                    .mergeRequestIids(mergeRequestIids)
                    .createGitlabNote(createGitlabNote)
                    .updateMrStatus(updateMrStatus)
                    .dryRun(dryRun)
                    .verbose(verbose)
                    .includeDraftMrs(includeDraftMrs)
                    .ignorePatterns(ignorePatterns)
                    .build();

            // Create unified configuration
            AppConfig config = configurationService.createUnifiedConfig(cliConfig, configFile);

            // Validate configuration
            configurationService.validateConfig(config);
            ServiceFactory.provideConfig(config);
            var conflictAnalysisService = new ConflictAnalysisService();

            // Authenticate with GitLab
            conflictAnalysisService.authenticate();

            // Validate GitLab connection
            if (!conflictAnalysisService.hasAccessToProjectFromConfig()) {
                throw new GitLabException("No access to project " + config.getProjectId() +
                        ". Check your GitLab token permissions.");
            }

            log.info("Analyzing merge requests for project ID: {}", config.getProjectId());

            log.debug("Fetching merge requests from GitLab...");
            // Fetch merge requests from GitLab
            var mergeRequests = conflictAnalysisService.fetchMergeRequests(
            );

            if (mergeRequests.isEmpty()) {
                log.info("No merge requests found for analysis");
                return EXIT_SUCCESS;
            }

            log.info("Found {} merge requests for analysis", mergeRequests.size());

            // Perform conflict detection
            log.info("Analyzing merge requests for conflicts...");
            List<MergeRequestConflict> conflicts = conflictAnalysisService.detectConflicts(
                    mergeRequests);

            // Generate and display output
            String output = conflictAnalysisService.formatConflicts(conflicts);
            log.info(output);

            // Log conflicting MR IDs
            var conflictingMrIds = conflictAnalysisService.getConflictingMergeRequestIds(conflicts);
            if (!conflictingMrIds.isEmpty()) {
                log.info("Merge requests with conflicts: {}", conflictingMrIds);
                log.info("These MRs should be marked with 'conflict' label");
            } else {
                log.info("No conflicts found at this moment");
            }

            // Optionally update GitLab with conflict information
            if (config.getCreateGitlabNote() || config.getUpdateMrStatus()) {
                log.info("Updating GitLab with conflict information");

                conflictAnalysisService.updateGitLabWithConflicts(
                        conflicts
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

    /**
     * Configures the logging level based on the verbose flag.
     * If verbose is true, sets the logging level to DEBUG.
     * Otherwise, sets the logging level to INFO.
     *
     * @param verbose whether to enable verbose logging
     */
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
