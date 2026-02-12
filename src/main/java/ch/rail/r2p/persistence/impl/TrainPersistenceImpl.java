package ch.rail.r2p.persistence.impl;

import ch.rail.r2p.api.dto.BranchState;
import ch.rail.r2p.domain.Branch;
import ch.rail.r2p.domain.TrainVersion;
import ch.rail.r2p.domain.TrainVersionEnriched;
import ch.rail.r2p.persistence.TrainPersistence;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class TrainPersistenceImpl implements TrainPersistence {

    private final TrainSpringDataRepository trainSpringDataRepository;
    private final BranchSpringDataRepository branchSpringDataRepository;

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
        if (branch.isEmpty()) {
            return Optional.empty();
        }
        TrainVersion version = trainSpringDataRepository.findHeadVersionByTrainIdAndBranchId(trainId, branch.get().getBranchId()).orElseThrow();
        return Optional.of(new TrainVersionEnriched(version, branch.get(), trainSpringDataRepository.getPartners(trainId)));
    }

    @Override
    public Optional<TrainVersionEnriched> findHeadVersionByTrainIdAndBranchId(UUID trainId, UUID branchId) {
        Optional<TrainVersion> version = trainSpringDataRepository.findHeadVersionByTrainIdAndBranchId(trainId, branchId);
        return version.map(this::enrich);
    }

    @Override
    public List<TrainVersionEnriched> findHeadVersions(String partner, LocalDate operationalDayFrom, LocalDate operationalDayTo) {
        return enrich(trainSpringDataRepository.findHeadVersions(partner, operationalDayFrom, operationalDayTo));
    }

    @Override
    public TrainVersionEnriched getVersion(UUID version) {
        return enrich(trainSpringDataRepository.findById(version).orElseThrow());
    }

    @Override
    public void deleteTrains(Collection<UUID> trainIds) {
        trainIds.forEach(trainSpringDataRepository::deleteByTrainId);
    }

    @Override
    public void insertBranch(Branch branch) {
        branchSpringDataRepository.insert(branch);
    }

    @Override
    public Branch updateBranchState(UUID branchId, BranchState state) {
        Branch branch = branchSpringDataRepository.findById(branchId).orElseThrow();
        branch.setState(state);
        return branchSpringDataRepository.save(branch);
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
