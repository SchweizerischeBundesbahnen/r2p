package ch.rail.r2p.persistence;

import ch.rail.r2p.domain.TrainVersion;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainPersistence {

    void insertVersion(TrainVersion train);
    void updateVersion(TrainVersion train);
    List<TrainVersion> findHeadVersionsByTrainId(UUID trainId);
    Optional<TrainVersion> findHeadVersionByTrainIdAndBranchName(UUID trainId, String branchName);
    Optional<TrainVersion> findHeadVersionByTrainIdAndBranchId(UUID trainId, UUID branchId);
    List<TrainVersion> findHeadVersionsByCriteria(String partner, LocalDate operationalDayFrom, LocalDate operationalDayTo);
    TrainVersion getVersion(UUID version);
    void deleteTrains(Collection<UUID> trainIds);

    void addPartner(UUID trainId, String partner);
    void removePartner(UUID trainId, String partner);
    List<String> getPartners(UUID trainId);
}
