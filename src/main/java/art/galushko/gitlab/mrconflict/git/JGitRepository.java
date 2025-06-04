package art.galushko.gitlab.mrconflict.git;

import art.galushko.gitlab.mrconflict.model.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory;
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import com.jcraft.jsch.Session;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JGit-based implementation of GitRepository interface with authentication support.
 */
@Slf4j
public class JGitRepository implements GitRepository {
    private Repository repository;
    private Git git;
    private CredentialsProvider credentialsProvider;

    /**
     * Sets up authentication for Git operations.
     * Supports GitLab personal access tokens and SSH keys.
     */
    public JGitRepository(String gitlabToken) {
        if (gitlabToken != null && !gitlabToken.isEmpty()) {
            // Use token-based authentication
            // For GitLab, username can be anything when using personal access token
            this.credentialsProvider = new UsernamePasswordCredentialsProvider("gitlab-ci-token", gitlabToken);
            log.info("Using GitLab token authentication");
        } else {
            // Try to use SSH key authentication
            setupSshAuthentication();
            log.info("Using SSH key authentication (if available)");
        }
    }

    /**
     * Sets up SSH authentication for Git operations.
     */
    private void setupSshAuthentication() {
        SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
            @SneakyThrows
            @Override
            protected void configure(OpenSshConfig.Host host, Session session) {
                // Accept all host keys (for CI environments)
                session.setConfig("StrictHostKeyChecking", "no");

                // Set connection timeout
                session.setTimeout(30000); // 30 seconds
            }
        };
        SshSessionFactory.setInstance(sshSessionFactory);
    }

    @Override
    public boolean isValidRepository(Path repositoryPath) {
        try {
            var repo = new FileRepositoryBuilder()
                    .setGitDir(repositoryPath.resolve(".git").toFile())
                    .readEnvironment()
                    .findGitDir()
                    .build();
            repo.close();
            return true;
        } catch (IOException e) {
            log.debug("Path {} is not a valid Git repository: {}", repositoryPath, e.getMessage());
            return false;
        }
    }

    @Override
    public void openRepository(Path repositoryPath) throws GitOperationException {
        try {
            this.repository = new FileRepositoryBuilder()
                    .setGitDir(repositoryPath.resolve(".git").toFile())
                    .readEnvironment()
                    .findGitDir()
                    .build();
            this.git = new Git(repository);
            log.info("Opened Git repository at {}", repositoryPath);
        } catch (IOException e) {
            throw new GitOperationException("Failed to open repository at " + repositoryPath, e);
        }
    }

    @Override
    public String getCurrentBranch() throws GitOperationException {
        try {
            return repository.getBranch();
        } catch (IOException e) {
            throw new GitOperationException("Failed to get current branch", e);
        }
    }

    @Override
    public List<String> getLocalBranches() throws GitOperationException {
        try {
            return git.branchList()
                    .call()
                    .stream()
                    .map(ref -> Repository.shortenRefName(ref.getName()))
                    .collect(Collectors.toList());
        } catch (GitAPIException e) {
            throw new GitOperationException("Failed to get local branches", e);
        }
    }

    @Override
    public List<String> getRemoteBranches() throws GitOperationException {
        try {
            return git.branchList()
                    .setListMode(org.eclipse.jgit.api.ListBranchCommand.ListMode.REMOTE)
                    .call()
                    .stream()
                    .map(ref -> Repository.shortenRefName(ref.getName()))
                    .collect(Collectors.toList());
        } catch (GitAPIException e) {
            throw new GitOperationException("Failed to get remote branches", e);
        }
    }

    @Override
    public boolean branchExists(String branchName) throws GitOperationException {
        try {
            Ref ref = repository.findRef(branchName);
            return ref != null;
        } catch (IOException e) {
            throw new GitOperationException("Failed to check if branch exists: " + branchName, e);
        }
    }

    @Override
    public String getCommitHash(String ref) throws GitOperationException {
        try {
            ObjectId objectId = repository.resolve(ref);
            if (objectId == null) {
                throw new GitOperationException("Reference not found: " + ref);
            }
            return objectId.getName();
        } catch (IOException e) {
            throw new GitOperationException("Failed to resolve reference: " + ref, e);
        }
    }

    @Override
    public void fetch() throws GitOperationException {
        try {
            var fetchCommand = git.fetch();

            // Set credentials provider if available
            if (credentialsProvider != null) {
                fetchCommand.setCredentialsProvider(credentialsProvider);
            }

            fetchCommand.call();
            log.info("Fetched latest changes from remote");
        } catch (GitAPIException e) {
            throw new GitOperationException("Failed to fetch from remote", e);
        }
    }

    @Override
    public MergeResult detectConflicts(String sourceBranch, String targetBranch) throws GitOperationException {
        try {
            var sourceCommit = getCommitHash(sourceBranch);
            var targetCommit = getCommitHash(targetBranch);

            Optional<String> mergeBase = findMergeBase(sourceCommit, targetCommit);
            if (mergeBase.isEmpty()) {
                return new MergeResult(sourceBranch, targetBranch, sourceCommit, targetCommit,
                        Collections.emptyList(), MergeStatus.FAILED,
                        "No common ancestor found");
            }

            return analyzeThreeWayMerge(sourceCommit, targetCommit, mergeBase.get());
        } catch (Exception e) {
            throw new GitOperationException("Failed to detect conflicts between " + sourceBranch +
                    " and " + targetBranch, e);
        }
    }

    @Override
    public MergeResult analyzeThreeWayMerge(String sourceCommit, String targetCommit, String baseCommit)
            throws GitOperationException {
        try (RevWalk revWalk = new RevWalk(repository)) {
            var sourceId = ObjectId.fromString(sourceCommit);
            var targetId = ObjectId.fromString(targetCommit);
            var baseId = ObjectId.fromString(baseCommit);

            var sourceRev = revWalk.parseCommit(sourceId);
            var targetRev = revWalk.parseCommit(targetId);
            var baseRev = revWalk.parseCommit(baseId);

            // Use recursive merger for complex merge scenarios
            var merger = new RecursiveMerger(repository);
            merger.setBase(baseRev);

            boolean canMerge = merger.merge(targetRev, sourceRev);

            List<ConflictInfo> conflicts = new ArrayList<>();
            MergeStatus status;

            if (!canMerge) {
                // Extract conflict information
                conflicts = extractConflictInfo(merger, sourceCommit, targetCommit);
                status = MergeStatus.CONFLICTED;
            } else {
                status = MergeStatus.CLEAN;
            }

            String sourceBranch = findBranchForCommit(sourceCommit);
            String targetBranch = findBranchForCommit(targetCommit);

            return new MergeResult(sourceBranch, targetBranch, sourceCommit, targetCommit,
                    conflicts, status, conflicts.isEmpty() ? "No conflicts detected" :
                    conflicts.size() + " conflicts detected");

        } catch (IOException e) {
            throw new GitOperationException("Failed to analyze three-way merge", e);
        }
    }

    private List<ConflictInfo> extractConflictInfo(RecursiveMerger merger, String sourceCommit, String targetCommit) {
        List<ConflictInfo> conflicts = new ArrayList<>();

        try {
            var mergeResults = merger.getMergeResults();

            for (var entry : mergeResults.entrySet()) {
                String filePath = entry.getKey();
                org.eclipse.jgit.merge.MergeResult<?> mergeResult = entry.getValue();

                if (mergeResult.containsConflicts()) {
                    ConflictType conflictType = determineConflictType(mergeResult);
                    var sections = extractConflictSections(mergeResult);

                    var conflictInfo = new ConflictInfo(filePath, conflictType, sections, sourceCommit, targetCommit);
                    conflicts.add(conflictInfo);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract detailed conflict information", e);
        }

        return conflicts;
    }

    private ConflictType determineConflictType(org.eclipse.jgit.merge.MergeResult<?> mergeResult) {
        // Analyze the merge result to determine conflict type
        if (mergeResult.getSequences().size() == 3) {
            return ConflictType.CONTENT;
        }
        return ConflictType.UNKNOWN;
    }

    private List<ConflictSection> extractConflictSections(org.eclipse.jgit.merge.MergeResult<?> mergeResult) {
        List<ConflictSection> sections = new ArrayList<>();

        try {
            // Extract conflict sections from merge result
            // This is a simplified implementation - in practice, you'd need more sophisticated parsing
            var section = new ConflictSection(1, 1,
                    "Source content", "Target content", "Base content");
            sections.add(section);
        } catch (Exception e) {
            log.warn("Failed to extract conflict sections", e);
        }

        return sections;
    }

    private String findBranchForCommit(String commitHash) {
        try {
            // Try to find a branch that points to this commit
            var branches = getLocalBranches();
            for (var branch : branches) {
                if (getCommitHash(branch).equals(commitHash)) {
                    return branch;
                }
            }
            return commitHash; // Return commit hash if no branch found
        } catch (Exception e) {
            return commitHash;
        }
    }

    @Override
    public Optional<String> findMergeBase(String commit1, String commit2) throws GitOperationException {
        try (var revWalk = new RevWalk(repository)) {
            var id1 = ObjectId.fromString(commit1);
            var id2 = ObjectId.fromString(commit2);

            var rev1 = revWalk.parseCommit(id1);
            var rev2 = revWalk.parseCommit(id2);

            revWalk.setRevFilter(org.eclipse.jgit.revwalk.filter.RevFilter.MERGE_BASE);
            revWalk.markStart(rev1);
            revWalk.markStart(rev2);

            var mergeBase = revWalk.next();
            return mergeBase != null ? Optional.of(mergeBase.getName()) : Optional.empty();
        } catch (IOException e) {
            throw new GitOperationException("Failed to find merge base", e);
        }
    }

    @Override
    public List<String> getAffectedFiles(String sourceBranch, String targetBranch) throws GitOperationException {
        try {
            var sourceCommit = getCommitHash(sourceBranch);
            var targetCommit = getCommitHash(targetBranch);

            var mergeBase = findMergeBase(sourceCommit, targetCommit);
            if (mergeBase.isEmpty()) {
                return Collections.emptyList();
            }

            List<String> affectedFiles = new ArrayList<>();

            // Get files changed between base and source
            affectedFiles.addAll(getChangedFiles(mergeBase.get(), sourceCommit));

            // Get files changed between base and target
            affectedFiles.addAll(getChangedFiles(mergeBase.get(), targetCommit));

            return affectedFiles.stream().distinct().collect(Collectors.toList());
        } catch (Exception e) {
            throw new GitOperationException("Failed to get affected files", e);
        }
    }

    private List<String> getChangedFiles(String fromCommit, String toCommit) throws GitOperationException {
        try (var diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            diffFormatter.setRepository(repository);

            var fromId = ObjectId.fromString(fromCommit);
            var toId = ObjectId.fromString(toCommit);

            var diffs = diffFormatter.scan(fromId, toId);

            return diffs.stream()
                    .map(diff -> diff.getNewPath().equals("/dev/null") ? diff.getOldPath() : diff.getNewPath())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new GitOperationException("Failed to get changed files", e);
        }
    }

    @Override
    public boolean hasUncommittedChanges() throws GitOperationException {
        try {
            return !git.status().call().isClean();
        } catch (GitAPIException e) {
            throw new GitOperationException("Failed to check for uncommitted changes", e);
        }
    }

    @Override
    public String createTemporaryBranch(String baseBranch) throws GitOperationException {
        try {
            var tempBranchName = "temp-conflict-detection-" + System.currentTimeMillis();
            git.branchCreate()
                    .setName(tempBranchName)
                    .setStartPoint(baseBranch)
                    .call();
            log.debug("Created temporary branch: {}", tempBranchName);
            return tempBranchName;
        } catch (GitAPIException e) {
            throw new GitOperationException("Failed to create temporary branch", e);
        }
    }

    @Override
    public void deleteTemporaryBranch(String branchName) throws GitOperationException {
        try {
            git.branchDelete()
                    .setBranchNames(branchName)
                    .setForce(true)
                    .call();
            log.debug("Deleted temporary branch: {}", branchName);
        } catch (GitAPIException e) {
            throw new GitOperationException("Failed to delete temporary branch: " + branchName, e);
        }
    }

    @Override
    public void close() {
        if (git != null) {
            git.close();
        }
        if (repository != null) {
            repository.close();
        }
        log.debug("Closed Git repository");
    }
}

