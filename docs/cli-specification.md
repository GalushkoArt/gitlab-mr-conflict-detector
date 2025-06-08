# CLI Package Specification

## Overview
The CLI (Command Line Interface) package provides the command-line interface for users to interact with the GitLab MR Conflict Detector application. It handles parsing command-line arguments, configuring the application, and orchestrating the workflow of fetching merge requests, detecting conflicts, and updating GitLab.

## Key Components

### ConflictDetectorApplication
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

### Basic Usage Examples

```bash
# Analyze all open MRs in a project
java -jar gitlab-mr-conflict-detector.jar \
  --gitlab-url https://gitlab.company.com \
  --gitlab-token glpat-xxxxxxxxxxxxxxxxxxxx \
  --project-id 42

# Analyze specific MR with verbose output
java -jar gitlab-mr-conflict-detector.jar \
  --gitlab-url https://gitlab.company.com \
  --gitlab-token glpat-xxxxxxxxxxxxxxxxxxxx \
  --project-id 42 \
  --mr-iid 123 \
  --verbose

# Dry run with ignore patterns
java -jar gitlab-mr-conflict-detector.jar \
  --gitlab-url https://gitlab.company.com \
  --gitlab-token glpat-xxxxxxxxxxxxxxxxxxxx \
  --project-id 42 \
  --ignore-patterns "*.md,docs/,test/" \
  --dry-run
```

### Configuration File Examples

```bash
# Using configuration file
java -jar gitlab-mr-conflict-detector.jar \
  --config-file /path/to/config.yml

# Override config file settings
java -jar gitlab-mr-conflict-detector.jar \
  --config-file /path/to/config.yml \
  --verbose \
  --dry-run
```

## Troubleshooting

### Common CLI Issues

**Invalid Arguments:**
- Check argument spelling and format
- Use `--help` to see all available options
- Ensure required arguments are provided

**Configuration Conflicts:**
- CLI arguments override configuration file settings
- Environment variables override configuration file settings
- CLI arguments override environment variables

**Token Issues:**
- Verify the token format (should start with 'glpat-')
- Check token permissions and scopes
- Ensure token hasn't expired
```