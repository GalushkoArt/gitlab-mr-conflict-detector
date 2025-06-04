package art.galushko.gitlab.mrconflict.config;

import art.galushko.gitlab.mrconflict.core.ConflictDetectionConfig;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * Branch-specific configuration for conflict detection.
 */
@Getter
@Builder
@EqualsAndHashCode
public class BranchConfig {
    @NonNull
    private final String branchName;
    private final ConflictDetectionConfig detectionConfig;
    private final FileFilterConfig fileFilterConfig;
    @Builder.Default
    private final boolean enabled = true;
    @Builder.Default
    private final int priority = 0;
}

