package art.galushko.gitlab.mrconflict.model;

/**
 * Status of a merge operation.
 */
public enum MergeStatus {
    /**
     * Merge can be completed without conflicts
     */
    CLEAN,
    
    /**
     * Merge has conflicts that need resolution
     */
    CONFLICTED,
    
    /**
     * Merge failed due to technical issues
     */
    FAILED,
    
    /**
     * Merge was aborted or cancelled
     */
    ABORTED,
    
    /**
     * Merge status is unknown or could not be determined
     */
    UNKNOWN
}

