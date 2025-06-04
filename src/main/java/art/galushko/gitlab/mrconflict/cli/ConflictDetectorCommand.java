package art.galushko.gitlab.mrconflict.cli;

import art.galushko.gitlab.mrconflict.gitlab.GitLab4JClient;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import static art.galushko.gitlab.mrconflict.config.ApplicationConfig.loadConfiguration;

/**
 * Command line interface for the GitLab Merge Request Conflict Detector.
 */
@Command(
        name = "gitlab-mr-conflict-detector",
        description = "Detects merge conflicts in GitLab merge requests",
        version = "1.0.0",
        mixinStandardHelpOptions = true
)
public class ConflictDetectorCommand implements Callable<Integer> {

    @Parameters(
            index = "0",
            description = "Path to the Git repository",
            arity = "0..1"
    )
    private Path repositoryPath = Path.of("C:\\Users\\1\\tmp\\galushkoart-project");

    @Option(
            names = {"-s", "--source-branch"},
            description = "Source branch for conflict detection",
            required = false
    )
    private String sourceBranch;

    @Option(
            names = {"-t", "--target-branches"},
            description = "Target branches to check conflicts against (comma-separated)",
            split = ","
    )
    private List<String> targetBranches;

    @Option(
            names = {"-c", "--config"},
            description = "Path to configuration file"
    )
    private Path configFile;

    @Option(
            names = {"--gitlab-url"},
            description = "GitLab instance URL"
    )
    private String gitlabUrl;

    @Option(
            names = {"--gitlab-token"},
            description = "GitLab access token"
    )
    private String gitlabToken;

    @Option(
            names = {"--project-id"},
            description = "GitLab project ID"
    )
    private Long projectId;

    @Option(
            names = {"--project-path"},
            description = "GitLab project path (namespace/project-name)"
    )
    private String projectPath;

    @Option(
            names = {"--mr-iid"},
            description = "Merge request internal ID to analyze"
    )
    private Long mergeRequestIid;

    @Option(
            names = {"--include-patterns"},
            description = "File patterns to include (glob syntax, comma-separated)",
            split = ","
    )
    private List<String> includePatterns;

    @Option(
            names = {"--exclude-patterns"},
            description = "File patterns to exclude (glob syntax, comma-separated)",
            split = ","
    )
    private List<String> excludePatterns;

    @Option(
            names = {"--sensitivity"},
            description = "Detection sensitivity: STRICT, NORMAL, PERMISSIVE",
            defaultValue = "NORMAL"
    )
    private String sensitivity;

    @Option(
            names = {"--force-pass"},
            description = "Force pass conflicts (override detection results)"
    )
    private boolean forcePass;

    @Option(
            names = {"--no-fetch"},
            description = "Skip fetching latest changes from remote"
    )
    private boolean noFetch;

    @Option(
            names = {"--output-format"},
            description = "Output format: TEXT, JSON, YAML",
            defaultValue = "TEXT"
    )
    private String outputFormat;

    @Option(
            names = {"--output-file"},
            description = "Output file path (default: stdout)"
    )
    private Path outputFile;

    @Option(
            names = {"--create-gitlab-note"},
            description = "Create a note on the merge request with results"
    )
    private boolean createGitlabNote;

    @Option(
            names = {"--update-mr-status"},
            description = "Update merge request status based on conflicts"
    )
    private boolean updateMrStatus;

    @Option(
            names = {"-v", "--verbose"},
            description = "Enable verbose logging"
    )
    private boolean verbose;

    @Option(
            names = {"--dry-run"},
            description = "Perform dry run without making changes"
    )
    private boolean dryRun;

    @Override
    public Integer call() throws Exception {
        var options = ConflictDetectorOptions.builder()
                .repositoryPath(repositoryPath)
                .sourceBranch(sourceBranch)
                .targetBranches(targetBranches)
                .configFile(configFile)
                .gitlabUrl(gitlabUrl)
                .gitlabToken(gitlabToken)
                .projectId(projectId)
                .projectPath(projectPath)
                .mergeRequestIid(mergeRequestIid)
                .includePatterns(includePatterns)
                .excludePatterns(excludePatterns)
                .sensitivity(sensitivity)
                .forcePass(forcePass)
                .noFetch(noFetch)
                .outputFormat(outputFormat)
                .outputFile(outputFile)
                .createGitlabNote(createGitlabNote)
                .updateMrStatus(updateMrStatus)
                .verbose(verbose)
                .dryRun(dryRun)
                .build();

        var config = loadConfiguration(options);
        var gitlabClient = new GitLab4JClient()
                .authenticate(config.getGitLabConfig().getGitlabUrl(), config.getGitLabConfig().getAccessToken());

        var app = new ConflictDetectorApplication(options, config, gitlabClient);

        return app.run();
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new ConflictDetectorCommand()).execute(args);
        System.exit(exitCode);
    }
}

