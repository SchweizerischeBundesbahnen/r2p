package ch.rail.r2p.persistence.impl;

import ch.rail.r2p.domain.Branch;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchSpringDataRepository extends CrudRepository<Branch, UUID>, CustomInsertRepository<Branch> {

    List<Branch> findTrainBranchByTrainId(UUID trainId);

    Optional<Branch> findTrainBranchByTrainIdAndName(UUID trainId, String branchName);
}
