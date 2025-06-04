package art.galushko.gitlab.mrconflict.core;

import java.util.Objects;

/**
 * Configuration for conflict detection operations.
 */
public class ConflictDetectionConfig {
    private final boolean fetchBeforeDetection;
    private final boolean forcePassEnabled;
    private final DetectionSensitivity sensitivity;
    private final boolean useRecursiveMerge;
    private final int maxConflictSections;

    private ConflictDetectionConfig(Builder builder) {
        this.fetchBeforeDetection = builder.fetchBeforeDetection;
        this.forcePassEnabled = builder.forcePassEnabled;
        this.sensitivity = builder.sensitivity;
        this.useRecursiveMerge = builder.useRecursiveMerge;
        this.maxConflictSections = builder.maxConflictSections;
    }

    public boolean shouldFetchBeforeDetection() {
        return fetchBeforeDetection;
    }

    public boolean isForcePassEnabled() {
        return forcePassEnabled;
    }

    public DetectionSensitivity getSensitivity() {
        return sensitivity;
    }

    public boolean shouldUseRecursiveMerge() {
        return useRecursiveMerge;
    }

    public int getMaxConflictSections() {
        return maxConflictSections;
    }

    public static Builder builder() {
        return new Builder();
    }
    
    public static ConflictDetectionConfig defaultConfig() {
        return builder().build();
    }

    public static class Builder {
        private boolean fetchBeforeDetection = true;
        private boolean forcePassEnabled = false;
        private DetectionSensitivity sensitivity = DetectionSensitivity.NORMAL;
        private boolean useRecursiveMerge = true;
        private int maxConflictSections = 100;

        public Builder fetchBeforeDetection(boolean fetchBeforeDetection) {
            this.fetchBeforeDetection = fetchBeforeDetection;
            return this;
        }

        public Builder forcePassEnabled(boolean forcePassEnabled) {
            this.forcePassEnabled = forcePassEnabled;
            return this;
        }

        public Builder sensitivity(DetectionSensitivity sensitivity) {
            this.sensitivity = Objects.requireNonNull(sensitivity, "Sensitivity cannot be null");
            return this;
        }

        public Builder useRecursiveMerge(boolean useRecursiveMerge) {
            this.useRecursiveMerge = useRecursiveMerge;
            return this;
        }

        public Builder maxConflictSections(int maxConflictSections) {
            if (maxConflictSections <= 0) {
                throw new IllegalArgumentException("Max conflict sections must be positive");
            }
            this.maxConflictSections = maxConflictSections;
            return this;
        }

        public ConflictDetectionConfig build() {
            return new ConflictDetectionConfig(this);
        }
    }
    
    /**
     * Detection sensitivity levels.
     */
    public enum DetectionSensitivity {
        /**
         * Strict mode - detect all possible conflicts including whitespace changes
         */
        STRICT,
        
        /**
         * Normal mode - standard conflict detection
         */
        NORMAL,
        
        /**
         * Permissive mode - only detect significant conflicts
         */
        PERMISSIVE
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConflictDetectionConfig that = (ConflictDetectionConfig) o;
        return fetchBeforeDetection == that.fetchBeforeDetection &&
               forcePassEnabled == that.forcePassEnabled &&
               useRecursiveMerge == that.useRecursiveMerge &&
               maxConflictSections == that.maxConflictSections &&
               sensitivity == that.sensitivity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fetchBeforeDetection, forcePassEnabled, sensitivity, useRecursiveMerge, maxConflictSections);
    }

    @Override
    public String toString() {
        return "ConflictDetectionConfig{" +
               "fetchBeforeDetection=" + fetchBeforeDetection +
               ", forcePassEnabled=" + forcePassEnabled +
               ", sensitivity=" + sensitivity +
               ", useRecursiveMerge=" + useRecursiveMerge +
               ", maxConflictSections=" + maxConflictSections +
               '}';
    }
}

