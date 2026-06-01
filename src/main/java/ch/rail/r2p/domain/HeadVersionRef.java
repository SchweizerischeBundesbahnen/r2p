package ch.rail.r2p.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Lightweight reference to the HEAD version of a branch.
 * Contains only the version UUID and timestamp – the payload column is intentionally not loaded.
 */
public record HeadVersionRef(UUID version, OffsetDateTime timestamp) {
}

