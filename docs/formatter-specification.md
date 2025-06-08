# Formatter Package Specification

## Overview
The Formatter package is responsible for formatting conflict information in a human-readable format. It provides interfaces and implementations for formatting conflicts, conflict lists, and conflict notes for GitLab merge requests.

## Key Components

### ConflictFormatter
An interface that defines the contract for formatting conflict information.

#### Methods
- `formatConflict(MergeRequestConflict conflict)`: Formats a single conflict
- `formatConflicts(List<MergeRequestConflict> conflicts)`: Formats a list of conflicts
- `formatConflictNote(List<MergeRequestConflict> conflicts, Long mergeRequestIid, List<MergeRequest> resolvedConflictMrs)`: Formats a note with conflict information for a specific merge request

### DefaultConflictFormatter
The default implementation of the ConflictFormatter interface that formats conflicts in a human-readable text format.

#### Features
- Formats single conflicts with merge request titles and conflict descriptions
- Formats lists of conflicts by joining individual conflict formats
- Creates detailed notes for GitLab merge requests with conflict information
- Includes information about resolved conflicts
- Limits the number of files shown in a note to avoid huge comments
- Provides different formatting for single vs. multiple conflicting files
- Truncates merge request titles to a reasonable length for display

#### Key Methods
- `formatConflict(MergeRequestConflict conflict)`: Formats a single conflict, showing the titles of the conflicting merge requests and a description of the conflict
- `formatConflicts(List<MergeRequestConflict> conflicts)`: Formats a list of conflicts by formatting each conflict and joining them with newlines
- `formatConflictNote(List<MergeRequestConflict> conflicts, Long mergeRequestIid, List<MergeRequest> resolvedConflictMrs)`: Formats a note with conflict information for a specific merge request, including information about resolved conflicts
- `appendInfoAboutResolvedConflicts(List<MergeRequest> resolvedConflictMrs, StringBuilder note)`: Appends information about resolved conflicts to the note
- `appendMergeRequestConflictInfo(Long mergeRequestIid, MergeRequestConflict conflict, StringBuilder note)`: Appends information about a specific merge request conflict to the note
- `formatConflictDescription(MergeRequestConflict conflict)`: Formats the description of a conflict
- `truncateTitle(String title)`: Truncates merge request titles to a reasonable length for display

#### Output Format Examples

##### Single Conflict Format
```
"Feature A: Add user authentication" vs "Feature B: Implement new UI components"
- Issue: conflict in modification of `src/app.js`
```

##### Multiple Conflicts Format
```
"Feature A: Add user authentication" vs "Feature B: Implement new UI components"
- Issue: conflict in modification of `src/app.js`
"Feature A: Add user authentication" vs "Feature C: Optimize database queries"
- Issue: conflict in modification of `tests/unit.test.js`
```

##### Conflict Note Format
```markdown
## Merge Request Conflict Analysis

This merge request has conflicts with the following merge requests:

### Conflict with MR !42 (Feature B: Implement new UI components)
- **Source branch:** feature-b
- **Target branch:** main
- **Conflict reason:** SAME_FILES
- **Conflicting files:** 1
  - `src/app.js`

Please resolve these conflicts before merging.
```

##### Resolved Conflicts Note Format
```markdown
## Merge Request Conflict Analysis

#### Resolved conflicts

- **Conflict with MR !42 (Feature B: Implement new UI components)** due to merge. Please check merge request to verify changes.

No more conflicts detected. All conflicts are resolved!
```

## Integration Points
- **Core Package**: Uses the formatter to format conflicts for output
- **Model Package**: Uses MergeRequestConflict and MergeRequestInfo models
- **GitLab Package**: Uses GitLab API models for merge requests
