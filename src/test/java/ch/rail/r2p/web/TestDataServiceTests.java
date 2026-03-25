package ch.rail.r2p.web;

import static ch.rail.r2p.api.dto.PartnerToken.BLS;
import static ch.rail.r2p.api.dto.PartnerToken.SOB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.rail.r2p.persistence.impl.BranchSpringDataRepository;
import ch.rail.r2p.persistence.impl.ChangelogSpringDataRepository;
import ch.rail.r2p.persistence.impl.TrainSpringDataRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "r2p.test-data.enabled=true")
class TestDataServiceTests {

    @Autowired
    private TestDataController testDataController;

    @Autowired
    private TrainSpringDataRepository trainSpringDataRepository;

    @Autowired
    private BranchSpringDataRepository branchSpringDataRepository;

    @Autowired
    private ChangelogSpringDataRepository changelogSpringDataRepository;

    private void cleanDatabase() {
        // Delete all train_partner rows first (no cascade), then all other data.
        // We must query all distinct train_ids from train_partner directly, because
        // orphaned partner rows can exist after other test classes clean up via cascade delete.
        trainSpringDataRepository.findAll()
            .forEach(tv -> trainSpringDataRepository.deleteTrainPartnersByTrainId(tv.getTrainId()));
        // Also purge any orphaned train_partner rows whose train_version was removed by cascade
        // from other test classes (they would otherwise skew countTrainsPerPartner queries).
        trainSpringDataRepository.deleteAllTrainPartners();
        changelogSpringDataRepository.deleteAll();
        branchSpringDataRepository.deleteAll();  // cascades to train_version via FK
        trainSpringDataRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private TestDataRequest request(int numberOfTrains, List<String> partners) {
        return new TestDataRequest(
            numberOfTrains,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 12, 31),
            partners
        );
    }

    @Test
    void should_generate_requested_number_of_trains() {
        var result = testDataController.generate(request(10, List.of())).getBody();

        assertThat(result).isNotNull();
        assertThat(result.trainsCreated()).isEqualTo(10);
    }

    @Test
    void should_persist_generated_trains_in_database() {
        testDataController.generate(request(5, List.of()));

        assertThat(trainSpringDataRepository.count()).isGreaterThanOrEqualTo(5);
    }

    @Test
    void result_counters_should_be_consistent() {
        var result = testDataController.generate(request(20, List.of(SOB.name(), BLS.name()))).getBody();

        assertThat(result).isNotNull();
        assertThat(result.trainsCreated()).isEqualTo(20);
        // branches created must be >= branches deleted (can't delete more than were created)
        assertThat(result.branchesCreated()).isGreaterThanOrEqualTo(result.branchesDeleted());
        // main branch deletions are a subset of all branch deletions
        assertThat(result.branchesDeleted()).isGreaterThanOrEqualTo(result.mainBranchesDeleted());
        // versions >= trains (every train has at least 1 version) + branches created
        assertThat(result.versionsCreated()).isGreaterThanOrEqualTo(result.trainsCreated());
        // merge requests can only exist on non-main branches that were created and not deleted.
        // Main branch deletions increment branchesDeleted but have no effect on the
        // non-main branch pool, so they must be excluded from the upper bound.
        int nonMainBranchesDeleted = result.branchesDeleted() - result.mainBranchesDeleted();
        assertThat(result.mergeRequestsCreated()).isGreaterThanOrEqualTo(0);
        assertThat(result.mergeRequestsCreated()).isLessThanOrEqualTo(result.branchesCreated() - nonMainBranchesDeleted);
    }

    @Test
    void db_open_merge_requests_should_match_result_counter() {
        var result = testDataController.generate(request(30, List.of(SOB.name(), BLS.name()))).getBody();

        assertThat(result).isNotNull();
        long mergeRequestedInDb = branchSpringDataRepository.countMergeRequestedBranchesByOwner()
            .stream().mapToLong(BranchSpringDataRepository.OwnerCount::cnt).sum();
        assertThat(mergeRequestedInDb).isEqualTo(result.mergeRequestsCreated());
    }

    @Test
    void db_branch_count_should_include_all_states() {
        var result = testDataController.generate(request(20, List.of(SOB.name()))).getBody();

        assertThat(result).isNotNull();
        // Branches are never physically deleted (state = DELETED), so the DB count is:
        // 1 main branch per train  +  all non-main branches created
        long totalBranchesInDb = branchSpringDataRepository.count();
        assertThat(totalBranchesInDb).isEqualTo((long) result.trainsCreated() + result.branchesCreated());
    }

    @Test
    void should_share_with_at_least_one_partner_when_partners_provided() {
        testDataController.generate(request(10, List.of(SOB.name(), BLS.name())));

        // At least some trains should be shared with a non-owner partner
        long sharedCount = trainSpringDataRepository.countTrainsPerPartner().stream()
            .filter(p -> !p.partner().equals("SBB"))
            .mapToLong(TrainSpringDataRepository.PartnerCount::cnt)
            .sum();
        assertThat(sharedCount).isGreaterThan(0);
    }

    @Test
    void should_not_share_when_no_partners_provided() {
        testDataController.generate(request(10, List.of()));

        // Only the instance owner (SBB) should appear as partner
        var partners = trainSpringDataRepository.countTrainsPerPartner();
        assertThat(partners).allMatch(p -> p.partner().equals("SBB"));
    }

    @Test
    void generated_trains_should_have_operational_day_within_requested_range() {
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to   = LocalDate.of(2026, 3, 31);
        testDataController.generate(new TestDataRequest(20, from, to, List.of()));

        trainSpringDataRepository.findAll().forEach(tv ->
            assertThat(tv.getOperationalDay())
                .isAfterOrEqualTo(from)
                .isBeforeOrEqualTo(to));
    }

    @Test
    void should_reject_invalid_date_range() {
        var badRequest = new TestDataRequest(
            5,
            LocalDate.of(2026, 12, 31),
            LocalDate.of(2026, 1, 1),   // dateTo before dateFrom
            List.of()
        );

        assertThrows(IllegalArgumentException.class,
            () -> testDataController.generate(badRequest));
    }

    @Test
    void should_write_changelog_entries_for_every_train() {
        testDataController.generate(request(10, List.of(SOB.name())));

        // Every train must have at least a CREATED entry
        assertThat(changelogSpringDataRepository.count()).isGreaterThanOrEqualTo(10);
    }
}