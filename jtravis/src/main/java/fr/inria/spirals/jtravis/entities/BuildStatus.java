package fr.inria.spirals.jtravis.entities;

/**
 * Business object to deal with status of the build or of the job in Travis CI API
 *
 * @author Simon Urli
 */
public enum BuildStatus {
    FAILED, PASSED, CREATED, STARTED, ERRORED, CANCELED
}
