# GitLab MR Conflict Detector

[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://openjdk.java.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Gradle](https://img.shields.io/badge/Gradle-8.x-blue.svg)](https://gradle.org/)

A comprehensive Java automation tool for detecting merge conflicts in GitLab merge requests with **GitLab API
integration** for dynamic merge request data fetching and comprehensive multi-MR conflict detection.

## 🚀 Features

### GitLab API Integration

- **Dynamic MR Fetching**: Automatically fetch merge request data from GitLab API
- **Real-time File Analysis**: Get changed files directly from GitLab diffs
- **Multi-MR Conflict Detection**: Analyze conflicts between multiple merge requests
- **GitLab Integration**: Create notes and update MR status based on conflicts

### Enhanced Conflict Detection

- **Cross-MR Analysis**: Detect conflicts between multiple merge requests
- **Dependency Chain Logic**: Intelligent handling of MR dependency relationships
- **Advanced Ignore Patterns**: File/directory exclusion support with glob patterns
- **Real-time Data**: Always up-to-date with latest MR changes

## 📋 Prerequisites

- **Java 21 or higher** (LTS recommended)
- **GitLab instance** with API access
- **GitLab Personal Access Token** with appropriate permissions:
    - `api` scope for full API access
    - `read_repository` for accessing merge request diffs
    - `write_repository` for creating notes (optional)

## 🛠️ Installation

### Option 1: Build from Source

```bash
git clone https://github.com/GalushkoArt/gitlab-mr-conflict-detector.git
cd gitlab-mr-conflict-detector
./gradlew build
```

The built JAR will be available at `build/libs/gitlab-mr-conflict-detector-1.0.0.jar`.

### Option 2: Download Release

Download the latest release from the [Releases page](https://github.com/GalushkoArt/gitlab-mr-conflict-detector/releases) and run:

```bash
java -jar gitlab-mr-conflict-detector-1.0.0.jar --help
```

## 🚀 Usage

### Basic Multi-MR Conflict Detection

Analyze all open merge requests in a GitLab project:

```bash
java -jar gitlab-mr-conflict-detector-1.0.0.jar \
  --gitlab-url https://gitlab.example.com \
  --gitlab-token your-access-token \
  --project-id 123
```

### Analyze Specific Merge Request

```bash
java -jar gitlab-mr-conflict-detector-1.0.0.jar \
  --gitlab-url https://gitlab.example.com \
  --gitlab-token your-access-token \
  --project-id 123 \
  --mr-iid 45
```

### Create Notes and Update Status

Create notes on conflicting merge requests and update their status:

```bash
java -jar gitlab-mr-conflict-detector-1.0.0.jar \
  --gitlab-url https://gitlab.example.com \
  --gitlab-token your-access-token \
  --project-id 123 \
  --create-gitlab-note \
  --update-mr-status
```

### Using Configuration File

```bash
java -jar gitlab-mr-conflict-detector-1.0.0.jar \
  --config-file config.yml
```

## ⚙️ Configuration

### Command Line Options

| Option                 | Description                                    | Required |
|------------------------|------------------------------------------------|----------|
| `--gitlab-url`         | GitLab instance URL                            | Yes      |
| `--gitlab-token`       | GitLab personal access token                   | Yes      |
| `--project-id`         | GitLab project ID                              | Yes      |
| `--mr-iid`             | Specific merge request IID to analyze          | No       |
| `--create-gitlab-note` | Create notes on merge requests with conflicts  | No       |
| `--update-mr-status`   | Update merge request status based on conflicts | No       |
| `--dry-run`            | Perform dry run without making changes         | No       |
| `--verbose`            | Enable verbose logging                         | No       |
| `--config-file`        | Path to YAML configuration file                | No       |
| `--ignore-patterns`    | Patterns for files/directories to ignore       | No       |
| `--include-draft-mrs`  | Include draft/WIP merge requests in analysis   | No       |

Required options can be passed via Env variables or via config file

### Environment Variables

You can also set GitLab credentials via environment variables with `GITLAB_MR_` prefix in upper snake case:

```bash
export GITLAB_MR_GITLAB_URL=https://gitlab.example.com
export GITLAB_MR_GITLAB_TOKEN=your-access-token
export GITLAB_MR_PROJECT_ID=123
```

### Configuration File Format

Create a `config.yml` file with the following structure:

```yaml
# GitLab connection settings
gitlabUrl: https://gitlab.example.com
gitlabToken: your-access-token
projectId: 123

# Behavior settings
createGitlabNote: true
updateMrStatus: true
dryRun: false
verbose: true
includeDraftMrs: false

# Conflict detection settings
ignorePatterns:
  - "**/ignored.txt"
  - "ignored_dir/*"
```

## 📊 Sample Output

```
Conflict Detection Results:
==========================

"Add user authentication system" vs "Implement new UI components"
- Issue: conflict in modification of `src/app.js`

"Add user authentication system" vs "Optimize database queries for better performance"  
- Issue: conflict in modification of `tests/unit.test.js`

"Fix critical security vulnerability" vs "Optimize database queries for better performance"
- Issue: conflict in modification of `tests/unit.test.js`

"Update configuration constants" vs "Refactor constants for maintainability"
- Issue: conflict in modification of `src/consts.js`

Summary: 4 conflicts detected across 6 merge requests
```

## 🔐 GitLab Token Permissions

Your GitLab personal access token needs the following scopes:

- **`api`**: Full API access for reading merge requests and creating notes
- **`read_repository`**: Access to repository data and merge request diffs

Optional scopes for enhanced functionality:

- **`write_repository`**: For updating merge request labels and status

## 📚 Documentation

For detailed documentation about the project architecture and implementation:

- [Architecture Documentation](docs/architecture.md): Detailed system architecture with component diagrams
- [CLI Specification](docs/cli-specification.md): Command-line interface documentation
- [Core Specification](docs/core-specification.md): Business logic and conflict detection algorithms
- [Configuration Specification](docs/config-specification.md): Configuration options and formats
- [GitLab Integration](docs/gitlab-specification.md): GitLab API integration details
- [Formatter Specification](docs/formatter-specification.md): Output formatting options

## 🧪 Testing

Run the test suite:

```bash
./gradlew test
```

Test with a real GitLab instance (dry run):

```bash
java -jar gitlab-mr-conflict-detector-1.0.0.jar \
  --gitlab-url https://gitlab.example.com \
  --gitlab-token your-token \
  --project-id 123 \
  --verbose \
  --dry-run
```

The multi-MR conflict detection logic is thoroughly tested with unit tests covering:

- Direct conflict detection
- Dependency relationship logic
- Ignore pattern functionality
- Cross-branch conflict detection

## 🔍 Conflict Detection Logic

The tool follows this workflow:

1. **Fetch Merge Requests**: Get all open merge requests from GitLab API
2. **Get Changed Files**: For each MR, fetch the list of changed files from GitLab diffs
3. **Analyze Conflicts**: Compare all MR pairs for file conflicts
4. **Apply Rules**: Use dependency chain logic and ignore patterns
5. **Report Results**: Generate formatted output and optionally update GitLab

### Conflict Types

**Direct Conflicts:**

- Multiple MRs target the same branch
- Both MRs modify the same file(s)
- No dependency relationship exists

**Cross-Branch Conflicts:**

- MRs target different branches
- Both MRs modify the same file(s)
- Potential integration conflicts

**Dependency Relationships (No Conflict):**

- One MR's source branch is another MR's target branch
- Indicates a merge chain where conflicts are expected to be resolved naturally

## 🏗️ Architecture

The application is organized into several key components:

- **CLI Component**: Command-line interface using Picocli framework
- **Configuration Component**: Settings and credentials management
- **Core Component**: Business logic for conflict detection with Strategy pattern
- **GitLab Integration**: API interaction layer using GitLab4J
- **Formatter Component**: Output formatting and display
- **Security Component**: Validation and security measures

## 🛠️ Development

### Building

```bash
./gradlew build
```

### Running Tests

```bash
./gradlew test
```

### Creating Fat JAR

```bash
./gradlew shadowJar
```

### Code Style

The project uses:

- Java 21 features and syntax
- Lombok for reducing boilerplate code
- SLF4J with Logback for logging
- JUnit 5 for testing
- Mockito for mocking

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🐛 Troubleshooting

### Common Issues

**Authentication Errors:**

- Verify your GitLab token has the required scopes
- Check that the GitLab URL is correct and accessible
- Ensure the project ID exists and you have access

**Connection Issues:**

- Verify network connectivity to GitLab instance
- Check for proxy settings if behind corporate firewall
- Validate SSL certificates if using self-signed certificates

**Performance Issues:**

- Use `--mr-iid` to analyze specific merge requests
- Consider using ignore patterns to exclude large directories
- Enable caching for repeated API calls

**No Conflicts Detected:**

- Verify merge requests have actual file changes
- Check that ignore patterns aren't excluding all files
- Ensure merge requests target the same or related branches

### Debug Mode

Enable verbose logging for troubleshooting:

```bash
java -jar gitlab-mr-conflict-detector-1.0.0.jar \
  --verbose \
  --dry-run \
  [other options]
```

## 📞 Support

- Create an [issue](https://github.com/GalushkoArt/gitlab-mr-conflict-detector/issues) for bug reports
- Start a [discussion](https://github.com/GalushkoArt/gitlab-mr-conflict-detector/discussions) for questions
- Check the [documentation](docs/) for detailed information