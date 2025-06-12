package art.galushko.gitlab.mrconflict.di;

import art.galushko.gitlab.mrconflict.config.AppConfig;
import art.galushko.gitlab.mrconflict.core.ConflictDetector;
import art.galushko.gitlab.mrconflict.core.IgnorePatternMatcher;
import art.galushko.gitlab.mrconflict.core.MultiMergeRequestConflictDetector;
import art.galushko.gitlab.mrconflict.core.PatternMatcher;
import art.galushko.gitlab.mrconflict.formatter.ConflictFormatter;
import art.galushko.gitlab.mrconflict.formatter.DefaultConflictFormatter;
import art.galushko.gitlab.mrconflict.gitlab.GitLab4JClient;
import art.galushko.gitlab.mrconflict.gitlab.GitLab4JMergeRequestService;
import art.galushko.gitlab.mrconflict.gitlab.GitLabClient;
import art.galushko.gitlab.mrconflict.gitlab.MergeRequestService;
import art.galushko.gitlab.mrconflict.security.CredentialService;
import art.galushko.gitlab.mrconflict.security.InputValidator;

/**
 * Factory for creating service instances.
 * This class implements a simple dependency injection pattern.
 */
public class ServiceFactory {

    private static ServiceFactory instance;

    // Singleton instances of services
    private PatternMatcher patternMatcher;
    private GitLabClient gitLabClient;
    private MergeRequestService mergeRequestService;
    private ConflictDetector conflictDetector;
    private ConflictFormatter conflictFormatter;
    private final AppConfig config;

    private ServiceFactory(AppConfig config) {
        // Private constructor to enforce singleton pattern
        this.config = config;
    }

    /**
     * Gets the singleton instance of the ServiceFactory.
     *
     * @return the ServiceFactory instance
     * @throws IllegalStateException when AppConfig is not provided via {@link ServiceFactory#provideConfig(AppConfig)}
     */
    public static synchronized ServiceFactory getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ServiceFactory instance has not been provided");
        }
        return instance;
    }

    /**
     * Provides the application configuration to initialize the ServiceFactory instance.
     * This method must be called before attempting to retrieve the ServiceFactory instance.
     *
     * @param config the application configuration to be used by the ServiceFactory
     * @throws IllegalArgumentException if the provided configuration is null
     */
    public static synchronized void provideConfig(AppConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("AppConfig must not be null");
        }
        instance = new ServiceFactory(config);
    }

    /**
     * Gets a PatternMatcher instance.
     *
     * @param caseSensitive whether pattern matching should be case-sensitive
     * @return the PatternMatcher instance
     */
    public synchronized PatternMatcher getPatternMatcher(boolean caseSensitive) {
        if (patternMatcher == null) {
            patternMatcher = new IgnorePatternMatcher(caseSensitive);
        }
        return patternMatcher;
    }

    /**
     * Gets a PatternMatcher instance with default settings (case-sensitive).
     *
     * @return the PatternMatcher instance
     */
    public PatternMatcher getPatternMatcher() {
        return getPatternMatcher(true);
    }

    /**
     * Gets a GitLabClient instance.
     *
     * @return the GitLabClient instance
     */
    public synchronized GitLabClient getGitLabClient() {
        if (gitLabClient == null) {
            gitLabClient = new GitLab4JClient(new CredentialService(), new InputValidator());
        }
        return gitLabClient;
    }

    /**
     * Gets a MergeRequestService instance.
     *
     * @return the MergeRequestService instance
     */
    public synchronized MergeRequestService getMergeRequestService() {
        if (mergeRequestService == null) {
            mergeRequestService = new GitLab4JMergeRequestService(getGitLabClient());
        }
        return mergeRequestService;
    }

    /**
     * Gets a ConflictDetector instance.
     *
     * @return the ConflictDetector instance
     */
    public synchronized ConflictDetector getConflictDetector() {
        if (conflictDetector == null) {
            conflictDetector = new MultiMergeRequestConflictDetector(getPatternMatcher());
        }
        return conflictDetector;
    }

    /**
     * Gets a ConflictFormatter instance.
     *
     * @return the ConflictFormatter instance
     */
    public synchronized ConflictFormatter getConflictFormatter() {
        if (conflictFormatter == null) {
            conflictFormatter = new DefaultConflictFormatter();
        }
        return conflictFormatter;
    }

    public synchronized AppConfig getConfig() {
        if (config == null) {
            throw new IllegalStateException("AppConfig has not been provided");
        }
        return config;
    }
}
