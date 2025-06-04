package art.galushko.gitlab.mrconflict.cli;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Options for the conflict detector application.
 */
@Getter
@Builder
@EqualsAndHashCode
public class ConflictDetectorOptions {
    private final Path repositoryPath;
    private final String sourceBranch;
    private final List<String> targetBranches;
    private final Path configFile;
    private final String gitlabUrl;
    private final String gitlabToken;
    private final Long projectId;
    private final String projectPath;
    private final Long mergeRequestIid;
    private final List<String> includePatterns;
    private final List<String> excludePatterns;
    private final String sensitivity;
    private final boolean forcePass;
    private final boolean noFetch;
    private final String outputFormat;
    private final Path outputFile;
    private final boolean createGitlabNote;
    private final boolean updateMrStatus;
    private final boolean verbose;
    private final boolean dryRun;
}

