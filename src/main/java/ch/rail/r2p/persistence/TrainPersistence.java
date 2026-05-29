package ch.rail.r2p.persistence;

import ch.rail.r2p.api.dto.BranchState;
import ch.rail.r2p.domain.Branch;
import ch.rail.r2p.domain.HeadVersionRef;
import ch.rail.r2p.domain.TrainVersionEnriched;
import ch.rail.r2p.domain.TrainVersion;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainPersistence {

    void insertVersion(TrainVersion train);

    void updateVersion(TrainVersion train);

    List<TrainVersionEnriched> findHeadVersionsByTrainId(UUID trainId);

    Optional<TrainVersionEnriched> findHeadVersionByTrainIdAndBranchName(UUID trainId, String branchName);

    Optional<TrainVersionEnriched> findHeadVersionByTrainIdAndBranchId(UUID trainId, UUID branchId);

    List<TrainVersionEnriched> findHeadVersions(String partner, LocalDate operationalDayFrom, LocalDate operationalDayTo);

    boolean existsByTrainId(UUID trainId);

    TrainVersionEnriched getVersion(UUID version);


    void insertBranch(Branch branch);

    Branch updateBranchState(UUID branchId, BranchState state);

    List<Branch> findBranchesByTrainId(UUID trainId);

    void addPartner(UUID trainId, String partner);

    void removePartner(UUID trainId, String partner);

    /**
     * Finds a branch by trainId and branchId without loading any train version payload.
     */
    Optional<Branch> findBranch(UUID trainId, UUID branchId);

    /**
     * Returns the list of partners for the given train without loading any train version payload.
     */
    List<String> getPartners(UUID trainId);

    /**
     * Returns a lightweight reference to the HEAD version of a branch (version UUID + timestamp only),
     * without deserializing the payload column.
     */
    Optional<HeadVersionRef> findHeadVersionRef(UUID trainId, UUID branchId);
}
