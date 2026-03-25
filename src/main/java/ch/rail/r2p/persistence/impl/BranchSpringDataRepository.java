package ch.rail.r2p.persistence.impl;

import ch.rail.r2p.domain.Branch;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchSpringDataRepository extends CrudRepository<Branch, UUID>, CustomInsertRepository<Branch> {

    List<Branch> findTrainBranchByTrainId(UUID trainId);

    Optional<Branch> findTrainBranchByTrainIdAndName(UUID trainId, String branchName);

    @Query("""
        DELETE FROM branch WHERE train_id=:trainId
        """)
    @Modifying
    void deleteBranchesByTrainId(UUID trainId);

    @Query("""
        SELECT owner, COUNT(*) AS cnt
        FROM branch
        WHERE state = 'OPEN'
        GROUP BY owner
        """)
    List<OwnerCount> countOpenBranchesByOwner();

    @Query("""
        SELECT owner, COUNT(*) AS cnt
        FROM branch
        WHERE state = 'OPEN'
          AND name != 'main'
        GROUP BY owner
        """)
    List<OwnerCount> countOpenNonMainBranchesByOwner();

    @Query("""
        SELECT owner, COUNT(*) AS cnt
        FROM branch
        WHERE state = 'MERGE_REQUESTED'
        GROUP BY owner
        """)
    List<OwnerCount> countMergeRequestedBranchesByOwner();

    record OwnerCount(String owner, int cnt) {
    }
}
