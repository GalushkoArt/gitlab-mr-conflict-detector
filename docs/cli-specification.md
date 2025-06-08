# CLI Package Specification

## Overview
The CLI (Command Line Interface) package provides the command-line interface for users to interact with the GitLab MR Conflict Detector application. It handles parsing command-line arguments, configuring the application, and orchestrating the workflow of fetching merge requests, detecting conflicts, and updating GitLab.

## Key Components

### SimpleGitLabMultiMergeRequestCommand
The main class in the CLI package that implements the `Callable<Integer>` interface from the Picocli framework. This class serves as the entry point for the application and handles all CLI-related functionality.

#### Responsibilities
- Defining command-line options and arguments
- Parsing and validating user input
- Configuring the application based on user input
- Orchestrating the workflow of the application
- Handling errors and providing appropriate feedback to the user
- Setting up logging based on verbosity level

#### Command-line Options
- `--gitlab-url`: GitLab instance URL
- `--gitlab-token`: GitLab personal access token
- `--project-id`: GitLab project ID
- `--mr-iids`: Specific merge requests IID to analyze (optional)
- `--create-gitlab-note`: Create notes on merge requests with conflict results
- `--update-mr-status`: Update merge request status based on conflicts
- `--dry-run`: Perform dry run without making changes to GitLab
- `--verbose`: Enable verbose logging
- `--include-draft-mrs`: Include draft/WIP merge requests in conflict analysis
- `--ignore-patterns`: Patterns for files/directories to ignore in conflict detection
- `--config-file`: Path to YAML configuration file

#### Workflow
1. Parse command-line arguments
2. Configure logging based on verbosity level
3. Create a configuration from command-line arguments, environment variables, and config file
4. Validate the configuration
5. Authenticate with GitLab
6. Validate GitLab connection and project access
7. Fetch merge requests from GitLab
8. Detect conflicts between merge requests
9. Format and display the output
10. Optionally update GitLab with conflict information (creating notes or updating MR status)
11. Return an appropriate exit code based on whether conflicts were found

#### Exit Codes
- `EXIT_SUCCESS (0)`: No conflicts detected
- `EXIT_CONFLICTS_DETECTED (1)`: Conflicts detected
- `EXIT_ERROR (2)`: An error occurred during execution

## Integration Points
- **Configuration Service**: For loading and validating configuration
- **Conflict Analysis Service**: For detecting conflicts between merge requests
- **GitLab Client**: For authenticating with GitLab and validating project access

## Usage Examples
```bash
# Basic usage
java -cp gitlab-mr-conflict-detector-1.0.0.jar \
  art.galushko.gitlab.mrconflict.cli.SimpleGitLabMultiMergeRequestCommand \
  --gitlab-url https://gitlab.example.com \
  --gitlab-token your-access-token \
  --project-id 123

# With GitLab integration
java -cp gitlab-mr-conflict-detector-1.0.0.jar \
  art.galushko.gitlab.mrconflict.cli.SimpleGitLabMultiMergeRequestCommand \
  --gitlab-url https://gitlab.example.com \
  --gitlab-token your-access-token \
  --project-id 123 \
  --create-gitlab-note \
  --update-mr-status
```