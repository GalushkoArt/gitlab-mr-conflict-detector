package art.galushko.gitlab.mrconflict.config;

import lombok.extern.slf4j.Slf4j;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

/**
 * Enhanced file pattern matcher that supports simple ignore patterns.
 */
@Slf4j
public class IgnorePatternMatcher {
    private final boolean caseSensitive;

    public IgnorePatternMatcher(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public IgnorePatternMatcher() {
        this(true);
    }

    /**
     * Checks if a file path matches any of the ignored patterns.
     *
     * @param filePath the file path to check
     * @return true if the file should be ignored
     */
    public boolean matches(String pattern, String filePath) {
        if (filePath == null || filePath.isEmpty() || pattern == null || pattern.isEmpty()) {
            return false;
        }

        String normalizedPath = normalizeFilePath(filePath);
        String normalizedPattern = normalizePattern(pattern);

        // Handle simple directory patterns like "temp/"
        if (normalizedPattern.endsWith("/")) {
            return normalizedPath.startsWith(normalizedPattern) ||
                    normalizedPath.equals(normalizedPattern.substring(0, normalizedPattern.length() - 1));
        }

        // Handle exact file matches
        if (normalizedPath.equals(normalizedPattern)) {
            return true;
        }

        // Handle glob patterns
        try {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + normalizedPattern);
            Path path = Path.of(normalizedPath);

            // Try direct match
            if (matcher.matches(path)) {
                return true;
            }

            // Try with different path variations for better compatibility
            String[] pathVariations = {
                    normalizedPath,
                    "/" + normalizedPath,
                    normalizedPath.replace("/", "\\"),
                    "\\" + normalizedPath.replace("/", "\\")
            };

            for (String variation : pathVariations) {
                try {
                    if (matcher.matches(Path.of(variation))) {
                        return true;
                    }
                } catch (Exception e) {
                    // Ignore and try next variation
                }
            }

        } catch (Exception e) {
            log.debug("Error matching path '{}' against pattern '{}': {}", filePath, pattern, e.getMessage());
        }

        return false;
    }

    /**
     * Normalizes an ignore pattern for consistent matching.
     */
    private String normalizePattern(String pattern) {
        if (!caseSensitive) {
            pattern = pattern.toLowerCase();
        }

        // Ensure pattern uses forward slashes for consistency
        pattern = pattern.replace('\\', '/');

        // Handle directory patterns - ensure they match subdirectories too
        if (pattern.endsWith("/")) {
            // Convert "temp/" to "temp/**" to match all files in the directory
            pattern = pattern.substring(0, pattern.length() - 1) + "/**";
        }

        return pattern;
    }

    /**
     * Normalizes a file path for consistent matching.
     */
    private String normalizeFilePath(String filePath) {
        if (!caseSensitive) {
            filePath = filePath.toLowerCase();
        }

        // Ensure path uses forward slashes for consistency
        filePath = filePath.replace('\\', '/');

        // Remove leading slash if present
        if (filePath.startsWith("/")) {
            filePath = filePath.substring(1);
        }

        return filePath;
    }
}

