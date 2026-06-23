package ch.rail.r2p.web;

import static ch.rail.r2p.api.dto.PartnerToken.BLS;
import static ch.rail.r2p.api.dto.PartnerToken.SBB;
import static ch.rail.r2p.api.dto.PartnerToken.SOB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.rail.r2p.Actor;
import ch.rail.r2p.NoAuthSpringBootTest;
import ch.rail.r2p.api.dto.Activity;
import ch.rail.r2p.api.dto.Branch;
import ch.rail.r2p.api.dto.Journeylocation;
import ch.rail.r2p.api.dto.Location;
import ch.rail.r2p.api.dto.LocationType;
import ch.rail.r2p.api.dto.PlanStatus;
import ch.rail.r2p.api.dto.Train;
import ch.rail.r2p.api.dto.TrainListElement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authorization.AuthorizationDeniedException;

/**
 * Verifies the two demo authorization rules on the branch editing endpoints, using a train whose
 * activities are assigned via partner tokens ({@code responsibility.id} = SBB/BLS/SOB):
 * <ul>
 *   <li>SBB (Lead RU / instance owner) may edit everything.</li>
 *   <li>BLS may only edit its own staff activity, not the route nor SOB's path activity.</li>
 *   <li>SOB may only edit its own path activity, not the route nor BLS's staff activity.</li>
 * </ul>
 */
@NoAuthSpringBootTest
class BranchEditAuthorizationTests {

    @Autowired
    private TrainController trainController;

    @Autowired
    private Actor actor;

    private TrainListElement train;
    private UUID mainBranchId;

    @BeforeEach
    void setUp() {
        actor.setup();
        // At creation only the Lead RU is a partner, so the train starts owner-only.
        Train initial = actor.loadTrain("/editAuthorizationTrain.json");
        setAllResponsibilities(initial, "SBB");
        train = actor.createTrain(SBB, "edit-auth train", initial);
        mainBranchId = train.getBranches().getFirst().getBranchId();
        actor.shareTrain(SBB, train.getTrainId(), BLS, "share with BLS");
        actor.shareTrain(SBB, train.getTrainId(), SOB, "share with SOB");
        // The Lead RU now assigns the activities to the shared partners (BLS staff, SOB path).
        trainController.updateTrainBranch(SBB, train.getTrainId(), mainBranchId, "assign partners",
            actor.loadTrain("/editAuthorizationTrain.json"));
    }

    @AfterEach
    void tearDown() {
        actor.cleanup();
    }

    @Test
    void leadRu_mayEditRouteAndAnyActivity() {
        Train payload = actor.loadTrain("/editAuthorizationTrain.json");
        // change the route and an activity owned by BLS
        firstLocation(payload).setCode("CH-0000");
        findActivity(payload, "bls-staff").getResponsibility().setStatus(PlanStatus.REQUESTED);

        assertThatCode(() -> trainController.updateTrainBranch(SBB, train.getTrainId(), mainBranchId, "lead edit", payload))
            .doesNotThrowAnyException();
    }

    @Test
    void bls_mayEditOwnStaffActivity() {
        Branch branch = actor.createBranch(BLS, train.getTrainId(), "blsBranch", "branch",
            actor.loadTrain("/editAuthorizationTrain.json"));

        Train payload = actor.loadTrain("/editAuthorizationTrain.json");
        findActivity(payload, "bls-staff").getResponsibility().setStatus(PlanStatus.REQUESTED);

        assertThatCode(() -> trainController.updateTrainBranch(BLS, train.getTrainId(), branch.getBranchId(), "bls edit", payload))
            .doesNotThrowAnyException();
    }

    @Test
    void bls_mayNotEditSobActivity() {
        Branch branch = actor.createBranch(BLS, train.getTrainId(), "blsBranch", "branch",
            actor.loadTrain("/editAuthorizationTrain.json"));

        Train payload = actor.loadTrain("/editAuthorizationTrain.json");
        findActivity(payload, "sob-path").getResponsibility().setStatus(PlanStatus.REQUESTED);

        assertThatThrownBy(() -> trainController.updateTrainBranch(BLS, train.getTrainId(), branch.getBranchId(), "bls edit", payload))
            .isInstanceOf(AuthorizationDeniedException.class)
            .hasMessageContaining("sob-path")
            .hasMessageContaining("BLS");
    }

    @Test
    void bls_mayNotEditRoute() {
        Branch branch = actor.createBranch(BLS, train.getTrainId(), "blsBranch", "branch",
            actor.loadTrain("/editAuthorizationTrain.json"));

        Train payload = actor.loadTrain("/editAuthorizationTrain.json");
        firstLocation(payload).setCode("CH-0000");

        assertThatThrownBy(() -> trainController.updateTrainBranch(BLS, train.getTrainId(), branch.getBranchId(), "bls edit", payload))
            .isInstanceOf(AuthorizationDeniedException.class)
            .hasMessageContaining("route");
    }

    @Test
    void sob_mayEditOwnPathActivity() {
        Branch branch = actor.createBranch(SOB, train.getTrainId(), "sobBranch", "branch",
            actor.loadTrain("/editAuthorizationTrain.json"));

        Train payload = actor.loadTrain("/editAuthorizationTrain.json");
        findActivity(payload, "sob-path").getResponsibility().setStatus(PlanStatus.REQUESTED);

        assertThatCode(() -> trainController.updateTrainBranch(SOB, train.getTrainId(), branch.getBranchId(), "sob edit", payload))
            .doesNotThrowAnyException();
    }

    @Test
    void sob_mayNotEditBlsActivity() {
        Branch branch = actor.createBranch(SOB, train.getTrainId(), "sobBranch", "branch",
            actor.loadTrain("/editAuthorizationTrain.json"));

        Train payload = actor.loadTrain("/editAuthorizationTrain.json");
        findActivity(payload, "bls-staff").getResponsibility().setStatus(PlanStatus.REQUESTED);

        assertThatThrownBy(() -> trainController.updateTrainBranch(SOB, train.getTrainId(), branch.getBranchId(), "sob edit", payload))
            .isInstanceOf(AuthorizationDeniedException.class)
            .hasMessageContaining("bls-staff");
    }

    @Test
    void bls_mayNotIntroduceRouteChangeAtBranchCreation() {
        Train payload = actor.loadTrain("/editAuthorizationTrain.json");
        payload.getJourney().getJourneylocations().add(extraLocation());

        assertThatThrownBy(() -> trainController.createTrainBranch(BLS, train.getTrainId(), "blsBranch", "branch", payload))
            .isInstanceOf(AuthorizationDeniedException.class)
            .hasMessageContaining("route");
    }

    @Test
    void mayNotAssignActivityToPartnerNotSharedWith() {
        // DBC is not shared with this train; even the Lead RU may not assign an activity to it.
        Train payload = actor.loadTrain("/editAuthorizationTrain.json");
        findActivity(payload, "sob-path").getResponsibility().setId("DBC");

        assertThatThrownBy(() -> trainController.updateTrainBranch(SBB, train.getTrainId(), mainBranchId, "assign DBC", payload))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("sob-path")
            .hasMessageContaining("DBC");
    }

    @Test
    void mayNotCreateTrainWithActivityForAnotherPartner() {
        // At creation only the creator (SBB) is a partner, so an activity assigned to BLS must be rejected.
        Train payload = actor.loadTrain("/editAuthorizationTrain.json");
        setAllResponsibilities(payload, "SBB");
        findActivity(payload, "bls-staff").getResponsibility().setId(BLS.name());

        assertThatThrownBy(() -> trainController.createTrain(SBB, "bad train", payload))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("BLS");
    }

    // --- helpers ----------------------------------------------------------------------------

    private static Location firstLocation(Train train) {
        return train.getJourney().getJourneylocations().getFirst().getLocations().getFirst();
    }

    private static void setAllResponsibilities(Train train, String owner) {
        train.getJourney().getJourneylocations().forEach(jl ->
            jl.getActivities().forEach(a -> a.getResponsibility().setId(owner)));
    }

    private static Activity findActivity(Train train, String id) {
        return train.getJourney().getJourneylocations().stream()
            .flatMap(jl -> jl.getActivities().stream())
            .filter(a -> id.equals(a.getId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("activity not found: " + id));
    }

    private static Journeylocation extraLocation() {
        return new Journeylocation()
            .locations(new ArrayList<>(List.of(new Location().type(LocationType.PLC).code("CH-9999").name("Extra"))))
            .activities(new ArrayList<>());
    }
}
