package art.galushko.gitlab.mrconflict.model;

/**
 * Types of merge conflicts that can be detected.
 */
public enum ConflictType {
    /**
     * Content conflict - both branches modified the same lines
     */
    CONTENT,
    
    /**
     * File deletion conflict - one branch deleted file, other modified it
     */
    DELETE_MODIFY,
    
    /**
     * File addition conflict - both branches added file with same name
     */
    ADD_ADD,
    
    /**
     * Rename conflict - both branches renamed file differently
     */
    RENAME_RENAME,
    
    /**
     * Mode conflict - file permissions/mode changed differently
     */
    MODE,
    
    /**
     * Binary file conflict
     */
    BINARY,
    
    /**
     * Submodule conflict
     */
    SUBMODULE,
    
    /**
     * Unknown or complex conflict type
     */
    UNKNOWN
}

