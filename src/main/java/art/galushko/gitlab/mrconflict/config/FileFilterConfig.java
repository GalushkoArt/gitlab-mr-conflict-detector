package art.galushko.gitlab.mrconflict.config;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;

/**
 * Configuration for file and directory filtering during conflict detection.
 */
@EqualsAndHashCode
@Getter
@Builder
public class FileFilterConfig {
    @Builder.Default
    private final List<String> includePatterns = List.of();
    @Builder.Default
    private final List<String> excludePatterns = List.of();
    @Builder.Default
    private final boolean caseSensitive = true;
    @Builder.Default
    private final boolean followSymlinks = false;
    @Builder.Default
    private final long maxFileSizeBytes = 10 * 1024 * 1024; // 10MB default

    public static FileFilterConfig defaultConfig() {
        return builder()
                .excludePatterns(List.of(
                        "**/.git/**",
                        "**/node_modules/**",
                        "**/target/**",
                        "**/build/**",
                        "**/.gradle/**",
                        "**/*.class",
                        "**/*.jar",
                        "**/*.war",
                        "**/*.log",
                        "**/.DS_Store",
                        "**/Thumbs.db"
                ))
                .build();
    }
}

