package ch.rail.r2p.persistence.impl;

import ch.rail.r2p.domain.Changelog;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChangelogSpringDataRepository extends CrudRepository<Changelog, UUID>, CustomInsertRepository<Changelog> {

    @Query("""
        SELECT * FROM changelog
        WHERE (:from IS NULL OR timestamp>=:from)
          AND (:to IS NULL OR timestamp<=:to)
          AND (:trainId IS NULL OR train_id=:trainId)
          AND (EXISTS (SELECT 1 FROM train_partner WHERE train_partner.train_id=changelog.train_id AND train_partner.partner=:partner)
              OR affected_partner=:partner
              )
          ORDER BY timestamp, id
        """)
    List<Changelog> find(String partner, OffsetDateTime from, OffsetDateTime to, UUID trainId);

    @Query("""
        SELECT c.* FROM (
            SELECT *, ROW_NUMBER() OVER (PARTITION BY branch_id ORDER BY timestamp DESC, id DESC) AS rn
            FROM changelog
            WHERE train_id = :trainId
              AND action IN ('MERGE_REQUESTED', 'MERGE_REJECTED', 'MERGED')
              AND branch_id IS NOT NULL
        ) c
        WHERE c.rn = 1
          AND EXISTS (SELECT 1 FROM train_partner WHERE train_partner.train_id = c.train_id AND train_partner.partner = :partner)
        ORDER BY c.timestamp DESC
        """)
    List<Changelog> findLatestMergeRelatedPerBranch(String partner, UUID trainId);

    @Query("""
        SELECT * FROM changelog WHERE train_id=:trainId
        """)
    List<Changelog> findByTrainId(UUID trainId);

    @Query("""
        DELETE FROM changelog WHERE train_id=:trainId
        """)
    @Modifying
    void deleteByTrainId(UUID trainId);
}
