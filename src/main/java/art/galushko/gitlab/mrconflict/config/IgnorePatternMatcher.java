package art.galushko.gitlab.mrconflict.config;

import lombok.extern.slf4j.Slf4j;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

/**
 * Enhanced file pattern matcher that supports simple ignore patterns.
 */
@Slf4j
public class IgnorePatternMatcher implements PatternMatcher {
    private final boolean caseSensitive;

    public IgnorePatternMatcher(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public IgnorePatternMatcher() {
        this(true);
    }

    /**
     * Checks if a file path matches the given pattern.
     *
     * @param pattern the pattern to match against
     * @param filePath the file path to check
     * @return true if the file path matches the pattern
     */
    @Override
    public boolean matches(String pattern, String filePath) {
        // Handle null or empty inputs
        if (filePath == null || filePath.isEmpty() || pattern == null || pattern.isEmpty()) {
            return false;
        }

        // Normalize paths for consistency
        String normalizedPath = normalizePath(filePath);
        String normalizedPattern = normalizePath(pattern);

        // For case-insensitive matching, convert to lowercase
        if (!caseSensitive) {
            normalizedPath = normalizedPath.toLowerCase();
            normalizedPattern = normalizedPattern.toLowerCase();
        }

        // Handle directory patterns (ending with /)
        if (pattern.endsWith("/")) {
            String dirPattern = normalizedPattern.substring(0, normalizedPattern.length() - 1);
            return normalizedPath.equals(dirPattern) || normalizedPath.startsWith(dirPattern + "/");
        }

        // Handle exact matches
        if (normalizedPath.equals(normalizedPattern)) {
            return true;
        }

        // Handle glob patterns
        try {
            // Create a glob pattern matcher
            String globPattern = "glob:" + normalizedPattern;
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher(globPattern);

            // Try to match the path
            try {
                Path path = Path.of(normalizedPath);
                if (matcher.matches(path)) {
                    return true;
                }
            } catch (Exception e) {
                // Ignore invalid paths
            }

            // Try with a leading slash
            try {
                Path path = Path.of("/" + normalizedPath);
                if (matcher.matches(path)) {
                    return true;
                }
            } catch (Exception e) {
                // Ignore invalid paths
            }
        } catch (Exception e) {
            log.debug("Error matching path '{}' against pattern '{}': {}", filePath, pattern, e.getMessage());
        }

        return false;
    }

    /**
     * Normalizes a path for consistent matching.
     */
    private String normalizePath(String path) {
        // Ensure path uses forward slashes for consistency
        path = path.replace('\\', '/');

        // Remove leading slash if present
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        return path;
    }
}
