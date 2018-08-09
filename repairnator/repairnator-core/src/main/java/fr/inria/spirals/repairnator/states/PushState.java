package fr.inria.spirals.repairnator.states;

/**
 * This enum is used to define if information has been pushed during execution of the pipeline
 */
public enum PushState {
    NONE,
    REPO_INITIALIZED, REPO_NOT_INITIALIZED,
    REPAIR_INFO_COMMITTED, REPAIR_INFO_NOT_COMMITTED,
    PATCH_COMMITTED, PATCH_NOT_COMMITTED,
    PROCESS_END_COMMITTED, PROCESS_END_NOT_COMMITTED,
    CHANGED_TESTS_COMMITTED, CHANGED_TESTS_NOT_COMMITTED,
    REPO_PUSHED, REPO_NOT_PUSHED
}
