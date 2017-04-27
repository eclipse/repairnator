package fr.inria.spirals.repairnator.states;

/**
 * Created by urli on 27/04/2017.
 */
public enum PushState {
    NONE,
    REPO_INITIALIZED, REPO_NOT_INITIALIZED,
    REPAIR_INFO_PUSHED, REPAIR_INFO_NOT_PUSHED,
    PATCH_PUSHED, PATCH_NOT_PUSHED,
    REPO_PUSHED, REPO_NOT_PUSHED
}
