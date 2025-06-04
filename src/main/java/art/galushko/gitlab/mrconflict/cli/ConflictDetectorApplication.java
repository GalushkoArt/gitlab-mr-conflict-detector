package art.galushko.gitlab.mrconflict.cli;

import art.galushko.gitlab.mrconflict.config.ApplicationConfig;
import art.galushko.gitlab.mrconflict.core.ConflictDetectionException;
import art.galushko.gitlab.mrconflict.core.ConflictDetector;
import art.galushko.gitlab.mrconflict.git.JGitRepository;
import art.galushko.gitlab.mrconflict.gitlab.BranchManager;
import art.galushko.gitlab.mrconflict.gitlab.GitLab4JClient;
import art.galushko.gitlab.mrconflict.gitlab.GitLabException;
import art.galushko.gitlab.mrconflict.model.MergeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gitlab4j.api.models.MergeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Main application class that orchestrates the conflict detection process.
 */
@Slf4j
@RequiredArgsConstructor
public class ConflictDetectorApplication {
    private final ConflictDetectorOptions options;
    private final ApplicationConfig config;
    private final GitLab4JClient gitlabClient;
    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_CONFLICTS_DETECTED = 1;
    private static final int EXIT_ERROR = 2;

    /**
     * Runs the conflict detection application with the provided options.
     *
     * @return exit code
     */
    public int run() {
        try {
            configureLogging(options.isVerbose());

            log.info("Starting GitLab MR Conflict Detector");
            log.debug("Options: {}", options);

            // Validate repository
            validateRepository(options.getRepositoryPath());

            // Determine source and target branches
            var sourceBranch = determineSourceBranch();
            var targetBranches = determineTargetBranches(options, config);

            log.info("Detecting conflicts for source branch '{}' against targets: {}",
                    sourceBranch, targetBranches);

            // Perform conflict detection
            var results = performConflictDetection(sourceBranch, targetBranches);

            // Generate output
            var formatter = new OutputFormatter(options.getOutputFormat());
            var output = formatter.format(results);

            // Write output
            writeOutput(output, options.getOutputFile());

            // Handle GitLab integration
            if (shouldPerformGitLabIntegration()) {
                handleGitLabIntegration(results);
            }

            // Determine exit code
            boolean hasConflicts = results.stream().anyMatch(MergeResult::hasConflicts);

            if (hasConflicts && !config.isForcePassEnabled()) {
                log.warn("Conflicts detected - exiting with error code");
                return EXIT_CONFLICTS_DETECTED;
            } else {
                log.info("No conflicts detected or force pass enabled - exiting successfully");
                return EXIT_SUCCESS;
            }

        } catch (Exception e) {
            log.error("Application failed", e);
            return EXIT_ERROR;
        }
    }

    private void configureLogging(boolean verbose) {
        // Configure logging level based on verbose flag
        var rootLogger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

        if (verbose) {
            rootLogger.setLevel(ch.qos.logback.classic.Level.DEBUG);
        } else {
            rootLogger.setLevel(ch.qos.logback.classic.Level.INFO);
        }
    }

    private void validateRepository(Path repositoryPath) throws ConflictDetectionException {
        if (!Files.exists(repositoryPath)) {
            throw new ConflictDetectionException("Repository path does not exist: " + repositoryPath);
        }

        if (!Files.isDirectory(repositoryPath)) {
            throw new ConflictDetectionException("Repository path is not a directory: " + repositoryPath);
        }

        var gitRepo = new JGitRepository(config.getGitLabConfig().getAccessToken());
        if (!gitRepo.isValidRepository(repositoryPath)) {
            throw new ConflictDetectionException("Invalid Git repository: " + repositoryPath);
        }

        log.debug("Repository validation passed: {}", repositoryPath);
    }

    private String determineSourceBranch() throws ConflictDetectionException {

        if (options.getSourceBranch() != null) {
            return options.getSourceBranch();
        }

        // If merge request is specified, get source branch from GitLab
        if (options.getMergeRequestIid() != null) {
            try {
                return getMergeRequest().getSourceBranch();
            } catch (GitLabException e) {
                throw new ConflictDetectionException("Failed to get source branch from merge request", e);
            }
        }

        // Default to current branch
        try {
            var gitRepo = new JGitRepository(config.getGitLabConfig().getAccessToken());
            gitRepo.openRepository(options.getRepositoryPath());
            var currentBranch = gitRepo.getCurrentBranch();
            gitRepo.close();
            return currentBranch;
        } catch (Exception e) {
            throw new ConflictDetectionException("Failed to determine current branch", e);
        }
    }

    private MergeRequest getMergeRequest() throws GitLabException {
        return gitlabClient.getMergeRequest(config.getGitLabConfig().getProjectId(), options.getMergeRequestIid());
    }

    private List<String> determineTargetBranches(ConflictDetectorOptions options, ApplicationConfig config)
            throws ConflictDetectionException {

        if (options.getTargetBranches() != null && !options.getTargetBranches().isEmpty()) {
            return options.getTargetBranches();
        }

        // Use GitLab to determine target branches
        try {
            var branchManager = new BranchManager(gitlabClient);
            return branchManager.getTargetBranches(config.getGitLabConfig().getProjectId(), null);

        } catch (GitLabException e) {
            throw new ConflictDetectionException("Failed to determine target branches", e);
        }
    }

    private List<MergeResult> performConflictDetection(String sourceBranch,
                                                       List<String> targetBranches)
            throws ConflictDetectionException {

        var gitRepository = new JGitRepository(config.getGitLabConfig().getAccessToken());
        var detector = new ConflictDetector(gitRepository, config.getDetectionConfig());

        return detector.detectConflicts(options.getRepositoryPath(), sourceBranch, targetBranches);
    }

    private void writeOutput(String output, Path outputFile) throws Exception {
        if (outputFile != null) {
            Files.writeString(outputFile, output);
            log.info("Output written to: {}", outputFile);
        } else {
            System.out.println(output);
        }
    }

    private boolean shouldPerformGitLabIntegration() {
        return options.isCreateGitlabNote() || options.isUpdateMrStatus();
    }

    private void handleGitLabIntegration(List<MergeResult> results) throws GitLabException {

        if (options.isDryRun()) {
            log.info("Dry run mode - skipping GitLab integration");
            return;
        }

        if (options.getMergeRequestIid() == null) {
            log.warn("Merge request IID not specified - skipping GitLab integration");
            return;
        }

        if (options.isCreateGitlabNote()) {
            log.info("Creating GitLab note with conflict results");
            gitlabClient.createConflictNote(config.getGitLabConfig().getProjectId(), options.getMergeRequestIid(), results);
        }

        if (options.isUpdateMrStatus()) {
            boolean hasConflicts = results.stream().anyMatch(MergeResult::hasConflicts);
            log.info("Updating merge request status: hasConflicts={}", hasConflicts);
            gitlabClient.updateMergeRequestStatus(config.getGitLabConfig().getProjectId(), options.getMergeRequestIid(), hasConflicts);
        }
    }
}

