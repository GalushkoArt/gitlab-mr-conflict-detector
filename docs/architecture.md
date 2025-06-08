# GitLab MR Conflict Detector - Architecture Documentation

This document provides a comprehensive overview of the GitLab MR Conflict Detector architecture, including component diagrams, integration points, and dependencies.

## System Overview

The GitLab MR Conflict Detector is a Java application designed to detect potential merge conflicts between multiple merge requests in GitLab repositories. It integrates with the GitLab API to fetch merge request data, analyzes potential conflicts, and can update GitLab with the results.

```plantuml
@startuml
!theme plain
skinparam componentStyle rectangle

package "GitLab MR Conflict Detector" {
  [CLI Application] as CLI
  [Configuration Service] as Config
  [Conflict Analysis Service] as Core
  [GitLab Integration] as GitLab
  [Formatter] as Formatter
  [Security] as Security
  [Dependency Injection] as DI
}

[GitLab API] as GitLabAPI
[User] as User

User --> CLI : uses
CLI --> Config : reads
CLI --> Core : uses
Core --> GitLab : uses
GitLab --> GitLabAPI : calls
Core --> Formatter : uses
Config --> Security : validates
DI --> CLI : injects dependencies
DI --> Core : injects dependencies
DI --> GitLab : injects dependencies
Config --> DI : provides config
DI --> Formatter : injects dependencies

@enduml
```

## Component Architecture

The application is organized into several key components, each with specific responsibilities:

### CLI Component

The CLI component provides the command-line interface for users to interact with the application.

```plantuml
@startuml
!theme plain
skinparam componentStyle rectangle

package "CLI" {
  [SimpleGitLabMultiMergeRequestCommand] as Command
  [CommandLineOptions] as Options
}

[Configuration Service] as Config
[Conflict Analysis Service] as Core

Command --> Options : uses
Command --> Config : configures
Command --> Core : executes
@enduml
```

### Configuration Component

The Configuration component manages application settings and GitLab credentials.

```plantuml
@startuml
!theme plain
skinparam componentStyle rectangle

package "Configuration" {
  [ConfigurationService] as ConfigService
  [ConfigLoader] as ConfigLoader
  [Configuration] as Config
}

[CLI] as CLI
[Security] as Security

CLI --> ConfigService : uses
ConfigService --> ConfigLoader : uses
ConfigLoader --> Config : creates
ConfigService --> Security : validates
@enduml
```

### Core Component

The Core component contains the business logic for detecting conflicts between merge requests.

```plantuml
@startuml
!theme plain
skinparam componentStyle rectangle

package "Core" {
  [ConflictAnalysisService] as AnalysisService
  [MultiMergeRequestConflictDetector] as Detector
  [IgnorePatternMatcher] as Matcher
}

[GitLab Integration] as GitLab
[Formatter] as Formatter

AnalysisService --> Detector : uses
AnalysisService --> GitLab : fetches data
AnalysisService --> Formatter : formats output
Detector --> Matcher : uses
@enduml
```

### GitLab Integration Component

The GitLab Integration component handles communication with the GitLab API.

```plantuml
@startuml
!theme plain
skinparam componentStyle rectangle

package "GitLab Integration" {
  [GitLab4JClient] as Client
  [MergeRequestService] as MRService
}

[GitLab API] as GitLabAPI
[Core] as Core

Core --> Client : authenticates
Client --> GitLabAPI : calls API
MRService --> Client : uses
Core --> MRService : uses
@enduml
```

### Formatter Component

The Formatter component handles the formatting of conflict detection results.

```plantuml
@startuml
!theme plain
skinparam componentStyle rectangle

package "Formatter" {
  [ConflictFormatter] as ConflictFormatter
  [OutputFormatter] as OutputFormatter
}

[Core] as Core

Core --> ConflictFormatter : formats conflicts
ConflictFormatter --> OutputFormatter : uses
@enduml
```

## Integration Points

The application integrates with external systems and components:

### GitLab API Integration

The primary external integration is with the GitLab API:

```plantuml
@startuml
!theme plain
skinparam componentStyle rectangle

package "GitLab MR Conflict Detector" {
  [GitLab Integration] as GitLab
}

[GitLab API] as GitLabAPI

GitLab --> GitLabAPI : REST API calls

note right of GitLabAPI
  - Authentication via Personal Access Token
  - Fetch merge requests
  - Get changed files
  - Create notes
  - Update MR status
end note
@enduml
```

## Data Flow

The following diagram illustrates the data flow through the system:

```plantuml
@startuml
!theme plain
skinparam componentStyle rectangle

actor User
participant CLI
participant Config
participant Core
participant GitLab
participant Formatter
database "GitLab API" as API

User -> CLI : Run command with options
CLI -> Config : Load configuration
Config -> CLI : Return configuration
CLI -> Core : Analyze conflicts
Core -> GitLab : Get merge requests
GitLab -> API : API request
API -> GitLab : Return MR data
GitLab -> Core : Return MR data
Core -> GitLab : Get changed files
GitLab -> API : API request
API -> GitLab : Return changed files
GitLab -> Core : Return changed files
Core -> Core : Detect conflicts
Core -> Formatter : Format results
Formatter -> Core : Return formatted output
Core -> CLI : Return results
CLI -> User : Display results
alt Update GitLab
  CLI -> GitLab : Create notes/update status
  GitLab -> API : API request
  API -> GitLab : Confirm update
  GitLab -> CLI : Confirm update
end
@enduml
```

## Dependencies

The application has the following key dependencies:

### Internal Dependencies

```plantuml
@startuml
!theme plain
skinparam componentStyle rectangle

package "GitLab MR Conflict Detector" {
  [CLI] as CLI
  [Configuration] as Config
  [Core] as Core
  [GitLab Integration] as GitLab
  [Formatter] as Formatter
  [Security] as Security
  [Model] as Model
  [Utils] as Utils
  [Exception] as Exception
}

CLI --> Config : depends on
CLI --> Core : depends on
Core --> GitLab : depends on
Core --> Formatter : depends on
Core --> Model : depends on
GitLab --> Model : depends on
GitLab --> Exception : depends on
Config --> Security : depends on
Config --> Model : depends on
All --> Utils : depend on
@enduml
```

### External Dependencies

- **GitLab4J-API**: Java client for GitLab API
- **Picocli**: Command-line interface framework
- **Jackson**: YAML parsing for configuration files
- **SLF4J/Logback**: Logging framework
- **Lombok**: Reduces boilerplate code

## Security Considerations

```plantuml
@startuml
!theme plain
skinparam componentStyle rectangle

package "Security" {
  [InputValidator] as Validator
  [CredentialManager] as CredManager
}

[Configuration] as Config
[GitLab Integration] as GitLab

Config --> Validator : validates input
Config --> CredManager : manages credentials
GitLab --> CredManager : uses credentials
@enduml
```

The application implements several security measures:
- Input validation to prevent injection attacks
- Secure credential handling (environment variables, secure storage)
- Token validation and minimal permission checking
- No exposure of GitLab token in logs or error messages

## Deployment Architecture

The application can be deployed in various ways:

```plantuml
@startuml
!theme plain
skinparam componentStyle rectangle

node "Developer Workstation" {
  [GitLab MR Conflict Detector] as App1
}

node "CI/CD Pipeline" {
  [GitLab MR Conflict Detector] as App2
}

node "Scheduled Job Server" {
  [GitLab MR Conflict Detector] as App3
}

cloud "GitLab" {
  [GitLab API] as API
}

App1 --> API : API calls
App2 --> API : API calls
App3 --> API : API calls
@enduml
```

## Extension Points

The application is designed with several extension points:

1. **New Conflict Detection Algorithms**: The strategy pattern allows for new conflict detection algorithms.
2. **Additional Output Formats**: The formatter component can be extended with new output formats.
3. **Alternative GitLab API Clients**: The GitLab integration is abstracted behind interfaces.
4. **Custom Configuration Sources**: The configuration system supports multiple sources.
