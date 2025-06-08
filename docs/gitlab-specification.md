# GitLab Package Specification

## Overview
The GitLab package is responsible for integrating with the GitLab API to fetch merge request data, analyze potential conflicts, and update GitLab with the results. It provides interfaces and implementations for GitLab operations, including authentication, project access, and merge request management.

## Key Components

### GitLabClient
A composite interface that extends several smaller, focused interfaces for different GitLab API operations, following the Interface Segregation Principle.

#### Extended Interfaces
- **GitLabAuthenticationClient**: For authentication operations
- **GitLabProjectClient**: For project-related operations
- **GitLabMergeRequestClient**: For merge request-related operations

### GitLabAuthenticationClient
An interface for GitLab authentication operations.

#### Methods
- `authenticate(String gitlabUrl, String accessToken)`: Authenticates with GitLab using the provided token
- `hasProjectAccess(Long projectId)`: Checks if the current user has permission to access the project

### GitLabProjectClient
An interface for GitLab project operations.

#### Methods
- `getProject(Long projectId)`: Gets a project by its ID

### GitLabMergeRequestClient
An interface for GitLab merge request operations.

#### Methods
- `getMergeRequests(Long projectId, String state)`: Gets merge requests for a project
- `getMergeRequest(Long projectId, Long mergeRequestIid)`: Gets a specific merge request
- `updateMergeRequestStatus(Long projectId, Long mergeRequestIid, Set<String> labels)`: Updates the merge request status based on conflict detection results
- `getMergeRequestChanges(Long projectId, Long mergeRequestIid)`: Gets the changes for a merge request
- `createMergeRequestNote(Long projectId, Long mergeRequestIid, String noteContent)`: Creates a note (comment) on a merge request

### GitLab4JClient
An implementation of the GitLabClient interface using the GitLab4J API library.

#### Features
- Authenticates with GitLab using personal access tokens
- Caches API responses for better performance
- Implements retry logic for transient API failures
- Handles GitLab API pagination
- Provides error handling and logging

#### Key Methods
- `authenticate(String gitlabUrl, String accessToken)`: Authenticates with GitLab
- `getCurrentUser()`: Gets the current authenticated user
- `getProject(Long projectId)`: Gets a project by its ID
- `getMergeRequests(Long projectId, String state)`: Gets merge requests for a project
- `getMergeRequest(Long projectId, Long mergeRequestIid)`: Gets a specific merge request
- `updateMergeRequestStatus(Long projectId, Long mergeRequestIid, Set<String> labels)`: Updates the merge request status
- `hasProjectAccess(Long projectId)`: Checks if the user has access to a project
- `getMergeRequestChanges(Long projectId, Long mergeRequestIid)`: Gets changes for a merge request
- `createMergeRequestNote(Long projectId, Long mergeRequestIid, String noteContent)`: Creates a note on a merge request
- `withRetry(ThrowingFunction<T, R> operation, String errorMessage)`: Executes an operation with retry logic
- `executeWithRetry(ThrowingRunnable operation, String errorMessage)`: Executes a runnable with retry logic

### MergeRequestService
An interface for fetching merge request data from GitLab API.

#### Methods
- `getMergeRequestsForConflictAnalysis(Long projectId, boolean includeDraftMrs)`: Fetches merge requests that are ready for conflict analysis
- `getOpenMergeRequests(Long projectId)`: Fetches all open merge requests for a project
- `getMergeRequests(Long projectId, String state)`: Fetches merge requests with specific state for a project
- `getMergeRequest(Long projectId, Long mergeRequestIid)`: Fetches a specific merge request by its IID
- `getChangedFiles(Long projectId, Long mergeRequestIid)`: Fetches changed files for a specific merge request

### GitLab4JMergeRequestService
An implementation of the MergeRequestService interface using the GitLab4J API library.

#### Features
- Fetches merge request data from GitLab
- Handles retrieving changed files, including renamed and deleted files
- Converts GitLab API objects to internal model objects
- Determines if a merge request is in draft state based on flags and title prefixes
- Filters out draft/WIP merge requests if requested

#### Key Methods
- `getOpenMergeRequests(Long projectId)`: Gets all open merge requests for a project
- `getMergeRequests(Long projectId, String state)`: Gets merge requests with a specific state for a project
- `getMergeRequest(Long projectId, Long mergeRequestIid)`: Gets a specific merge request by its IID
- `getChangedFiles(Long projectId, Long mergeRequestIid)`: Gets changed files for a specific merge request
- `convertToMergeRequestInfo(MergeRequest mergeRequest, Set<String> changedFiles)`: Converts a GitLab API MergeRequest object to the internal MergeRequestInfo model
- `getMergeRequestsForConflictAnalysis(Long projectId, boolean includeDraftMrs)`: Gets merge requests that are ready for conflict analysis
- `isDraftMergeRequest(MergeRequestInfo mr)`: Checks if a merge request is in the draft state

### GitLabException
A custom exception for GitLab-related errors.

#### Features
- Provides specific error messages for GitLab API failures
- Includes the original exception as the cause
- Used throughout the application to handle GitLab-specific errors

## Integration Points
- **Core Package**: Uses the GitLab package to fetch merge request data and update GitLab with conflict information
- **Model Package**: Provides data models for merge requests and conflicts
- **Security Package**: Used for validating input values and managing credentials
- **Configuration Package**: Provides configuration settings for GitLab connection
