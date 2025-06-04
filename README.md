# GitLab Merge Request Conflict Detector

A comprehensive Java automation tool for detecting merge conflicts in GitLab merge requests. This tool provides advanced conflict detection capabilities with support for multiple target branches, file filtering, and configurable detection rules.

## Features

### Core Functionality
- **Automatic Conflict Detection**: Detects merge conflicts at each run using advanced Git analysis
- **Status Reporting**: Provides clear, detailed conflict status messages
- **Multiple Output Formats**: Supports TEXT, JSON, and YAML output formats

### Target Branch Handling
- **Multi-target Support**: Detect conflicts against multiple specified target branches
- **Protected Branches**: Automatically prioritizes conflict detection for protected branches
- **Auto-detection**: Automatically determines target branches when not specified

### Advanced Merge Support
- **Recursive Merge Analysis**: Handles complex merge histories and recursive merge scenarios
- **Three-way Merge**: Performs sophisticated three-way merge analysis
- **Merge Base Detection**: Finds common ancestors for accurate conflict detection

### File and Directory Filtering
- **Include/Exclude Patterns**: Support for glob patterns for file and directory inclusion/exclusion
- **Case Sensitivity**: Configurable case-sensitive or case-insensitive pattern matching
- **Default Exclusions**: Sensible defaults for common build artifacts and system files

### Configuration Management
- **Project-level Settings**: Per-project configuration of detection rules
- **Branch-specific Rules**: Different filtering rules for different target branches
- **Sensitivity Levels**: Configurable detection sensitivity (STRICT, NORMAL, PERMISSIVE)
- **Force Pass**: Override conflict detection results via environment variable

### GitLab Integration
- **API Integration**: Full GitLab API integration for project and branch management
- **Merge Request Notes**: Automatically create notes on merge requests with conflict results
- **Status Updates**: Update merge request status based on conflict detection results
- **Protected Branch Detection**: Automatically identifies and prioritizes protected branches

## Requirements

- Java 21 or higher
- Git repository access
- GitLab instance with API access
- GitLab Personal Access Token with appropriate permissions

## Installation

### Download Pre-built JAR

Download the latest release from the releases page and run:

```bash
java -jar gitlab-mr-conflict-detector-1.0.0.jar --help
```

### Build from Source

```bash
git clone https://github.com/your-org/gitlab-mr-conflict-detector.git
cd gitlab-mr-conflict-detector
./gradlew build
```

The built JAR will be available at `build/libs/gitlab-mr-conflict-detector-1.0.0.jar`.

## Quick Start

### Basic Usage

```bash
# Detect conflicts for current branch against default targets
java -jar gitlab-mr-conflict-detector.jar \
  --gitlab-url https://gitlab.example.com \
  --gitlab-token your-access-token \
  --project-id 123

# Detect conflicts for specific branches
java -jar gitlab-mr-conflict-detector.jar \
  --gitlab-url https://gitlab.example.com \
  --gitlab-token your-access-token \
  --project-id 123 \
  --source-branch feature/new-feature \
  --target-branches main,develop
```

### Environment Variables

Set environment variables for easier usage:

```bash
export GITLAB_URL=https://gitlab.example.com
export GITLAB_TOKEN=your-access-token
export GITLAB_PROJECT_ID=123

# Now you can run with minimal arguments
java -jar gitlab-mr-conflict-detector.jar --source-branch feature/new-feature
```

### Force Pass Mode

Override conflict detection results:

```bash
export CONFLICT_DETECTOR_FORCE_PASS=true
java -jar gitlab-mr-conflict-detector.jar --source-branch feature/new-feature
```

## Configuration

### Configuration File

Create a `config.yml` file for advanced configuration:

```yaml
gitlab:
  url: https://gitlab.example.com
  token: your-access-token
  projectId: 123
  timeoutSeconds: 30
  verifySSL: true

detection:
  fetchBeforeDetection: true
  sensitivity: NORMAL
  useRecursiveMerge: true
  maxConflictSections: 100

fileFilter:
  includePatterns:
    - "**/*.java"
    - "**/*.kt"
    - "**/*.js"
    - "**/*.ts"
  excludePatterns:
    - "**/.git/**"
    - "**/node_modules/**"
    - "**/target/**"
    - "**/build/**"
    - "**/.gradle/**"
    - "**/*.class"
    - "**/*.jar"
    - "**/*.log"
  caseSensitive: true
  followSymlinks: false
  maxFileSizeBytes: 10485760  # 10MB

forcePass: false
```

Use the configuration file:

```bash
java -jar gitlab-mr-conflict-detector.jar --config config.yml
```

### Project-specific Configuration

For multiple projects, create project-specific configurations:

```yaml
gitlab:
  url: https://gitlab.example.com
  token: your-access-token

projects:
  - projectId: 123
    targetBranches: ["main", "develop"]
    detection:
      sensitivity: STRICT
    fileFilter:
      includePatterns: ["**/*.java"]
      
  - projectId: 456
    targetBranches: ["master", "staging"]
    detection:
      sensitivity: PERMISSIVE
    fileFilter:
      includePatterns: ["**/*.js", "**/*.ts"]
```

## Command Line Options

### Required Options

- `--gitlab-url`: GitLab instance URL
- `--gitlab-token`: GitLab personal access token
- `--project-id` or `--project-path`: GitLab project identifier

### Optional Options

- `--source-branch`: Source branch for conflict detection (default: current branch)
- `--target-branches`: Comma-separated list of target branches
- `--config`: Path to configuration file
- `--mr-iid`: Merge request internal ID to analyze
- `--include-patterns`: File patterns to include (glob syntax)
- `--exclude-patterns`: File patterns to exclude (glob syntax)
- `--sensitivity`: Detection sensitivity (STRICT, NORMAL, PERMISSIVE)
- `--force-pass`: Force pass conflicts
- `--no-fetch`: Skip fetching latest changes
- `--output-format`: Output format (TEXT, JSON, YAML)
- `--output-file`: Output file path
- `--create-gitlab-note`: Create note on merge request
- `--update-mr-status`: Update merge request status
- `--verbose`: Enable verbose logging
- `--dry-run`: Perform dry run without changes

## GitLab Integration

### Personal Access Token

Create a GitLab Personal Access Token with the following scopes:
- `api`: Full API access
- `read_repository`: Read repository content
- `write_repository`: Write repository content (if updating MR status)

### Permissions

Ensure your GitLab user has the following permissions:
- **Reporter** or higher access to the project
- **Developer** access to create notes on merge requests
- **Maintainer** access to update merge request status

### Webhook Integration

For automated conflict detection, set up a GitLab webhook:

1. Go to Project Settings → Webhooks
2. Add webhook URL pointing to your automation system
3. Enable "Merge request events"
4. Configure your automation to call the conflict detector

Example webhook handler:

```bash
#!/bin/bash
# webhook-handler.sh

PROJECT_ID=$1
MR_IID=$2
SOURCE_BRANCH=$3

java -jar gitlab-mr-conflict-detector.jar \
  --project-id $PROJECT_ID \
  --mr-iid $MR_IID \
  --source-branch $SOURCE_BRANCH \
  --create-gitlab-note \
  --update-mr-status
```

## Output Formats

### Text Output (Default)

```
GitLab Merge Request Conflict Detection Report
==================================================

Summary:
  Total target branches checked: 2
  Branches with conflicts: 1
  Overall status: CONFLICTS DETECTED

Target Branch: main
------------------------------
  Source Branch: feature/new-feature
  Source Commit: abc123def456
  Target Commit: def456ghi789
  Status: CLEAN
  No conflicts detected
  Timestamp: 2024-01-15T10:30:00

Target Branch: develop
------------------------------
  Source Branch: feature/new-feature
  Source Commit: abc123def456
  Target Commit: ghi789jkl012
  Status: CONFLICTED
  Conflicts (2 files):
    - src/main/java/Main.java (content)
      Conflict sections: 1
    - README.md (content)
      Conflict sections: 2
  Timestamp: 2024-01-15T10:30:00
```

### JSON Output

```json
{
  "version": "1.0.0",
  "timestamp": "2024-01-15T10:30:00Z",
  "summary": {
    "totalTargetBranches": 2,
    "branchesWithConflicts": 1,
    "totalConflicts": 2,
    "overallStatus": "CONFLICTS_DETECTED"
  },
  "results": [
    {
      "sourceBranch": "feature/new-feature",
      "targetBranch": "main",
      "sourceCommit": "abc123def456",
      "targetCommit": "def456ghi789",
      "status": "CLEAN",
      "conflicts": [],
      "message": "No conflicts detected"
    }
  ]
}
```

## CI/CD Integration

### GitLab CI

Add to your `.gitlab-ci.yml`:

```yaml
conflict-detection:
  stage: test
  image: openjdk:21-jdk
  script:
    - wget https://github.com/your-org/gitlab-mr-conflict-detector/releases/download/v1.0.0/gitlab-mr-conflict-detector-1.0.0.jar
    - java -jar gitlab-mr-conflict-detector-1.0.0.jar
        --project-id $CI_PROJECT_ID
        --source-branch $CI_MERGE_REQUEST_SOURCE_BRANCH_NAME
        --mr-iid $CI_MERGE_REQUEST_IID
        --create-gitlab-note
  only:
    - merge_requests
  variables:
    GITLAB_URL: $CI_SERVER_URL
    GITLAB_TOKEN: $GITLAB_API_TOKEN
```

### Jenkins

```groovy
pipeline {
    agent any
    
    environment {
        GITLAB_URL = 'https://gitlab.example.com'
        GITLAB_TOKEN = credentials('gitlab-api-token')
    }
    
    stages {
        stage('Conflict Detection') {
            steps {
                sh '''
                    java -jar gitlab-mr-conflict-detector.jar \
                        --project-id ${PROJECT_ID} \
                        --source-branch ${BRANCH_NAME} \
                        --create-gitlab-note
                '''
            }
        }
    }
}
```

## Troubleshooting

### Common Issues

1. **Authentication Failed**
   - Verify GitLab URL and access token
   - Check token permissions and expiration
   - Ensure project access permissions

2. **Branch Not Found**
   - Verify branch names are correct
   - Check if branches exist in the repository
   - Ensure fetch is enabled to get latest branches

3. **Permission Denied**
   - Verify user has required project permissions
   - Check if project is private and accessible
   - Ensure token has appropriate scopes

4. **Git Operation Failed**
   - Verify repository is accessible
   - Check Git repository integrity
   - Ensure sufficient disk space

### Debug Mode

Enable verbose logging for troubleshooting:

```bash
java -jar gitlab-mr-conflict-detector.jar --verbose --dry-run
```

### Log Files

Logs are written to stdout/stderr. To capture logs:

```bash
java -jar gitlab-mr-conflict-detector.jar 2>&1 | tee conflict-detection.log
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Run the test suite: `./gradlew test`
6. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions:
- Create an issue on GitHub
- Check the troubleshooting section
- Review the configuration examples

## Changelog

### v1.0.0
- Initial release
- Core conflict detection functionality
- GitLab API integration
- File filtering support
- Multiple output formats
- Configuration management
- CLI interface

