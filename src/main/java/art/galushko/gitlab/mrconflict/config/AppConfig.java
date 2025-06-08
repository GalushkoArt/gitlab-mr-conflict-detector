package art.galushko.gitlab.mrconflict.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Configuration class for the application.
 * Holds all configuration options from various sources (CLI, environment variables, YAML files).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppConfig {
    // GitLab connection settings
    private String gitlabUrl;
    private String gitlabToken;
    private Long projectId;
    private List<Long> mergeRequestIids;

    // Behavior settings
    private Boolean createGitlabNote;
    private Boolean updateMrStatus;
    private Boolean dryRun;
    private Boolean verbose;
    private Boolean includeDraftMrs;

    // Conflict detection settings
    private List<String> ignorePatterns;
}
