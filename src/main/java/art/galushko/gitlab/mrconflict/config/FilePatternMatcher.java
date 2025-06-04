package art.galushko.gitlab.mrconflict.config;

import lombok.extern.slf4j.Slf4j;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Objects;

/**
 * Utility class for matching file paths against glob patterns.
 */
@Slf4j
public class FilePatternMatcher {
    private final List<PathMatcher> includeMatchers;
    private final List<PathMatcher> excludeMatchers;
    private final boolean caseSensitive;
    
    public FilePatternMatcher(FileFilterConfig config) {
        Objects.requireNonNull(config, "File filter config cannot be null");
        
        this.caseSensitive = config.isCaseSensitive();
        this.includeMatchers = createMatchers(config.getIncludePatterns());
        this.excludeMatchers = createMatchers(config.getExcludePatterns());
        
        log.debug("Created file pattern matcher with {} include and {} exclude patterns",
                    includeMatchers.size(), excludeMatchers.size());
    }
    
    /**
     * Checks if a file path should be included based on the configured patterns.
     *
     * @param filePath the file path to check
     * @return true if the file should be included, false otherwise
     */
    public boolean shouldInclude(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }
        
        Path path = Path.of(normalizeFilePath(filePath));
        
        // If exclude patterns match, exclude the file
        if (matchesAny(path, excludeMatchers)) {
            log.trace("File excluded by exclude pattern: {}", filePath);
            return false;
        }
        
        // If include patterns are specified, file must match at least one
        if (!includeMatchers.isEmpty()) {
            boolean included = matchesAny(path, includeMatchers);
            if (!included) {
                log.trace("File not included by include patterns: {}", filePath);
            }
            return included;
        }
        
        // No include patterns specified, include by default (unless excluded)
        return true;
    }
    
    /**
     * Filters a list of file paths based on the configured patterns.
     *
     * @param filePaths list of file paths to filter
     * @return filtered list containing only paths that should be included
     */
    public List<String> filterPaths(List<String> filePaths) {
        return filePaths.stream()
                .filter(this::shouldInclude)
                .toList();
    }
    
    /**
     * Creates PathMatcher instances from glob patterns.
     */
    private List<PathMatcher> createMatchers(List<String> patterns) {
        return patterns.stream()
                .map(this::createMatcher)
                .filter(Objects::nonNull)
                .toList();
    }
    
    /**
     * Creates a single PathMatcher from a glob pattern.
     */
    private PathMatcher createMatcher(String pattern) {
        try {
            String normalizedPattern = normalizePattern(pattern);
            return FileSystems.getDefault().getPathMatcher("glob:" + normalizedPattern);
        } catch (Exception e) {
            log.warn("Invalid glob pattern '{}': {}", pattern, e.getMessage());
            return null;
        }
    }
    
    /**
     * Normalizes a glob pattern for consistent matching.
     */
    private String normalizePattern(String pattern) {
        if (!caseSensitive) {
            pattern = pattern.toLowerCase();
        }
        
        // Ensure pattern uses forward slashes for consistency
        pattern = pattern.replace('\\', '/');
        
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
    
    /**
     * Checks if a path matches any of the provided matchers.
     */
    private boolean matchesAny(Path path, List<PathMatcher> matchers) {
        String pathString = path.toString().replace('\\', '/');

        for (PathMatcher matcher : matchers) {
            try {
                // Try matching the path directly
                if (matcher.matches(path)) {
                    return true;
                }
                
                // Try matching with normalized separators
                Path normalizedPath = Path.of(pathString);
                if (matcher.matches(normalizedPath)) {
                    return true;
                }

                // For patterns starting with **, also try with a leading directory
                // This handles cases where ** expects at least one directory level
                String prefixedPath = "dummy/" + pathString;
                if (matcher.matches(Path.of(prefixedPath))) {
                    return true;
                }

                // Try matching as relative path (remove leading slash if present)
                String relativePath = pathString.startsWith("/") ? pathString.substring(1) : pathString;
                if (!relativePath.equals(pathString) && matcher.matches(Path.of(relativePath))) {
                    return true;
                }
            } catch (Exception e) {
                log.debug("Error matching path '{}' against pattern: {}", path, e.getMessage());
            }
        }
        return false;
    }
    
    /**
     * Gets the number of include patterns configured.
     */
    public int getIncludePatternCount() {
        return includeMatchers.size();
    }
    
    /**
     * Gets the number of exclude patterns configured.
     */
    public int getExcludePatternCount() {
        return excludeMatchers.size();
    }
    
    /**
     * Checks if any patterns are configured.
     */
    public boolean hasPatterns() {
        return !includeMatchers.isEmpty() || !excludeMatchers.isEmpty();
    }
}

