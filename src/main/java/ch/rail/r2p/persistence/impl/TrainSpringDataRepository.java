package ch.rail.r2p.persistence.impl;

import ch.rail.r2p.domain.TrainVersion;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainSpringDataRepository extends CrudRepository<TrainVersion, UUID>, CustomInsertRepository<TrainVersion> {

    @Query("""
        SELECT * FROM train_version
        WHERE train_id = :trainId
          AND head = true
          AND NOT EXISTS (
              SELECT 1 FROM branch b
              WHERE b.train_id = :trainId
                AND b.name = 'main'
                AND b.state = 'DELETED'
          )
        """)
    List<TrainVersion> findHeadVersionsByTrainId(UUID trainId);

    @Query("""
        SELECT * FROM train_version
        WHERE train_id=:trainId AND branch_id=:branchId AND head=true
        """)
    Optional<TrainVersion> findHeadVersionByTrainIdAndBranchId(UUID trainId, UUID branchId);

    @Query("""
        SELECT * FROM train_version tv
        WHERE (:operationalDayFrom IS NULL OR tv.operational_day>=:operationalDayFrom)
          AND (:operationalDayTo IS NULL OR tv.operational_day<=:operationalDayTo)
          AND EXISTS (SELECT 1 FROM train_partner WHERE train_partner.train_id=tv.train_id AND train_partner.partner=:partner)
          AND tv.head = true
          AND NOT EXISTS (
              SELECT 1 FROM branch b
              WHERE b.train_id = tv.train_id
                AND b.name = 'main'
                AND b.state = 'DELETED'
          )
        """)
    List<TrainVersion> findHeadVersions(String partner, LocalDate operationalDayFrom, LocalDate operationalDayTo);

    @Query("""
        INSERT INTO train_partner(train_id, partner) VALUES(:trainId, :partner);
        """)
    @Modifying
    void addPartner(UUID trainId, String partner);

    @Query("""
        DELETE FROM train_partner WHERE train_id=:trainId and partner=:partner;
        """)
    @Modifying
    void removePartner(UUID trainId, String partner);

    @Query("""
        SELECT partner FROM train_partner WHERE train_id=:trainId
        """)
    List<String> getPartners(UUID trainId);

    @Query("""
        SELECT train_id, partner FROM train_partner WHERE train_id IN (:trainIds)
        """)
    List<TrainIdAndPartner> getPartnersByTrainIds(List<UUID> trainIds);

    @Query("""
        SELECT EXISTS (
            SELECT 1 FROM train_version WHERE train_id=:trainId
        )
        """)
    boolean existsByTrainId(UUID trainId);

    @Query("""
        DELETE FROM train_partner WHERE train_id=:trainId
        """)
    @Modifying
    void deleteTrainPartnersByTrainId(UUID trainId);

    @Query("""
        DELETE FROM train_partner
        """)
    @Modifying
    void deleteAllTrainPartners();

    @Query("""
        SELECT tp.partner, COUNT(DISTINCT tp.train_id) AS cnt
        FROM train_partner tp
        WHERE NOT EXISTS (
            SELECT 1 FROM branch b
            WHERE b.train_id = tp.train_id
              AND b.name = 'main'
              AND b.state = 'DELETED'
        )
        GROUP BY tp.partner
        """)
    List<PartnerCount> countTrainsPerPartner();

    record TrainIdAndPartner(UUID trainId, String partner) {
    }

    record PartnerCount(String partner, int cnt) {
    }
}
