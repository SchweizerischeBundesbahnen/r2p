package ch.rail.r2p.persistence.impl;

import ch.rail.r2p.api.dto.BranchState;
import ch.rail.r2p.domain.Branch;
import ch.rail.r2p.domain.TrainVersion;
import ch.rail.r2p.domain.TrainVersionEnriched;
import ch.rail.r2p.persistence.TrainPersistence;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class TrainPersistenceImpl implements TrainPersistence {

    private final TrainSpringDataRepository trainSpringDataRepository;
    private final BranchSpringDataRepository branchSpringDataRepository;
    private final TrainReadingConverter trainReadingConverter;

    @Override
    public void insertVersion(TrainVersion train) {
        trainSpringDataRepository.insert(train);
    }

    @Override
    public void updateVersion(TrainVersion train) {
        trainSpringDataRepository.save(train);
    }

    @Override
    public List<TrainVersionEnriched> findHeadVersionsByTrainId(UUID trainId) {
        return enrich(trainSpringDataRepository.findHeadVersionsByTrainId(trainId));
    }

    @Override
    public Optional<TrainVersionEnriched> findHeadVersionByTrainIdAndBranchName(UUID trainId, String branchName) {
        Optional<Branch> branch = branchSpringDataRepository.findTrainBranchByTrainIdAndName(trainId, branchName);
        return branch.flatMap(value -> trainSpringDataRepository
            .findHeadVersionByTrainIdAndBranchId(trainId, value.getBranchId())
            .map(version -> new TrainVersionEnriched(version, value, trainSpringDataRepository.getPartners(trainId))));
    }

    @Override
    public Optional<TrainVersionEnriched> findHeadVersionByTrainIdAndBranchId(UUID trainId, UUID branchId) {
        Optional<TrainVersion> version = trainSpringDataRepository.findHeadVersionByTrainIdAndBranchId(trainId, branchId);
        return version.map(this::enrich);
    }

    @Override
    public List<TrainVersionEnriched> findHeadVersions(String partner, LocalDate operationalDayFrom, LocalDate operationalDayTo) {
        List<TrainSpringDataRepository.VersionAndPayload> candidates =
            trainSpringDataRepository.findHeadVersionIdsAndPayloads(partner, operationalDayFrom, operationalDayTo);
        if (candidates.isEmpty()) {
            return List.of();
        }
        // Deserialize each payload individually so that one corrupt entry does not abort the entire list
        List<UUID> validIds = new java.util.ArrayList<>();
        for (TrainSpringDataRepository.VersionAndPayload candidate : candidates) {
            try {
                trainReadingConverter.convert(candidate.payload());
                validIds.add(candidate.version());
            } catch (PayloadDeserializationException e) {
                log.error("Skipping train version {} (trainId={}, branchId={}) due to payload deserialization error: {}",
                    candidate.version(), candidate.trainId(), candidate.branchId(), e.getMessage());
            }
        }
        if (validIds.isEmpty()) {
            return List.of();
        }
        // Fetch full rows for valid IDs in a single IN-query; converter will succeed because we pre-validated
        List<TrainVersion> versions = StreamSupport.stream(
            trainSpringDataRepository.findAllById(validIds).spliterator(), false).toList();
        return enrich(versions);
    }

    @Override
    public TrainVersionEnriched getVersion(UUID version) {
        return enrich(trainSpringDataRepository.findById(version)
            .orElseThrow(() -> new NoSuchElementException("Train version with id %s not found".formatted(version))));
    }


    @Override
    public void insertBranch(Branch branch) {
        branchSpringDataRepository.insert(branch);
    }

    @Override
    public Branch updateBranchState(UUID branchId, BranchState state) {
        Branch branch = branchSpringDataRepository.findById(branchId)
            .orElseThrow(() -> new NoSuchElementException("Branch with id %s not found".formatted(branchId)));
        branch.setState(state);
        return branchSpringDataRepository.save(branch);
    }

    @Override
    public boolean existsByTrainId(UUID trainId) {
        return trainSpringDataRepository.existsByTrainId(trainId);
    }


    @Override
    public List<Branch> findBranchesByTrainId(UUID trainId) {
        return branchSpringDataRepository.findTrainBranchByTrainId(trainId);
    }

    @Override
    public void addPartner(UUID trainId, String partner) {
        trainSpringDataRepository.addPartner(trainId, partner);
    }

    @Override
    public void removePartner(UUID trainId, String partner) {
        trainSpringDataRepository.removePartner(trainId, partner);
    }

    private List<TrainVersionEnriched> enrich(List<TrainVersion> versions) {
        if (versions.isEmpty()) {
            return List.of();
        }
        var branches = branchSpringDataRepository.findAllById(versions.stream().map(TrainVersion::getBranchId).toList());
        Map<UUID, Branch> branchById = StreamSupport.stream(branches.spliterator(), false).collect(Collectors.toMap(Branch::getBranchId, Function.identity()));
        var partners = trainSpringDataRepository.getPartnersByTrainIds(versions.stream().map(TrainVersion::getTrainId).collect(Collectors.toList()));
        Map<UUID, List<String>> partnersByTrainId = partners.stream()
            .collect(Collectors.groupingBy(TrainSpringDataRepository.TrainIdAndPartner::trainId, Collectors.mapping(TrainSpringDataRepository.TrainIdAndPartner::partner, Collectors.toList())));
        return versions.stream().map(version -> new TrainVersionEnriched(version, branchById.get(version.getBranchId()), partnersByTrainId.computeIfAbsent(version.getTrainId(), _ -> List.of())))
            .toList();
    }

    private TrainVersionEnriched enrich(TrainVersion version) {
        return enrich(List.of(version)).getFirst();
    }
}
