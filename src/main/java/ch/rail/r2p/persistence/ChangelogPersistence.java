package ch.rail.r2p.persistence;

import ch.rail.r2p.domain.Changelog;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ChangelogPersistence {

    void insert(Changelog entry);

    List<Changelog> find(String partner, OffsetDateTime from, OffsetDateTime to, UUID trainId);

    /**
     * Returns the latest merge-related changelog entry (MERGE_REQUESTED, MERGE_REJECTED, MERGED)
     * per branch for the given train. Only branches with at least one such entry are included.
     */
    List<Changelog> findLatestMergeRelatedPerBranch(String partner, UUID trainId);

    void deleteByTrainId(Collection<UUID> trainId);
}
