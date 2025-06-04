package art.galushko.gitlab.mrconflict.config;

import art.galushko.gitlab.mrconflict.cli.ConflictDetectorOptions;
import art.galushko.gitlab.mrconflict.core.ConflictDetectionConfig;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Main configuration class for the conflict detector application.
 */
@Getter
@Builder
@EqualsAndHashCode
@Slf4j
public class ApplicationConfig {
    private final GitLabConfig gitLabConfig;
    private final ConflictDetectionConfig detectionConfig;
    private final FileFilterConfig fileFilterConfig;
    private final List<ProjectConfig> projectConfigs;
    private final boolean forcePassEnabled;

    public static ApplicationConfig loadConfiguration(ConflictDetectorOptions options) throws ConfigurationException {
        var loader = new ConfigurationLoader();

        // Try to load from config file first
        if (options.getConfigFile() != null) {
            log.info("Loading configuration from file: {}", options.getConfigFile());
            return loader.loadFromFile(options.getConfigFile());
        }

        // Try environment variables
        try {
            log.info("Loading configuration from environment variables");
            return loader.loadFromEnvironment();
        } catch (ConfigurationException e) {
            log.debug("Failed to load from environment: {}", e.getMessage());
        }

        // Build configuration from command line options
        return buildConfigurationFromOptions(options);
    }

    private static ApplicationConfig buildConfigurationFromOptions(ConflictDetectorOptions options)
            throws ConfigurationException {

        // GitLab configuration
        var gitlabBuilder = GitLabConfig.builder();

        if (options.getGitlabUrl() != null) {
            gitlabBuilder.gitlabUrl(options.getGitlabUrl());
        } else {
            throw new ConfigurationException("GitLab URL must be specified");
        }

        if (options.getGitlabToken() != null) {
            gitlabBuilder.accessToken(options.getGitlabToken());
        } else {
            throw new ConfigurationException("GitLab token must be specified");
        }

        if (options.getProjectId() != null) {
            gitlabBuilder.projectId(options.getProjectId());
        } else if (options.getProjectPath() != null) {
            gitlabBuilder.projectPath(options.getProjectPath());
        } else {
            throw new ConfigurationException("Either project ID or project path must be specified");
        }

        // Detection configuration
        var detectionBuilder = ConflictDetectionConfig.builder()
                .fetchBeforeDetection(!options.isNoFetch())
                .forcePassEnabled(options.isForcePass());

        if (options.getSensitivity() != null) {
            detectionBuilder.sensitivity(
                    ConflictDetectionConfig.DetectionSensitivity.valueOf(options.getSensitivity().toUpperCase()));
        }

        // File filter configuration
        var filterBuilder = FileFilterConfig.builder();

        if (options.getIncludePatterns() != null) {
            filterBuilder.includePatterns(options.getIncludePatterns());
        }

        if (options.getExcludePatterns() != null) {
            filterBuilder.excludePatterns(options.getExcludePatterns());
        }

        return ApplicationConfig.builder()
                .gitLabConfig(gitlabBuilder.build())
                .detectionConfig(detectionBuilder.build())
                .fileFilterConfig(filterBuilder.build())
                .forcePassEnabled(options.isForcePass())
                .build();
    }
}

