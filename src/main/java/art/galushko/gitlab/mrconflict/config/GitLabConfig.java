package art.galushko.gitlab.mrconflict.config;

import java.util.Objects;

/**
 * Configuration for GitLab connection and authentication.
 */
public class GitLabConfig {
    private final String gitlabUrl;
    private final String accessToken;
    private final Long projectId;
    private final String projectPath;
    private final int timeoutSeconds;
    private final boolean verifySSL;

    private GitLabConfig(Builder builder) {
        this.gitlabUrl = Objects.requireNonNull(builder.gitlabUrl, "GitLab URL cannot be null");
        this.accessToken = Objects.requireNonNull(builder.accessToken, "Access token cannot be null");
        this.projectId = builder.projectId;
        this.projectPath = builder.projectPath;
        this.timeoutSeconds = builder.timeoutSeconds;
        this.verifySSL = builder.verifySSL;

        if (projectId == null && projectPath == null) {
            throw new IllegalArgumentException("Either project ID or project path must be specified");
        }
    }

    public String getGitlabUrl() {
        return gitlabUrl;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public boolean isVerifySSL() {
        return verifySSL;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String gitlabUrl;
        private String accessToken;
        private Long projectId;
        private String projectPath;
        private int timeoutSeconds = 30;
        private boolean verifySSL = true;

        public Builder gitlabUrl(String gitlabUrl) {
            this.gitlabUrl = gitlabUrl;
            return this;
        }

        public Builder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public Builder projectId(Long projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder projectPath(String projectPath) {
            this.projectPath = projectPath;
            return this;
        }

        public Builder timeoutSeconds(int timeoutSeconds) {
            if (timeoutSeconds <= 0) {
                throw new IllegalArgumentException("Timeout must be positive");
            }
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public Builder verifySSL(boolean verifySSL) {
            this.verifySSL = verifySSL;
            return this;
        }

        public GitLabConfig build() {
            return new GitLabConfig(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GitLabConfig that = (GitLabConfig) o;
        return timeoutSeconds == that.timeoutSeconds &&
               verifySSL == that.verifySSL &&
               Objects.equals(gitlabUrl, that.gitlabUrl) &&
               Objects.equals(accessToken, that.accessToken) &&
               Objects.equals(projectId, that.projectId) &&
               Objects.equals(projectPath, that.projectPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gitlabUrl, accessToken, projectId, projectPath, timeoutSeconds, verifySSL);
    }

    @Override
    public String toString() {
        return "GitLabConfig{" +
               "gitlabUrl='" + gitlabUrl + '\'' +
               ", accessToken='***'" +
               ", projectId=" + projectId +
               ", projectPath='" + projectPath + '\'' +
               ", timeoutSeconds=" + timeoutSeconds +
               ", verifySSL=" + verifySSL +
               '}';
    }
}

