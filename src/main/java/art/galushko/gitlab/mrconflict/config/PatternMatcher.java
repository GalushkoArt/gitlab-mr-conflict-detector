package art.galushko.gitlab.mrconflict.config;

/**
 * Interface for pattern matching functionality.
 */
public interface PatternMatcher {

    /**
     * Checks if a file path matches the given pattern.
     *
     * @param pattern the pattern to match against
     * @param filePath the file path to check
     * @return true if the file path matches the pattern
     */
    boolean matches(String pattern, String filePath);
}