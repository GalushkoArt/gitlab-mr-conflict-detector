package art.galushko.gitlab.mrconflict.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 * Loads configuration from various sources (YAML files, environment variables).
 */
@Slf4j
public class ConfigLoader {
    private static final String ENV_PREFIX = "GITLAB_MR_";
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    /**
     * Loads configuration from a YAML file.
     * 
     * @param configFile the YAML configuration file
     * @return the loaded configuration, or null if loading failed
     */
    public AppConfig loadFromYaml(File configFile) {
        if (configFile == null || !configFile.exists() || !configFile.isFile()) {
            log.warn("Configuration file does not exist or is not a file: {}", 
                    configFile != null ? configFile.getAbsolutePath() : "null");
            return null;
        }

        try {
            log.info("Loading configuration from file: {}", configFile.getAbsolutePath());
            return YAML_MAPPER.readValue(configFile, AppConfig.class);
        } catch (IOException e) {
            log.error("Failed to load configuration from file: {}", configFile.getAbsolutePath(), e);
            return null;
        }
    }

    /**
     * Loads configuration from environment variables.
     * Environment variables should be prefixed with GITLAB_MR_ and use uppercase with underscores.
     * For example, GITLAB_MR_GITLAB_URL for gitlabUrl.
     * 
     * @return the loaded configuration
     */
    public AppConfig loadFromEnvironment() {
        Map<String, String> envVars = getEnvironmentVariables();
        AppConfig.AppConfigBuilder builder = AppConfig.builder();

        // Process environment variables
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key.startsWith(ENV_PREFIX)) {
                String configKey = key.substring(ENV_PREFIX.length()).toLowerCase();
                if (value != null) {
                    setConfigValue(builder, configKey, value);
                }
            }
        }

        return builder.build();
    }

    /**
     * Gets the environment variables.
     * This method is extracted to make testing easier.
     * 
     * @return the environment variables
     */
    protected Map<String, String> getEnvironmentVariables() {
        return System.getenv();
    }

    /**
     * Sets a configuration value on the builder based on the key and value.
     * 
     * @param builder the AppConfig builder
     * @param key the configuration key (lowercase)
     * @param value the configuration value
     */
    private void setConfigValue(AppConfig.AppConfigBuilder builder, String key, String value) {
        switch (key) {
            case "gitlab_url":
                builder.gitlabUrl(value);
                break;
            case "gitlab_token":
                builder.gitlabToken(value);
                break;
            case "project_id":
                try {
                    builder.projectId(Long.parseLong(value));
                } catch (NumberFormatException e) {
                    log.warn("Invalid project ID in environment variable: {}", value);
                }
                break;
            case "merge_request_iid":
                try {
                    builder.mergeRequestIids(Arrays.stream(value.split(",")).map(Long::parseLong).toList());
                } catch (NumberFormatException e) {
                    log.warn("Invalid merge request IID in environment variable: {}", value);
                }
                break;
            case "create_gitlab_note":
                builder.createGitlabNote(Boolean.parseBoolean(value));
                break;
            case "update_mr_status":
                builder.updateMrStatus(Boolean.parseBoolean(value));
                break;
            case "dry_run":
                builder.dryRun(Boolean.parseBoolean(value));
                break;
            case "verbose":
                builder.verbose(Boolean.parseBoolean(value));
                break;
            case "include_draft_mrs":
                builder.includeDraftMrs(Boolean.parseBoolean(value));
                break;
            default:
                log.warn("Unknown configuration key in environment variable: {}", key);
                break;
        }
    }
}
