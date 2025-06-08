# Core Package Specification

## Overview
The Core package contains the business logic for detecting conflicts between merge requests in GitLab. It provides interfaces and implementations for conflict detection, pattern matching, and conflict analysis orchestration.

## Key Components

### ConflictAnalysisService
The main orchestrator of the conflict analysis process. This service coordinates the workflow of authenticating with GitLab, fetching merge requests, detecting conflicts, and updating GitLab with the results.

#### Responsibilities
- Authenticating with GitLab
- Validating GitLab connection and project access
- Fetching merge requests from GitLab
- Detecting conflicts between merge requests
- Formatting conflicts for output
- Updating GitLab with conflict information (creating notes and updating status)

#### Key Methods
- `authenticate()`: Authenticates with GitLab
- `hasAccessToProjectFromConfig()`: Checks if the user has access to the project
- `fetchMergeRequests()`: Fetches merge requests from GitLab
- `detectConflicts(List<MergeRequestInfo> mergeRequests)`: Detects conflicts between merge requests using ignore patterns from configuration
- `formatConflicts(List<MergeRequestConflict> conflicts)`: Formats conflicts for output
- `getConflictingMergeRequestIds(List<MergeRequestConflict> conflicts)`: Gets the IDs of merge requests that have conflicts
- `updateGitLabWithConflicts(List<MergeRequestConflict> conflicts)`: Updates GitLab with conflict information using project ID and other settings from configuration

### ConflictDetector
An interface that defines the contract for detecting conflicts between merge requests.

#### Methods
- `detectConflicts(List<MergeRequestInfo> mergeRequests, List<String> ignorePatterns)`: Detects conflicts between multiple merge requests
- `getConflictingMergeRequestIds(List<MergeRequestConflict> conflicts)`: Gets the list of merge request IDs that have conflicts

### MultiMergeRequestConflictDetector
An implementation of the ConflictDetector interface that detects conflicts between multiple merge requests using various strategies.

#### Features
- Uses the Strategy pattern to allow different conflict detection algorithms
- Supports multiple conflict detection strategies
- Checks all pairs of merge requests with all strategies
- Collects all detected conflicts into a list

#### Key Methods
- `detectConflicts(List<MergeRequestInfo> mergeRequests, List<String> ignorePatterns)`: Detects conflicts between multiple merge requests
- `getConflictingMergeRequestIds(List<MergeRequestConflict> conflicts)`: Gets the list of merge request IDs that have conflicts
- `addStrategy(ConflictDetectionStrategy strategy)`: Adds a conflict detection strategy

### PatternMatcher
An interface for pattern matching functionality.

#### Methods
- `matches(String pattern, String filePath)`: Checks if a file path matches the given pattern

### IgnorePatternMatcher
An implementation of the PatternMatcher interface that provides sophisticated pattern matching for ignoring files/directories in conflict detection.

#### Features
- Supports directory patterns (e.g., "dir/")
- Supports glob patterns (e.g., "*.txt")
- Supports case-sensitive and case-insensitive matching
- Supports extended glob syntax

#### Key Methods
- `matches(String pattern, String filePath)`: Checks if a file path matches the given pattern
- `matchesDirectoryPattern(String normalizedPath, String normalizedPattern)`: Checks if a path matches a directory pattern
- `matchesGlobPattern(String normalizedPath, String normalizedPattern)`: Checks if a path matches a glob pattern
- `convertToJavaGlob(String pattern)`: Converts a pattern to a Java glob pattern
- `normalizePath(String path)`: Normalizes paths for consistent matching

### Strategy Package

#### ConflictDetectionStrategy
An interface that defines the contract for conflict detection strategies.

##### Methods
- `detectConflict(MergeRequestInfo mr1, MergeRequestInfo mr2, List<String> ignorePatterns)`: Detects if there is a conflict between two merge requests
- `getStrategyName()`: Gets the name of the strategy

#### DefaultConflictDetectionStrategy
The default implementation of the ConflictDetectionStrategy interface.

##### Features
- Checks for dependency relationships between merge requests
- Determines the reason for conflicts
- Handles file ignoring based on patterns
- Checks for overlapping code paths

##### Key Methods
- `detectConflict(MergeRequestInfo mr1, MergeRequestInfo mr2, List<String> ignorePatterns)`: Detects if there is a conflict between two merge requests
- `hasDependencyRelationship(MergeRequestInfo mr1, MergeRequestInfo mr2)`: Checks if there's a dependency relationship between two merge requests
- `determineConflictReason(MergeRequestInfo mr1, MergeRequestInfo mr2)`: Determines the reason for a conflict
- `isFileIgnored(String filePath, List<String> ignorePatterns)`: Checks if a file should be ignored based on ignore patterns
- `hasOverlappingCodePaths(MergeRequestInfo mr1, MergeRequestInfo mr2)`: Checks if two merge requests have overlapping code paths

## Integration Points
- **GitLab Package**: For fetching merge request data and updating GitLab with conflict information
- **Formatter Package**: For formatting conflicts for output
- **Model Package**: For data models like MergeRequestInfo and MergeRequestConflict
- **Configuration Package**: For accessing configuration settings
