package art.galushko.gitlab.mrconflict.config;

import art.galushko.gitlab.mrconflict.core.ConflictDetectionConfig;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Project-specific configuration for conflict detection.
 */
public class ProjectConfig {
    private final Long projectId;
    private final String projectPath;
    private final List<String> targetBranches;
    private final Map<String, BranchConfig> branchConfigs;
    private final ConflictDetectionConfig detectionConfig;
    private final FileFilterConfig fileFilterConfig;
    private final boolean enabled;

    private ProjectConfig(Builder builder) {
        this.projectId = builder.projectId;
        this.projectPath = builder.projectPath;
        this.targetBranches = List.copyOf(builder.targetBranches != null ? builder.targetBranches : List.of());
        this.branchConfigs = Map.copyOf(builder.branchConfigs != null ? builder.branchConfigs : Map.of());
        this.detectionConfig = builder.detectionConfig;
        this.fileFilterConfig = builder.fileFilterConfig;
        this.enabled = builder.enabled;

        if (projectId == null && projectPath == null) {
            throw new IllegalArgumentException("Either project ID or project path must be specified");
        }
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public List<String> getTargetBranches() {
        return targetBranches;
    }

    public Map<String, BranchConfig> getBranchConfigs() {
        return branchConfigs;
    }

    public ConflictDetectionConfig getDetectionConfig() {
        return detectionConfig;
    }

    public FileFilterConfig getFileFilterConfig() {
        return fileFilterConfig;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets branch-specific configuration for a given branch.
     *
     * @param branchName name of the branch
     * @return branch configuration if found, null otherwise
     */
    public BranchConfig getBranchConfig(String branchName) {
        return branchConfigs.get(branchName);
    }

    /**
     * Checks if target branches are specified for this project.
     */
    public boolean hasTargetBranches() {
        return !targetBranches.isEmpty();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long projectId;
        private String projectPath;
        private List<String> targetBranches;
        private Map<String, BranchConfig> branchConfigs;
        private ConflictDetectionConfig detectionConfig;
        private FileFilterConfig fileFilterConfig;
        private boolean enabled = true;

        public Builder projectId(Long projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder projectPath(String projectPath) {
            this.projectPath = projectPath;
            return this;
        }

        public Builder targetBranches(List<String> targetBranches) {
            this.targetBranches = targetBranches;
            return this;
        }

        public Builder branchConfigs(Map<String, BranchConfig> branchConfigs) {
            this.branchConfigs = branchConfigs;
            return this;
        }

        public Builder detectionConfig(ConflictDetectionConfig detectionConfig) {
            this.detectionConfig = detectionConfig;
            return this;
        }

        public Builder fileFilterConfig(FileFilterConfig fileFilterConfig) {
            this.fileFilterConfig = fileFilterConfig;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public ProjectConfig build() {
            return new ProjectConfig(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectConfig that = (ProjectConfig) o;
        return enabled == that.enabled &&
               Objects.equals(projectId, that.projectId) &&
               Objects.equals(projectPath, that.projectPath) &&
               Objects.equals(targetBranches, that.targetBranches) &&
               Objects.equals(branchConfigs, that.branchConfigs) &&
               Objects.equals(detectionConfig, that.detectionConfig) &&
               Objects.equals(fileFilterConfig, that.fileFilterConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, projectPath, targetBranches, branchConfigs,
                           detectionConfig, fileFilterConfig, enabled);
    }

    @Override
    public String toString() {
        return "ProjectConfig{" +
               "projectId=" + projectId +
               ", projectPath='" + projectPath + '\'' +
               ", targetBranches=" + targetBranches.size() +
               ", branchConfigs=" + branchConfigs.size() +
               ", enabled=" + enabled +
               '}';
    }
}

