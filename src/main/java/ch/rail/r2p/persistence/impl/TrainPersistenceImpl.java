package ch.rail.r2p.persistence.impl;

import ch.rail.r2p.domain.TrainVersion;
import ch.rail.r2p.persistence.TrainPersistence;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class TrainPersistenceImpl implements TrainPersistence {

    private final TrainSpringDataRepository trainSpringDataRepository;

    @Override
    public void insertVersion(TrainVersion train) {
        trainSpringDataRepository.insert(train);
    }

    @Override
    public void updateVersion(TrainVersion train) {
        trainSpringDataRepository.save(train);
    }

    @Override
    public List<TrainVersion> findHeadVersionsByTrainId(UUID trainId) {
        return trainSpringDataRepository.findHeadVersionsByTrainId(trainId);
    }

    @Override
    public Optional<TrainVersion> findHeadVersionByTrainIdAndBranchName(UUID trainId, String branchName) {
        return trainSpringDataRepository.findHeadVersionByTrainIdAndBranchName(trainId, branchName);
    }

    @Override
    public Optional<TrainVersion> findHeadVersionByTrainIdAndBranchId(UUID trainId, UUID branchId) {
        return trainSpringDataRepository.findHeadVersionByTrainIdAndBranchId(trainId, branchId);
    }

    @Override
    public List<TrainVersion> findHeadVersionsByCriteria(String partner, LocalDate operationalDayFrom, LocalDate operationalDayTo) {
        return trainSpringDataRepository.findHeadVersionsByCriteria(partner, operationalDayFrom, operationalDayTo);
    }

    @Override
    public TrainVersion getVersion(UUID version) {
        return trainSpringDataRepository.findById(version).orElseThrow();
    }

    @Override
    public void deleteTrains(Collection<UUID> trainIds) {
        trainIds.forEach(trainSpringDataRepository::deleteByTrainId);
    }

    @Override
    public void addPartner(UUID trainId, String partner) {
        trainSpringDataRepository.addPartner(trainId, partner);
    }

    @Override
    public void removePartner(UUID trainId, String partner) {
        trainSpringDataRepository.removePartner(trainId, partner);
    }

    @Override
    public List<String> getPartners(UUID trainId) {
        return trainSpringDataRepository.getPartners(trainId);
    }
}
