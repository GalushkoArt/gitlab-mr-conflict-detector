# Configuration Package Specification

## Overview
The Configuration package is responsible for managing the application's configuration settings. It handles loading configuration from various sources (command-line arguments, environment variables, YAML files), merging them with the correct precedence, and validating the configuration values.

## Key Components

### AppConfig
A data model class that holds all configuration settings for the application.

#### Configuration Categories
- **GitLab Connection Settings**
  - `gitlabUrl`: GitLab instance URL
  - `gitlabToken`: GitLab personal access token
  - `projectId`: GitLab project ID
  - `mergeRequestIids`: List of specific merge request IIDs to analyze

- **Behavior Settings**
  - `createGitlabNote`: Whether to create notes on merge requests with conflict results
  - `updateMrStatus`: Whether to update merge request status based on conflicts
  - `dryRun`: Whether to perform a dry run without making changes to GitLab
  - `verbose`: Whether to enable verbose logging
  - `includeDraftMrs`: Whether to include draft/WIP merge requests in conflict analysis

- **Conflict Detection Settings**
  - `ignorePatterns`: List of patterns for files/directories to ignore in conflict detection

### ConfigLoader
Responsible for loading configuration from different sources.

#### Methods
- `loadFromYaml(File configFile)`: Loads configuration from a YAML file
- `loadFromEnvironment()`: Loads configuration from environment variables prefixed with "GITLAB_MR_"

#### Environment Variable Mapping
Environment variables are mapped to configuration properties as follows:
- `GITLAB_MR_GITLAB_URL` → `gitlabUrl`
- `GITLAB_MR_GITLAB_TOKEN` → `gitlabToken`
- `GITLAB_MR_PROJECT_ID` → `projectId`
- `GITLAB_MR_MERGE_REQUEST_IID` → `mergeRequestIids`
- `GITLAB_MR_CREATE_GITLAB_NOTE` → `createGitlabNote`
- `GITLAB_MR_UPDATE_MR_STATUS` → `updateMrStatus`
- `GITLAB_MR_DRY_RUN` → `dryRun`
- `GITLAB_MR_VERBOSE` → `verbose`
- `GITLAB_MR_INCLUDE_DRAFT_MRS` → `includeDraftMrs`

### ConfigurationService
The main service class that orchestrates configuration loading, merging, and validation.

#### Methods
- `createUnifiedConfig(AppConfig cliConfig, File configFile)`: Creates a unified configuration from various sources with the correct precedence
- `validateConfig(AppConfig config)`: Validates the configuration to ensure it contains all required values and that they are in the correct format

#### Configuration Precedence
When merging configurations from different sources, the following precedence order is applied (highest to lowest):
1. Environment variables
2. Command-line arguments
3. YAML configuration file

#### Validation Rules
- GitLab token is required and must be at least 20 characters and alphanumeric
- GitLab URL is required and must be in a valid URL format
- Project ID is required and must be a positive number
- Merge request IIDs, if provided, must be positive numbers

## Integration Points
- **CLI Package**: Provides command-line arguments for configuration
- **Security Package**: Used for validating input values
- **Service Factory**: Receives the validated configuration for use by other components
