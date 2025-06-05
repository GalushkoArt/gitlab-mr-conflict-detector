package art.galushko.gitlab.mrconflict.cli;

import art.galushko.gitlab.mrconflict.config.IgnorePatternMatcher;
import art.galushko.gitlab.mrconflict.core.MultiMergeRequestConflictDetector;
import art.galushko.gitlab.mrconflict.gitlab.GitLab4JClient;
import art.galushko.gitlab.mrconflict.gitlab.GitLab4JMergeRequestService;
import art.galushko.gitlab.mrconflict.gitlab.GitLabException;
import art.galushko.gitlab.mrconflict.model.MergeRequestConflict;
import art.galushko.gitlab.mrconflict.model.MergeRequestInfo;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.util.List;
import java.util.Set;

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
public class SimpleGitLabMultiMergeRequestCommand implements Runnable {

    @CommandLine.Option(
            names = {"--gitlab-url"},
            description = "GitLab instance URL",
            required = true
    )
    private String gitlabUrl;

    @CommandLine.Option(
            names = {"--gitlab-token"},
            description = "GitLab personal access token",
            required = true
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

    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_CONFLICTS_DETECTED = 1;
    private static final int EXIT_ERROR = 2;

    @Override
    public void run() {
        try {
            configureLogging(verbose);

            log.info("Starting GitLab Multi-MR Conflict Detection");

            // Authenticate with GitLab
            var gitlabClient = new GitLab4JClient().authenticate(gitlabUrl, gitlabToken);

            // Validate GitLab connection
            if (!gitlabClient.hasProjectAccess(projectId)) {
                throw new GitLabException("No access to project " + projectId +
                        ". Check your GitLab token permissions.");
            }

            log.info("Analyzing merge requests for project ID: {}", projectId);

            // Fetch merge requests from GitLab
            var mergeRequestService = new GitLab4JMergeRequestService(gitlabClient);
            var mergeRequests = fetchMergeRequests(mergeRequestService, projectId);

            if (mergeRequests.isEmpty()) {
                log.info("No merge requests found for analysis");
                System.out.println("No merge requests found for conflict analysis.");
                System.exit(EXIT_SUCCESS);
            }

            log.info("Found {} merge requests for analysis", mergeRequests.size());

            // Get ignore patterns (empty for now, can be extended)
            var ignorePatterns = List.<String>of("ignored.txt", "ignored_dir/*");

            // Perform conflict detection
            var conflicts = performConflictDetection(mergeRequests, ignorePatterns);

            // Generate and display output
            var output = formatOutput(conflicts);
            System.out.println(output);

            // Log conflicting MR IDs
            var conflictingMrIds = getConflictingMergeRequestIds(conflicts);
            if (!conflictingMrIds.isEmpty()) {
                log.info("Merge requests with conflicts: {}", conflictingMrIds);
                log.info("These MRs should be marked with 'conflict' label");

                // Optionally update GitLab with conflict information
                if (createGitlabNote || updateMrStatus) {
                    updateGitLabWithConflicts(gitlabClient, conflictingMrIds, conflicts);
                }
            }

            // Determine exit code
            int exitCode = conflicts.isEmpty() ? EXIT_SUCCESS : EXIT_CONFLICTS_DETECTED;
            System.exit(exitCode);

        } catch (Exception e) {
            log.error("Application failed", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(EXIT_ERROR);
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

    private List<MergeRequestInfo> fetchMergeRequests(GitLab4JMergeRequestService service, Long projectId)
            throws GitLabException {

        if (mergeRequestIid != null) {
            // Fetch specific merge request
            log.info("Fetching specific merge request: {}", mergeRequestIid);
            var mr = service.getMergeRequest(projectId, mergeRequestIid);
            return List.of(mr);
        } else {
            // Fetch all open merge requests for conflict analysis
            log.info("Fetching all open merge requests for conflict analysis");
            return service.getMergeRequestsForConflictAnalysis(projectId);
        }
    }

    private List<MergeRequestConflict> performConflictDetection(List<MergeRequestInfo> mergeRequests,
                                                                List<String> ignorePatterns) {
        var ignorePatternMatcher = new IgnorePatternMatcher();
        var detector = new MultiMergeRequestConflictDetector(ignorePatternMatcher);

        return detector.detectConflicts(mergeRequests, ignorePatterns);
    }

    private String formatOutput(List<MergeRequestConflict> conflicts) {
        if (conflicts.isEmpty()) {
            return "No conflicts detected.";
        }

        var detector = new MultiMergeRequestConflictDetector(new IgnorePatternMatcher());
        return detector.formatConflicts(conflicts);
    }

    private Set<Integer> getConflictingMergeRequestIds(List<MergeRequestConflict> conflicts) {
        var detector = new MultiMergeRequestConflictDetector(new IgnorePatternMatcher());
        return detector.getConflictingMergeRequestIds(conflicts);
    }

    private void updateGitLabWithConflicts(GitLab4JClient gitlabClient, Set<Integer> conflictingMrIds,
                                           List<MergeRequestConflict> conflicts) {
        if (dryRun) {
            log.info("Dry run mode - skipping GitLab updates");
            return;
        }

        try {
            for (Integer mrId : conflictingMrIds) {
                if (createGitlabNote) {
                    createConflictNote(gitlabClient, projectId, mrId.longValue(), conflicts);
                }

                if (updateMrStatus) {
                    gitlabClient.updateMergeRequestStatus(projectId, mrId.longValue(), true);
                }
            }

        } catch (Exception e) {
            log.warn("Failed to update GitLab with conflict information: {}", e.getMessage());
        }
    }

    private void createConflictNote(GitLab4JClient gitlabClient, Long projectId, Long mergeRequestIid,
                                    List<MergeRequestConflict> conflicts) {
        try {
            // Find conflicts involving this MR
            var relevantConflicts = conflicts.stream()
                    .filter(conflict -> conflict.firstMr().id() == mergeRequestIid ||
                            conflict.secondMr().id() == mergeRequestIid)
                    .toList();

            if (!relevantConflicts.isEmpty()) {
                String noteContent = formatConflictNote(relevantConflicts, mergeRequestIid);

                gitlabClient.getNotesApi()
                        .createMergeRequestNote(projectId, mergeRequestIid, noteContent);

                log.info("Created conflict note for MR {} in project {}", mergeRequestIid, projectId);
            }

        } catch (Exception e) {
            log.warn("Failed to create conflict note for MR {}: {}", mergeRequestIid, e.getMessage());
        }
    }

    private String formatConflictNote(List<MergeRequestConflict> conflicts, Long mergeRequestIid) {
        StringBuilder note = new StringBuilder();
        note.append("## ⚠️ Merge Request Conflicts Detected\n\n");
        note.append("This merge request has conflicts with other open merge requests:\n\n");

        for (MergeRequestConflict conflict : conflicts) {
            int otherMrId = conflict.firstMr().id() == mergeRequestIid ?
                    conflict.secondMr().id() : conflict.firstMr().id();

            note.append("- **Conflict with MR").append(otherMrId).append("**: ");
            note.append(conflict.getDescription()).append("\n");
        }

        note.append("\n**Action Required:**\n");
        note.append("- Review the conflicting files\n");
        note.append("- Coordinate with other MR authors\n");
        note.append("- Consider rebasing or merging order\n\n");
        note.append("---\n");
        note.append("*Generated by GitLab Multi-MR Conflict Detector at ")
                .append(java.time.Instant.now()).append("*");

        return note.toString();
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new SimpleGitLabMultiMergeRequestCommand()).execute(args);
        System.exit(exitCode);
    }
}

