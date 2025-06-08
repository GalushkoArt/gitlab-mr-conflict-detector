package art.galushko.gitlab.mrconflict.model;

/**
 * Enumeration of reasons why merge requests might conflict.
 */
public enum ConflictReason {
    /**
     * Direct conflict: Both MRs target the same branch and modify the same files.
     */
    DIRECT_CONFLICT,

    /**
     * Cross-branch conflict: MRs target different branches but modify the same files
     * and the branches will eventually merge.
     */
    CROSS_BRANCH_CONFLICT
}

