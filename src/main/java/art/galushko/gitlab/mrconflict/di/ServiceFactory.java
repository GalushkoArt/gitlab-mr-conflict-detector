package art.galushko.gitlab.mrconflict.di;

import art.galushko.gitlab.mrconflict.config.IgnorePatternMatcher;
import art.galushko.gitlab.mrconflict.config.PatternMatcher;
import art.galushko.gitlab.mrconflict.core.ConflictDetector;
import art.galushko.gitlab.mrconflict.core.MultiMergeRequestConflictDetector;
import art.galushko.gitlab.mrconflict.formatter.ConflictFormatter;
import art.galushko.gitlab.mrconflict.formatter.DefaultConflictFormatter;
import art.galushko.gitlab.mrconflict.gitlab.GitLab4JClient;
import art.galushko.gitlab.mrconflict.gitlab.GitLab4JMergeRequestService;
import art.galushko.gitlab.mrconflict.gitlab.GitLabClient;
import art.galushko.gitlab.mrconflict.gitlab.MergeRequestService;

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

    private ServiceFactory() {
        // Private constructor to enforce singleton pattern
    }

    /**
     * Gets the singleton instance of the ServiceFactory.
     *
     * @return the ServiceFactory instance
     */
    public static synchronized ServiceFactory getInstance() {
        if (instance == null) {
            instance = new ServiceFactory();
        }
        return instance;
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
            gitLabClient = new GitLab4JClient();
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
}
