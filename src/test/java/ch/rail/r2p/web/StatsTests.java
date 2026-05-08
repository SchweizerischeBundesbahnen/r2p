package ch.rail.r2p.web;

import static ch.rail.r2p.api.dto.PartnerToken.BLS;
import static ch.rail.r2p.api.dto.PartnerToken.SBB;
import static ch.rail.r2p.api.dto.PartnerToken.SOB;
import static org.assertj.core.api.Assertions.assertThat;

import ch.rail.r2p.Actor;
import ch.rail.r2p.persistence.TrainPersistence;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class StatsTests {

    @Autowired
    private StatsController statsController;

    @Autowired
    private TrainController trainController;

    @Autowired
    private Actor actor;

    @MockitoSpyBean
    private TrainPersistence trainPersistence;

    @BeforeEach
    void setUp() {
        actor.setup();
    }

    @AfterEach
    void tearDown() {
        actor.cleanup();
    }

    @Test
    void should_return_totals_row_with_null_partner() {
        actor.createTrain(SBB, "test");

        List<PartnerStats> stats = statsController.getStats();

        PartnerStats totals = stats.stream()
            .filter(s -> s.partner() == null)
            .findFirst()
            .orElseThrow(() -> new AssertionError("No totals row found"));

        assertThat(totals.leadRU()).isFalse();
        assertThat(totals.trains()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void should_set_lead_ru_true_only_for_instance_owner() {
        actor.createTrain(SBB, "test");

        List<PartnerStats> stats = statsController.getStats();

        assertThat(stats.stream().filter(s -> s.partner() != null && s.leadRU()))
            .as("exactly one partner has leadRU=true")
            .hasSize(1);

        assertThat(stats.stream().filter(s -> s.partner() != null && s.leadRU()).findFirst())
            .get()
            .extracting(PartnerStats::partner)
            .isEqualTo(SBB.name());
    }

    @Test
    void should_count_trains_per_partner_after_sharing() {
        var train = actor.createTrain(SBB, "test");
        actor.shareTrain(SBB, train.getTrainId(), SOB, "sharing with SOB");

        List<PartnerStats> stats = statsController.getStats();

        PartnerStats sobStats = stats.stream()
            .filter(s -> SOB.name().equals(s.partner()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No stats row for SOB"));

        assertThat(sobStats.trains()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void should_count_open_branches_per_partner_after_branch_creation() {
        var train = actor.createTrain(SBB, "test");
        actor.shareTrain(SBB, train.getTrainId(), SOB, "sharing with SOB");
        actor.createBranch(SOB, train.getTrainId(), "myBranch", "test");

        List<PartnerStats> stats = statsController.getStats();

        PartnerStats sobStats = stats.stream()
            .filter(s -> SOB.name().equals(s.partner()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No stats row for SOB"));

        assertThat(sobStats.openBranches()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void should_include_open_branches_in_totals() {
        var train = actor.createTrain(SBB, "test");
        actor.shareTrain(SBB, train.getTrainId(), SOB, "sharing with SOB");
        actor.createBranch(SOB, train.getTrainId(), "myBranch", "test");

        List<PartnerStats> stats = statsController.getStats();

        PartnerStats totals = stats.stream()
            .filter(s -> s.partner() == null)
            .findFirst()
            .orElseThrow(() -> new AssertionError("No totals row found"));

        int sumOfPartnerOpenBranches = stats.stream()
            .filter(s -> s.partner() != null)
            .mapToInt(PartnerStats::openBranches)
            .sum();

        assertThat(totals.openBranches()).isEqualTo(sumOfPartnerOpenBranches);
    }

    @Test
    void should_not_include_deleted_branches_in_open_count() {
        var train = actor.createTrain(SBB, "test");
        actor.shareTrain(SBB, train.getTrainId(), BLS, "sharing with BLS");
        var branch = actor.createBranch(BLS, train.getTrainId(), "myBranch", "test");

        // Delete the branch
        trainController.deleteTrainBranch(BLS, train.getTrainId(), branch.getBranchId(), "cleanup");

        List<PartnerStats> stats = statsController.getStats();

        PartnerStats blsStats = stats.stream()
            .filter(s -> BLS.name().equals(s.partner()))
            .findFirst()
            .orElseThrow();
        assertThat(blsStats.openBranches()).isZero();
    }
}

