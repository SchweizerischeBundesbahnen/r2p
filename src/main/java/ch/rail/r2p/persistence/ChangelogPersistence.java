package ch.rail.r2p.persistence;

import ch.rail.r2p.domain.Changelog;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ChangelogPersistence {

    void insert(Changelog entry);

    List<Changelog> find(String partner, OffsetDateTime from, OffsetDateTime to, UUID trainId);

    void deleteByTrainId(Collection<UUID> trainId);
}
