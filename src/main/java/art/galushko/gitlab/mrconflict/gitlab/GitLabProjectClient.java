package art.galushko.gitlab.mrconflict.gitlab;

import org.gitlab4j.api.models.Project;

/**
 * Interface for GitLab project operations.
 * This interface follows the Interface Segregation Principle by focusing only on project-related operations.
 */
public interface GitLabProjectClient {
    
    /**
     * Gets a project by its ID.
     *
     * @param projectId GitLab project ID
     * @return project information
     * @throws GitLabException if project cannot be retrieved
     */
    Project getProject(Long projectId) throws GitLabException;
    
    /**
     * Gets a project by its path (namespace/project-name).
     *
     * @param projectPath project path
     * @return project information
     * @throws GitLabException if project cannot be retrieved
     */
    Project getProject(String projectPath) throws GitLabException;
    
    /**
     * Gets the default branch for a project.
     *
     * @param projectId GitLab project ID
     * @return default branch name
     * @throws GitLabException if default branch cannot be determined
     */
    String getDefaultBranch(Long projectId) throws GitLabException;
}