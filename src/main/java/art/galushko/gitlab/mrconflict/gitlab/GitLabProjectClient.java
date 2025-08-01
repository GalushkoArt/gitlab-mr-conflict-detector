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
     */
    Project getProject(Long projectId);
}