package ch.rail.r2p.web;

/**
 * Thrown when attempting to create a branch with a name that already exists for the same train.
 * Maps to HTTP 409 Conflict.
 */
public class DuplicateBranchNameException extends RuntimeException {

    public DuplicateBranchNameException(String branchName) {
        super("A branch with name '" + branchName + "' already exists for this train");
    }
}

