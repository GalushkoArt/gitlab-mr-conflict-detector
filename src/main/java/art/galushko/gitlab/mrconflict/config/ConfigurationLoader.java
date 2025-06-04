package art.galushko.gitlab.mrconflict.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import art.galushko.gitlab.mrconflict.core.ConflictDetectionConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Loads application configuration from various sources.
 */
@Slf4j
public class ConfigurationLoader {
    private static final String ENV_GITLAB_URL = "GITLAB_URL";
    private static final String ENV_GITLAB_TOKEN = "GITLAB_TOKEN";
    private static final String ENV_PROJECT_ID = "GITLAB_PROJECT_ID";
    private static final String ENV_PROJECT_PATH = "GITLAB_PROJECT_PATH";
    private static final String ENV_FORCE_PASS = "CONFLICT_DETECTOR_FORCE_PASS";

    private final ObjectMapper yamlMapper;

    public ConfigurationLoader() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    /**
     * Loads configuration from a YAML file.
     *
     * @param configPath path to the configuration file
     * @return loaded application configuration
     * @throws ConfigurationException if configuration cannot be loaded
     */
    public ApplicationConfig loadFromFile(Path configPath) throws ConfigurationException {
        try {
            if (!Files.exists(configPath)) {
                throw new ConfigurationException("Configuration file not found: " + configPath);
            }

            log.info("Loading configuration from: {}", configPath);

            Map<String, Object> configData = yamlMapper.readValue(configPath.toFile(), Map.class);
            return parseConfiguration(configData);

        } catch (IOException e) {
            throw new ConfigurationException("Failed to load configuration from " + configPath, e);
        }
    }

    /**
     * Loads configuration from environment variables.
     *
     * @return application configuration based on environment variables
     * @throws ConfigurationException if required environment variables are missing
     */
    public ApplicationConfig loadFromEnvironment() throws ConfigurationException {
        log.info("Loading configuration from environment variables");

        var gitlabUrl = getRequiredEnv(ENV_GITLAB_URL);
        var gitlabToken = getRequiredEnv(ENV_GITLAB_TOKEN);

        var gitlabConfigBuilder = GitLabConfig.builder()
                .gitlabUrl(gitlabUrl)
                .accessToken(gitlabToken);

        // Project ID or path
        var projectId = System.getenv(ENV_PROJECT_ID);
        var projectPath = System.getenv(ENV_PROJECT_PATH);

        if (projectId != null) {
            gitlabConfigBuilder.projectId(Long.parseLong(projectId));
        } else if (projectPath != null) {
            gitlabConfigBuilder.projectPath(projectPath);
        } else {
            throw new ConfigurationException("Either " + ENV_PROJECT_ID + " or " + ENV_PROJECT_PATH + " must be set");
        }

        // Force pass setting
        boolean forcePass = Boolean.parseBoolean(System.getenv(ENV_FORCE_PASS));

        return ApplicationConfig.builder()
                .gitLabConfig(gitlabConfigBuilder.build())
                .detectionConfig(ConflictDetectionConfig.defaultConfig())
                .fileFilterConfig(FileFilterConfig.defaultConfig())
                .forcePassEnabled(forcePass)
                .build();
    }

    /**
     * Creates a default configuration.
     *
     * @return default application configuration
     */
    public ApplicationConfig createDefaultConfig() {
        log.info("Creating default configuration");

        GitLabConfig gitlabConfig = GitLabConfig.builder()
                .gitlabUrl("https://gitlab.com")
                .accessToken("your-access-token")
                .projectId(1L)
                .build();

        return ApplicationConfig.builder()
                .gitLabConfig(gitlabConfig)
                .detectionConfig(ConflictDetectionConfig.defaultConfig())
                .fileFilterConfig(FileFilterConfig.defaultConfig())
                .build();
    }

    /**
     * Parses configuration from a map structure (typically from YAML).
     */
    @SuppressWarnings("unchecked")
    private ApplicationConfig parseConfiguration(Map<String, Object> configData) throws ConfigurationException {
        try {
            var appConfigBuilder = ApplicationConfig.builder();

            // Parse GitLab configuration
            Map<String, Object> gitlabData = (Map<String, Object>) configData.get("gitlab");
            if (gitlabData != null) {
                appConfigBuilder.gitLabConfig(parseGitLabConfig(gitlabData));
            }

            // Parse detection configuration
            Map<String, Object> detectionData = (Map<String, Object>) configData.get("detection");
            if (detectionData != null) {
                appConfigBuilder.detectionConfig(parseDetectionConfig(detectionData));
            } else {
                appConfigBuilder.detectionConfig(ConflictDetectionConfig.defaultConfig());
            }

            // Parse file filter configuration
            Map<String, Object> filterData = (Map<String, Object>) configData.get("fileFilter");
            if (filterData != null) {
                appConfigBuilder.fileFilterConfig(parseFileFilterConfig(filterData));
            } else {
                appConfigBuilder.fileFilterConfig(FileFilterConfig.defaultConfig());
            }

            // Parse force pass setting
            Boolean forcePass = (Boolean) configData.get("forcePass");
            if (forcePass != null) {
                appConfigBuilder.forcePassEnabled(forcePass);
            }

            return appConfigBuilder.build();

        } catch (Exception e) {
            throw new ConfigurationException("Failed to parse configuration", e);
        }
    }

    private GitLabConfig parseGitLabConfig(Map<String, Object> gitlabData) {
        var builder = GitLabConfig.builder();

        String url = (String) gitlabData.get("url");
        if (url != null) {
            builder.gitlabUrl(url);
        }

        String token = (String) gitlabData.get("token");
        if (token != null) {
            builder.accessToken(token);
        }

        Object projectId = gitlabData.get("projectId");
        if (projectId instanceof Number) {
            builder.projectId(((Number) projectId).longValue());
        }

        String projectPath = (String) gitlabData.get("projectPath");
        if (projectPath != null) {
            builder.projectPath(projectPath);
        }

        Object timeout = gitlabData.get("timeoutSeconds");
        if (timeout instanceof Number) {
            builder.timeoutSeconds(((Number) timeout).intValue());
        }

        Boolean verifySSL = (Boolean) gitlabData.get("verifySSL");
        if (verifySSL != null) {
            builder.verifySSL(verifySSL);
        }

        return builder.build();
    }

    private ConflictDetectionConfig parseDetectionConfig(Map<String, Object> detectionData) {
        var builder = ConflictDetectionConfig.builder();

        Boolean fetchBefore = (Boolean) detectionData.get("fetchBeforeDetection");
        if (fetchBefore != null) {
            builder.fetchBeforeDetection(fetchBefore);
        }

        String sensitivity = (String) detectionData.get("sensitivity");
        if (sensitivity != null) {
            builder.sensitivity(ConflictDetectionConfig.DetectionSensitivity.valueOf(sensitivity.toUpperCase()));
        }

        Boolean useRecursive = (Boolean) detectionData.get("useRecursiveMerge");
        if (useRecursive != null) {
            builder.useRecursiveMerge(useRecursive);
        }

        Object maxSections = detectionData.get("maxConflictSections");
        if (maxSections instanceof Number) {
            builder.maxConflictSections(((Number) maxSections).intValue());
        }

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private FileFilterConfig parseFileFilterConfig(Map<String, Object> filterData) {
        var builder = FileFilterConfig.builder();

        List<String> includePatterns = (List<String>) filterData.get("includePatterns");
        if (includePatterns != null) {
            builder.includePatterns(includePatterns);
        }

        List<String> excludePatterns = (List<String>) filterData.get("excludePatterns");
        if (excludePatterns != null) {
            builder.excludePatterns(excludePatterns);
        }

        Boolean caseSensitive = (Boolean) filterData.get("caseSensitive");
        if (caseSensitive != null) {
            builder.caseSensitive(caseSensitive);
        }

        Boolean followSymlinks = (Boolean) filterData.get("followSymlinks");
        if (followSymlinks != null) {
            builder.followSymlinks(followSymlinks);
        }

        Object maxFileSize = filterData.get("maxFileSizeBytes");
        if (maxFileSize instanceof Number) {
            builder.maxFileSizeBytes(((Number) maxFileSize).longValue());
        }

        return builder.build();
    }

    private String getRequiredEnv(String envVar) throws ConfigurationException {
        String value = System.getenv(envVar);
        if (value == null || value.trim().isEmpty()) {
            throw new ConfigurationException("Required environment variable not set: " + envVar);
        }
        return value.trim();
    }
}

