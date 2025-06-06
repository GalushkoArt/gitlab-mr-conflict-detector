# GitLab MR Conflict Detector - Enhanced with GitLab API Integration

A comprehensive Java automation tool for detecting merge conflicts in GitLab merge requests, now with **GitLab API integration** for dynamic merge request data fetching and comprehensive multi-MR conflict detection.

## 🚀 New Features

### GitLab API Integration
- **Dynamic MR Fetching**: Automatically fetch merge request data from GitLab API
- **Real-time File Analysis**: Get changed files directly from GitLab diffs
- **Multi-MR Conflict Detection**: Analyze conflicts between multiple merge requests
- **GitLab Integration**: Create notes and update MR status based on conflicts

### Enhanced Conflict Detection
- **Cross-MR Analysis**: Detect conflicts between multiple merge requests
- **Dependency Chain Logic**: Intelligent handling of MR dependency relationships
- **Advanced Ignore Patterns**: File/directory exclusion support
- **Real-time Data**: Always up-to-date with latest MR changes

## 📋 Requirements

- Java 21 or higher
- GitLab instance with API access
- GitLab Personal Access Token with appropriate permissions:
   - `api` scope for full API access
   - `read_repository` for accessing merge request diffs
   - `write_repository` for creating notes (optional)

## 🔧 Installation

### Download Release
Download the latest release and run:

```bash
java -jar gitlab-mr-conflict-detector-1.0.0.jar --help
```

### Build from Source
```bash
git clone https://github.com/GalushkoArt/gitlab-mr-conflict-detector.git
cd gitlab-mr-conflict-detector
./gradlew build
```

The built JAR will be available at `build/libs/gitlab-mr-conflict-detector-1.0.0.jar`.

## 🎯 Usage

### GitLab Multi-MR Conflict Detection (New)

Analyze all open merge requests in a GitLab project:

```bash
java -cp gitlab-mr-conflict-detector-1.0.0.jar \
  art.galushko.gitlab.mrconflict.cli.SimpleGitLabMultiMergeRequestCommand \
  --gitlab-url https://gitlab.example.com \
  --gitlab-token your-access-token \
  --project-id 123
```

### Analyze Specific Merge Request

```bash
java -cp gitlab-mr-conflict-detector-1.0.0.jar \
  art.galushko.gitlab.mrconflict.cli.SimpleGitLabMultiMergeRequestCommand \
  --gitlab-url https://gitlab.example.com \
  --gitlab-token your-access-token \
  --project-id 123 \
  --mr-iid 45
```

### With GitLab Integration

Create notes on conflicting merge requests and update their status:

```bash
java -cp gitlab-mr-conflict-detector-1.0.0.jar \
  art.galushko.gitlab.mrconflict.cli.SimpleGitLabMultiMergeRequestCommand \
  --gitlab-url https://gitlab.example.com \
  --gitlab-token your-access-token \
  --project-id 123 \
  --create-gitlab-note \
  --update-mr-status
```

### Expected Output (Updated with Titles)
```
"Add user authentication system" vs "Implement new UI components"
- Issue: conflict in modification of `src/app.js`
"Add user authentication system" vs "Optimize database queries for better performance"
- Issue: conflict in modification of `tests/unit.test.js`
"Fix critical security vulnerability" vs "Optimize database queries for better performance"
- Issue: conflict in modification of `tests/unit.test.js`
"Update configuration constants" vs "Refactor constants for maintainability"
- Issue: conflict in modification of `src/consts.js`
```

## ⚙️ Configuration

### Command Line Options

| Option | Description | Required |
|--------|-------------|----------|
| `--gitlab-url` | GitLab instance URL | Yes |
| `--gitlab-token` | GitLab personal access token | Yes |
| `--project-id` | GitLab project ID | Yes |
| `--mr-iid` | Specific merge request IID to analyze | No |
| `--create-gitlab-note` | Create notes on merge requests with conflicts | No |
| `--update-mr-status` | Update merge request status based on conflicts | No |
| `--dry-run` | Perform dry run without making changes | No |
| `--verbose` | Enable verbose logging | No |

### GitLab Token Permissions

Your GitLab personal access token needs the following scopes:

- **`api`**: Full API access for reading merge requests and creating notes
- **`read_repository`**: Access to repository data and merge request diffs

Optional scopes for enhanced functionality:
- **`write_repository`**: For updating merge request labels and status

### Environment Variables

You can also set GitLab credentials via environment variables:

```bash
export GITLAB_URL=https://gitlab.example.com
export GITLAB_TOKEN=your-access-token
export GITLAB_PROJECT_ID=123
```

## 🏗️ Architecture

### Documentation

For detailed documentation about the project architecture and code, please refer to:

- [Architecture Documentation](docs/architecture.md): Detailed system architecture with component diagrams and integration points

### New Components

#### GitLab Integration (`src/main/java/art/galushko/gitlab/mrconflict/gitlab/`)
- **`MergeRequestService`**: Interface for fetching MR data
- **`GitLab4JMergeRequestService`**: Implementation using GitLab4J API
- **Enhanced `GitLab4JClient`**: Extended with public API access

#### CLI Application (`src/main/java/art/galushko/gitlab/mrconflict/cli/`)
- **`SimpleGitLabMultiMergeRequestCommand`**: Self-contained CLI for GitLab integration

### Key Features

#### Dynamic Data Fetching
```java
// Fetch all open merge requests
List<MergeRequestInfo> mergeRequests = service.getOpenMergeRequests(projectId);

// Get changed files for each MR
List<String> changedFiles = service.getChangedFiles(projectId, mergeRequestIid);
```

#### Conflict Detection
```java
// Detect conflicts between multiple MRs
var detector = new MultiMergeRequestConflictDetector(ignorePatternMatcher);
var conflicts = detector.detectConflicts(mergeRequests, ignorePatterns);
```

#### GitLab Integration
```java
// Create conflict notes on merge requests
gitlabClient.getGitLabApi().getNotesApi()
    .createMergeRequestNote(projectId, mergeRequestIid, noteContent);

// Update merge request status
gitlabClient.updateMergeRequestStatus(projectId, mergeRequestIid, hasConflicts);
```

## 🧪 Testing

### Manual Testing

Test with a real GitLab instance:

```bash
# Test connection and basic functionality
java -cp gitlab-mr-conflict-detector-1.0.0.jar \
  art.galushko.gitlab.mrconflict.cli.SimpleGitLabMultiMergeRequestCommand \
  --gitlab-url https://gitlab.example.com \
  --gitlab-token your-token \
  --project-id 123 \
  --verbose \
  --dry-run
```

### Unit Tests

Run the existing unit test suite:

```bash
./gradlew test
```

The multi-MR conflict detection logic is thoroughly tested with unit tests covering:
- Direct conflict detection
- Dependency relationship logic
- Ignore pattern functionality
- Cross-branch conflict detection

## 🔍 Conflict Detection Logic

### How It Works

1. **Fetch Merge Requests**: Get all open merge requests from GitLab API
2. **Get Changed Files**: For each MR, fetch the list of changed files from GitLab diffs
3. **Analyze Conflicts**: Compare all MR pairs for file conflicts
4. **Apply Rules**: Use dependency chain logic and ignore patterns
5. **Report Results**: Generate formatted output and optionally update GitLab

### Conflict Types

#### Direct Conflicts
- Multiple MRs target the same branch
- Both MRs modify the same file(s)
- No dependency relationship exists

#### Cross-Branch Conflicts
- MRs target different branches
- Both MRs modify the same file(s)
- Potential integration conflicts

#### Dependency Relationships (No Conflict)
- One MR's source branch is another MR's target branch
- Indicates a merge chain where conflicts are expected to be resolved naturally

### GitLab Integration Features

#### Conflict Notes
When `--create-gitlab-note` is enabled, the tool creates detailed notes on merge requests:

```markdown
## ⚠️ Merge Request Conflicts Detected

This merge request has conflicts with other open merge requests:

- **Conflict with MR42**: conflict in modification of `src/app.js`
- **Conflict with MR43**: conflict in modification of `tests/unit.test.js`

**Action Required:**
- Review the conflicting files
- Coordinate with other MR authors
- Consider rebasing or merging order
```

#### Status Updates
When `--update-mr-status` is enabled, the tool:
- Adds `conflicts` label to conflicting merge requests
- Provides clear visual indication in GitLab UI

## 📊 Performance

### Scalability
- **API Efficiency**: Batched API calls where possible
- **Memory Usage**: Efficient processing of large MR lists
- **Network Optimization**: Minimal API calls for maximum data

### Typical Performance
- **Small projects** (< 10 MRs): < 5 seconds
- **Medium projects** (10-50 MRs): < 30 seconds
- **Large projects** (50+ MRs): < 2 minutes

## 🔧 Troubleshooting

### Common Issues

**Issue**: "No access to project" error
- **Solution**: Check GitLab token permissions and project ID
- **Debug**: Verify token has `api` scope and project exists

**Issue**: "No merge requests found"
- **Solution**: Ensure project has open merge requests
- **Debug**: Check project ID and MR state filters

**Issue**: Empty changed files list
- **Solution**: Verify token has `read_repository` scope
- **Debug**: Check merge request has actual file changes

### Debug Mode

Enable verbose logging to see detailed API interactions:

```bash
java -cp gitlab-mr-conflict-detector-1.0.0.jar \
  art.galushko.gitlab.mrconflict.cli.SimpleGitLabMultiMergeRequestCommand \
  --gitlab-url https://gitlab.example.com \
  --gitlab-token your-token \
  --project-id 123 \
  --verbose
```

Debug output includes:
- GitLab API authentication status
- Merge request fetching progress
- File change analysis
- Conflict detection reasoning

## 🚀 Advanced Usage

### CI/CD Integration

Use in GitLab CI/CD pipelines:

```yaml
stages:
  - conflict-check

conflict-detection:
  stage: conflict-check
  image: openjdk:21-jdk
  script:
    - wget https://github.com/GalushkoArt/gitlab-mr-conflict-detector/releases/download/v1.1.0/gitlab-mr-conflict-detector-1.0.0.jar
    - java -cp gitlab-mr-conflict-detector-1.0.0.jar 
        art.galushko.gitlab.mrconflict.cli.SimpleGitLabMultiMergeRequestCommand
        --gitlab-url $CI_SERVER_URL
        --gitlab-token $GITLAB_TOKEN
        --project-id $CI_PROJECT_ID
        --create-gitlab-note
  only:
    - merge_requests
```

### Scheduled Conflict Monitoring

Run periodic conflict checks:

```bash
#!/bin/bash
# Daily conflict check script

java -cp gitlab-mr-conflict-detector-1.0.0.jar \
  art.galushko.gitlab.mrconflict.cli.SimpleGitLabMultiMergeRequestCommand \
  --gitlab-url https://gitlab.example.com \
  --gitlab-token $GITLAB_TOKEN \
  --project-id 123 \
  --create-gitlab-note \
  --update-mr-status

if [ $? -eq 1 ]; then
    echo "Conflicts detected - notifications sent to affected MRs"
fi
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Setup

```bash
git clone https://github.com/GalushkoArt/gitlab-mr-conflict-detector.git
cd gitlab-mr-conflict-detector
./gradlew build
./gradlew test
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Original GitLab MR Conflict Detector by GalushkoArt
- Enhanced with GitLab API integration and multi-MR capabilities
- Built with Java 21, GitLab4J API, and modern development practices

---

## 📈 Changelog

### Version 1.1.0 (GitLab API Integration)
- ✨ **NEW**: GitLab API integration for dynamic MR fetching
- ✨ **NEW**: Real-time changed files analysis from GitLab diffs
- ✨ **NEW**: Multi-MR conflict detection with GitLab data
- ✨ **NEW**: Conflict notes creation on merge requests
- ✨ **NEW**: Merge request status updates based on conflicts
- ✨ **NEW**: Comprehensive CLI with GitLab authentication
- 🔧 **IMPROVED**: Performance with efficient API usage
- 🔧 **IMPROVED**: Error handling and debugging capabilities
- 📚 **DOCS**: Complete documentation for GitLab integration

### Version 1.0.0 (Original)
- 🎯 GitLab API integration
- 🎯 Single MR conflict detection
- 🎯 Multiple output formats
- 🎯 File filtering capabilities
