package art.galushko.gitlab.mrconflict.core;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

/**
 * Enhanced file pattern matcher that supports .gitignore-style patterns.
 * This class provides functionality to match file paths against patterns,
 * similar to .gitignore patterns, including:
 * 
 * <ul>
 *   <li>Basic glob patterns with * and ?</li>
 *   <li>Directory-specific patterns (ending with /)</li>
 *   <li>Negation patterns (starting with !)</li>
 *   <li>Double-asterisk for matching across directories (**)</li>
 *   <li>Character classes ([abc], [0-9])</li>
 * </ul>
 */
@Slf4j
@Builder
public class IgnorePatternMatcher implements PatternMatcher {
    /**
     * Flag indicating whether pattern matching should be case-sensitive.
     * If true, matching is case-sensitive; if false, matching is case-insensitive.
     */
    private final boolean caseSensitive;

    /**
     * Flag indicating whether to use extended glob syntax.
     * If true, patterns like ** will match across directories.
     */
    private final boolean extendedGlob;

    /**
     * Constructs a new IgnorePatternMatcher with the specified options.
     *
     * @param caseSensitive true for case-sensitive matching, false for case-insensitive matching
     * @param extendedGlob true to enable extended glob syntax, false to use basic glob
     */
    public IgnorePatternMatcher(boolean caseSensitive, boolean extendedGlob) {
        this.caseSensitive = caseSensitive;
        this.extendedGlob = extendedGlob;
    }

    /**
     * Constructs a new IgnorePatternMatcher with the specified case sensitivity and extended glob enabled.
     *
     * @param caseSensitive true for case-sensitive matching, false for case-insensitive matching
     */
    public IgnorePatternMatcher(boolean caseSensitive) {
        this(caseSensitive, true);
    }

    /**
     * Constructs a new IgnorePatternMatcher with default case-sensitive matching and extended glob enabled.
     */
    public IgnorePatternMatcher() {
        this(false, true);
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

        // Handle negation patterns (patterns starting with !)
        boolean negated = false;
        String effectivePattern = pattern;
        if (pattern.startsWith("!")) {
            negated = true;
            effectivePattern = pattern.substring(1);
            // If there's only the ! character, it's not a valid pattern
            if (effectivePattern.isEmpty()) {
                return false;
            }
        }

        // Normalize paths for consistency
        String normalizedPath = normalizePath(filePath);
        String normalizedPattern = normalizePath(effectivePattern);

        // For case-insensitive matching, convert to lowercase
        if (!caseSensitive) {
            normalizedPath = normalizedPath.toLowerCase();
            normalizedPattern = normalizedPattern.toLowerCase();
        }

        // Check for exact match first (most common and fastest check)
        boolean matches = normalizedPath.equals(normalizedPattern);

        // If no exact match, try other pattern types
        if (!matches) {
            // Handle directory patterns (ending with /)
            if (effectivePattern.endsWith("/")) {
                matches = matchesDirectoryPattern(normalizedPath, normalizedPattern);
            } else {
                // Handle glob patterns
                matches = matchesGlobPattern(normalizedPath, normalizedPattern);
            }
        }

        // Apply negation if needed
        return negated != matches;
    }

    /**
     * Checks if a path matches a directory pattern.
     * A directory pattern is a pattern that ends with a slash.
     * 
     * @param normalizedPath the normalized path to check
     * @param normalizedPattern the normalized pattern to match against
     * @return true if the path matches the directory pattern
     */
    private boolean matchesDirectoryPattern(String normalizedPath, String normalizedPattern) {
        // Remove the trailing slash from the pattern
        String dirPattern = normalizedPattern.substring(0, normalizedPattern.length() - 1);

        // The path matches if it's exactly the directory or is a file within the directory
        return normalizedPath.equals(dirPattern) || normalizedPath.startsWith(dirPattern + "/");
    }

    /**
     * Checks if a path matches a glob pattern.
     * 
     * @param normalizedPath the normalized path to check
     * @param normalizedPattern the normalized pattern to match against
     * @return true if the path matches the glob pattern
     */
    private boolean matchesGlobPattern(String normalizedPath, String normalizedPattern) {
        try {
            // Convert .gitignore-style pattern to Java glob pattern
            String javaGlobPattern = convertToJavaGlob(normalizedPattern);

            // Create a glob pattern matcher
            String globPattern = "glob:" + javaGlobPattern;
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher(globPattern);

            // Try to match the path directly
            if (matchPath(matcher, normalizedPath)) {
                return true;
            }

            // Try with a leading slash (for absolute paths)
            return matchPath(matcher, "/" + normalizedPath);

        } catch (Exception e) {
            log.debug("Error matching path '{}' against pattern '{}': {}", 
                    normalizedPath, normalizedPattern, e.getMessage());
            return false;
        }
    }

    /**
     * Converts a .gitignore-style pattern to a Java glob pattern.
     * 
     * @param pattern the .gitignore-style pattern
     * @return the equivalent Java glob pattern
     */
    private String convertToJavaGlob(String pattern) {
        // If extended glob is disabled, return the pattern as is
        if (!extendedGlob) {
            return pattern;
        }

        // Handle ** pattern (match across directories)
        // In .gitignore, ** matches zero or more directories
        // In Java glob, ** does the same but needs special handling
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < pattern.length()) {
            if (i < pattern.length() - 1 && pattern.charAt(i) == '*' && pattern.charAt(i + 1) == '*') {
                // Handle ** pattern
                if (i > 0 && pattern.charAt(i - 1) != '/') {
                    result.append('/');
                }
                result.append("**");
                if (i + 2 < pattern.length() && pattern.charAt(i + 2) != '/') {
                    result.append('/');
                }
                i += 2;
            } else {
                // Copy character as is
                result.append(pattern.charAt(i));
                i++;
            }
        }

        return result.toString();
    }

    /**
     * Attempts to match a path against a PathMatcher.
     * 
     * @param matcher the PathMatcher to use
     * @param pathStr the path string to match
     * @return true if the path matches, false otherwise or if an error occurs
     */
    private boolean matchPath(PathMatcher matcher, String pathStr) {
        try {
            Path path = Path.of(pathStr);
            return matcher.matches(path);
        } catch (Exception e) {
            // Ignore invalid paths
            return false;
        }
    }

    /**
     * Normalizes a path for consistent matching.
     * This method ensures that paths are in a consistent format by:
     * 1. Converting all backslashes to forward slashes
     * 2. Removing any leading slash
     *
     * @param path the path to normalize
     * @return the normalized path
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
