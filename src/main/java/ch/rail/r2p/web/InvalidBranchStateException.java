package ch.rail.r2p.web;

/**
 * Thrown when a branch operation is attempted in a state that does not permit it
 * (e.g., requesting a merge on an already-requested branch, merging an already-merged branch).
 * Maps to HTTP 409 Conflict.
 */
public class InvalidBranchStateException extends RuntimeException {

    public InvalidBranchStateException(String message) {
        super(message);
    }
}

