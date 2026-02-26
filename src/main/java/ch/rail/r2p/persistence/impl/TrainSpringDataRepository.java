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
        WHERE train_id=:trainId AND head=true
        """)
    List<TrainVersion> findHeadVersionsByTrainId(UUID trainId);

    @Query("""
        SELECT * FROM train_version
        WHERE train_id=:trainId AND branch_id=:branchId AND head=true
        """)
    Optional<TrainVersion> findHeadVersionByTrainIdAndBranchId(UUID trainId, UUID branchId);

    @Query("""
        SELECT * FROM train_version
        WHERE (:operationalDayFrom IS NULL OR operational_day>=:operationalDayFrom)
          AND (:operationalDayTo IS NULL OR operational_day<=:operationalDayTo)
          AND EXISTS (SELECT 1 FROM train_partner WHERE train_partner.train_id=train_version.train_id AND train_partner.partner=:partner)
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
        DELETE FROM train_partner WHERE train_id=:trainId;
        DELETE FROM changelog WHERE train_id=:trainId;
        DELETE FROM train_version WHERE train_id=:trainId;
        DELETE FROM branch WHERE train_id=:trainId;
        """)
    @Modifying
    void deleteByTrainId(UUID trainId);

    record TrainIdAndPartner(UUID trainId, String partner) {
    }
}
