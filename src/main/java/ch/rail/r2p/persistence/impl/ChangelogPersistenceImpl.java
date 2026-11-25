package ch.rail.r2p.persistence.impl;

import ch.rail.r2p.domain.Changelog;
import ch.rail.r2p.persistence.ChangelogPersistence;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ChangelogPersistenceImpl implements ChangelogPersistence {

    private final ChangelogSpringDataRepository changelogSpringDataRepository;

    @Override
    public void insert(Changelog entry) {
        changelogSpringDataRepository.insert(entry);
    }

    @Override
    public List<Changelog> find(String partner, OffsetDateTime from, OffsetDateTime to, UUID trainId) {
        return changelogSpringDataRepository.find(partner, from, to, trainId);
    }

    @Override
    public List<Changelog> findLatestMergeRelatedPerBranch(String partner, UUID trainId) {
        return changelogSpringDataRepository.findLatestMergeRelatedPerBranch(partner, trainId);
    }

    @Override
    public void deleteByTrainId(Collection<UUID> trainIds) {
        trainIds.forEach(changelogSpringDataRepository::deleteByTrainId);
    }
}
