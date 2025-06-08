package art.galushko.gitlab.mrconflict.config;

import art.galushko.gitlab.mrconflict.gitlab.GitLabException;
import art.galushko.gitlab.mrconflict.security.InputValidator;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * Service for managing application configuration.
 * Handles loading configuration from various sources and merging them with the correct precedence.
 */
@Slf4j
public class ConfigurationService {
    private final ConfigLoader configLoader;
    private final InputValidator inputValidator;

    /**
     * Creates a new ConfigurationService with default dependencies.
     */
    public ConfigurationService() {
        this.configLoader = new ConfigLoader();
        this.inputValidator = new InputValidator();
    }

    /**
     * Creates a new ConfigurationService with the specified dependencies.
     * This constructor is primarily used for testing.
     *
     * @param configLoader   the configuration loader to use
     * @param inputValidator the input validator to use
     */
    public ConfigurationService(ConfigLoader configLoader, InputValidator inputValidator) {
        this.configLoader = configLoader;
        this.inputValidator = inputValidator;
    }

    /**
     * Creates a unified configuration from various sources.
     * Precedence order (highest to lowest):
     * 1. Environment variables
     * 2. Command-line arguments
     * 3. YAML configuration file
     *
     * @param cliConfig  configuration from command-line arguments
     * @param configFile YAML configuration file (can be null)
     * @return the unified configuration
     */
    public AppConfig createUnifiedConfig(AppConfig cliConfig, File configFile) {
        // Load configuration from YAML file (lowest priority)
        var fileConfig = configFile != null ? configLoader.loadFromYaml(configFile) : null;

        // Load configuration from environment variables (the highest priority)
        var envConfig = configLoader.loadFromEnvironment();

        // Merge configurations with the correct precedence
        var builder = AppConfig.builder();

        // Start with file config (the lowest priority)
        if (fileConfig != null) {
            applyConfig(builder, fileConfig);
        }

        // Apply CLI config (medium priority)
        applyConfig(builder, cliConfig);

        // Apply environment config (the highest priority)
        applyConfig(builder, envConfig);

        return builder.build();
    }

    /**
     * Validates the configuration.
     *
     * @param config the configuration to validate
     * @throws GitLabException if the configuration is invalid
     */
    public void validateConfig(AppConfig config) throws GitLabException {
        // Check if we have a token
        if (config.getGitlabToken() == null || config.getGitlabToken().trim().isEmpty()) {
            throw new GitLabException("GitLab token is required. Provide it with --gitlab-token, in a config file, or set GITLAB_MR_GITLAB_TOKEN environment variable.");
        }

        // Validate token format
        if (!isValidToken(config.getGitlabToken())) {
            throw new GitLabException("Invalid GitLab token format");
        }

        // Check if we have a URL
        if (config.getGitlabUrl() == null || config.getGitlabUrl().trim().isEmpty()) {
            throw new GitLabException("GitLab URL is required. Provide it with --gitlab-url, in a config file, or set GITLAB_MR_GITLAB_URL environment variable.");
        }

        // Validate URL format
        if (!inputValidator.isValidGitLabUrl(config.getGitlabUrl())) {
            throw new GitLabException("Invalid GitLab URL format: " + config.getGitlabUrl());
        }

        // Validate project ID
        if (config.getProjectId() == null || config.getProjectId() <= 0) {
            throw new GitLabException("Invalid project ID: " + config.getProjectId());
        }

        // Validate project ID format
        if (!inputValidator.isValidProjectId(config.getProjectId().toString())) {
            throw new GitLabException("Invalid project ID format: " + config.getProjectId());
        }

        // Validate merge request IID if provided
        if (config.getMergeRequestIids() != null &&
                (config.getMergeRequestIids().isEmpty() ||
                        config.getMergeRequestIids().stream().anyMatch(i -> i <= 0)
                )
        ) {
            throw new GitLabException("Invalid merge request IID: " + config.getMergeRequestIids());
        }
    }

    /**
     * Applies a configuration to a builder, only setting non-null values.
     *
     * @param builder the builder to apply the configuration to
     * @param config  the configuration to apply
     */
    private void applyConfig(AppConfig.AppConfigBuilder builder, AppConfig config) {
        if (config == null) {
            return;
        }
        if (config.getGitlabUrl() != null) {
            builder.gitlabUrl(config.getGitlabUrl());
        }
        if (config.getGitlabToken() != null) {
            builder.gitlabToken(config.getGitlabToken());
        }
        if (config.getProjectId() != null) {
            builder.projectId(config.getProjectId());
        }
        if (config.getMergeRequestIids() != null) {
            builder.mergeRequestIids(config.getMergeRequestIids());
        }
        if (config.getCreateGitlabNote() != null) {
            builder.createGitlabNote(config.getCreateGitlabNote());
        }
        if (config.getUpdateMrStatus() != null) {
            builder.updateMrStatus(config.getUpdateMrStatus());
        }
        if (config.getDryRun() != null) {
            builder.dryRun(config.getDryRun());
        }
        if (config.getVerbose() != null) {
            builder.verbose(config.getVerbose());
        }
        if (config.getIncludeDraftMrs() != null) {
            builder.includeDraftMrs(config.getIncludeDraftMrs());
        }
        if (config.getIgnorePatterns() != null) {
            builder.ignorePatterns(config.getIgnorePatterns());
        }
    }

    /**
     * Validates the GitLab token format.
     *
     * @param token the token to validate
     * @return true if the token is valid, false otherwise
     */
    private boolean isValidToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        // Simple validation: token should be at least 20 characters and alphanumeric
        return token.matches("^[a-zA-Z0-9_-]{20,}$");
    }
}
