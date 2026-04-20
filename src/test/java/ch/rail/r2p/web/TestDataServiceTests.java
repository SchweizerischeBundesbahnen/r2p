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
        // Delete changelog first (including entries with NULL branch_id such as SHARED/UNSHARED
        // that are not cascade-deleted when branches are removed), then partners and branches.
        changelogSpringDataRepository.deleteAll();
        trainSpringDataRepository.deleteAllTrainPartners();
        branchSpringDataRepository.deleteAll(); // cascades to train_version via FK ON DELETE CASCADE
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
    void should_generate_and_persist_requested_number_of_trains() {
        var result = testDataController.generate(request(10, List.of())).getBody();

        assertThat(result).isNotNull();
        assertThat(result.trainsCreated()).isEqualTo(10);
        assertThat(trainSpringDataRepository.count()).isGreaterThanOrEqualTo(10);
    }

    @Test
    void result_counters_should_be_consistent_and_match_database() {
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

        // DB: open merge requests match the result counter
        long mergeRequestedInDb = branchSpringDataRepository.countMergeRequestedBranchesByOwner()
            .stream().mapToLong(BranchSpringDataRepository.OwnerCount::cnt).sum();
        assertThat(mergeRequestedInDb).isEqualTo(result.mergeRequestsCreated());

        // DB: branches are never physically deleted (state = DELETED), so the DB count is:
        // 1 main branch per train  +  all non-main branches created
        long totalBranchesInDb = branchSpringDataRepository.count();
        assertThat(totalBranchesInDb).isEqualTo((long) result.trainsCreated() + result.branchesCreated());
    }

    @Test
    void partner_sharing_should_respect_provided_partner_list() {
        // With partners: at least some trains must be shared with a non-owner partner
        testDataController.generate(request(10, List.of(SOB.name(), BLS.name())));
        long sharedCount = trainSpringDataRepository.countTrainsPerPartner().stream()
            .filter(p -> !p.partner().equals("SBB"))
            .mapToLong(TrainSpringDataRepository.PartnerCount::cnt)
            .sum();
        assertThat(sharedCount).isGreaterThan(0);

        cleanDatabase();

        // Without partners: only the instance owner (SBB) should appear
        testDataController.generate(request(10, List.of()));
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
