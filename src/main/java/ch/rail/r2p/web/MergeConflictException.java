package ch.rail.r2p.web;

/**
 * Thrown when the reviewed version does not match the current head version of a branch during a merge.
 */
public class MergeConflictException extends RuntimeException {

    public MergeConflictException(String message) {
        super(message);
    }
}

