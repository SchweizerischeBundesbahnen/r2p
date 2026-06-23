package ch.rail.r2p.web;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.rail.r2p.api.dto.Activity;
import ch.rail.r2p.api.dto.Journey;
import ch.rail.r2p.api.dto.Journeylocation;
import ch.rail.r2p.api.dto.Location;
import ch.rail.r2p.api.dto.LocationType;
import ch.rail.r2p.api.dto.PlanStatus;
import ch.rail.r2p.api.dto.Responsibility;
import ch.rail.r2p.api.dto.ResponsibilityType;
import ch.rail.r2p.api.dto.Train;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authorization.AuthorizationDeniedException;

class TrainEditPolicyTest {

    private static final String LEAD_RU = "SBB";
    private static final String BLS = "BLS";
    private static final String SOB = "SOB";

    private final TrainEditPolicy policy = new TrainEditPolicy(LEAD_RU);

    @Test
    void leadRu_mayChangeRouteAndAnyActivity() {
        Train oldTrain = train(location("CH-3527", activity("a1", BLS), activity("a2", SOB)));
        // Lead RU adds a location, removes/changes activities owned by other partners
        Train newTrain = train(
            location("CH-3527", activity("a1", SOB), modify(activity("a2", SOB))),
            location("CH-309", activity("a3", BLS)));

        assertThatCode(() -> policy.checkEditAuthorization(LEAD_RU, oldTrain, newTrain))
            .doesNotThrowAnyException();
    }

    @Test
    void partner_mayEditOwnActivity() {
        Train oldTrain = train(location("CH-3527", activity("a1", BLS), activity("a2", SOB)));
        Train newTrain = train(location("CH-3527", modify(activity("a1", BLS)), activity("a2", SOB)));

        assertThatCode(() -> policy.checkEditAuthorization(BLS, oldTrain, newTrain))
            .doesNotThrowAnyException();
    }

    @Test
    void partner_mayAddOwnActivity() {
        Train oldTrain = train(location("CH-3527", activity("a1", BLS)));
        Train newTrain = train(location("CH-3527", activity("a1", BLS), activity("a-new", BLS)));

        assertThatCode(() -> policy.checkEditAuthorization(BLS, oldTrain, newTrain))
            .doesNotThrowAnyException();
    }

    @Test
    void partner_mayRemoveOwnActivity() {
        Train oldTrain = train(location("CH-3527", activity("a1", BLS), activity("a2", BLS)));
        Train newTrain = train(location("CH-3527", activity("a1", BLS)));

        assertThatCode(() -> policy.checkEditAuthorization(BLS, oldTrain, newTrain))
            .doesNotThrowAnyException();
    }

    @Test
    void partner_mayNotEditForeignActivity() {
        Train oldTrain = train(location("CH-3527", activity("a1", BLS), activity("a2", SOB)));
        Train newTrain = train(location("CH-3527", activity("a1", BLS), modify(activity("a2", SOB))));

        assertThatThrownBy(() -> policy.checkEditAuthorization(BLS, oldTrain, newTrain))
            .isInstanceOf(AuthorizationDeniedException.class)
            .hasMessageContaining("a2")
            .hasMessageContaining(BLS);
    }

    @Test
    void partner_mayNotAddForeignActivity() {
        Train oldTrain = train(location("CH-3527", activity("a1", BLS)));
        Train newTrain = train(location("CH-3527", activity("a1", BLS), activity("a-foreign", SOB)));

        assertThatThrownBy(() -> policy.checkEditAuthorization(BLS, oldTrain, newTrain))
            .isInstanceOf(AuthorizationDeniedException.class)
            .hasMessageContaining("a-foreign");
    }

    @Test
    void partner_mayNotRemoveForeignActivity() {
        Train oldTrain = train(location("CH-3527", activity("a1", BLS), activity("a2", SOB)));
        Train newTrain = train(location("CH-3527", activity("a1", BLS)));

        assertThatThrownBy(() -> policy.checkEditAuthorization(BLS, oldTrain, newTrain))
            .isInstanceOf(AuthorizationDeniedException.class)
            .hasMessageContaining("a2");
    }

    @Test
    void partner_mayNotReassignOwnActivityToAnotherPartner() {
        Train oldTrain = train(location("CH-3527", activity("a1", BLS)));
        Train newTrain = train(location("CH-3527", activity("a1", SOB)));

        assertThatThrownBy(() -> policy.checkEditAuthorization(BLS, oldTrain, newTrain))
            .isInstanceOf(AuthorizationDeniedException.class)
            .hasMessageContaining("a1");
    }

    @Test
    void partner_mayNotAddJourneylocation() {
        Train oldTrain = train(location("CH-3527", activity("a1", BLS)));
        Train newTrain = train(
            location("CH-3527", activity("a1", BLS)),
            location("CH-309"));

        assertThatThrownBy(() -> policy.checkEditAuthorization(BLS, oldTrain, newTrain))
            .isInstanceOf(AuthorizationDeniedException.class)
            .hasMessageContaining("route")
            .hasMessageContaining("journeylocation count changed");
    }

    @Test
    void partner_mayNotChangeLocationOfTheRoute() {
        Train oldTrain = train(location("CH-3527", activity("a1", BLS)));
        Train newTrain = train(location("CH-9999", activity("a1", BLS)));

        assertThatThrownBy(() -> policy.checkEditAuthorization(BLS, oldTrain, newTrain))
            .isInstanceOf(AuthorizationDeniedException.class)
            .hasMessageContaining("route")
            .hasMessageContaining("locations changed at position 0");
    }

    @Test
    void partner_mayNotMoveForeignActivityBetweenLocationsWithSameCode() {
        // Two journeylocations share the same location code; a foreign (SOB) activity is moved from the
        // first to the second. The move must still be detected (no authorization bypass via duplicate codes).
        Train oldTrain = train(
            location("CH-3527", activity("a1", BLS), activity("a2", SOB)),
            location("CH-3527"));
        Train newTrain = train(
            location("CH-3527", activity("a1", BLS)),
            location("CH-3527", activity("a2", SOB)));

        assertThatThrownBy(() -> policy.checkEditAuthorization(BLS, oldTrain, newTrain))
            .isInstanceOf(AuthorizationDeniedException.class)
            .hasMessageContaining("a2");
    }

    @Test
    void partner_unchangedTrain_isAllowed() {
        Train oldTrain = train(location("CH-3527", activity("a1", BLS), activity("a2", SOB)));
        Train newTrain = train(location("CH-3527", activity("a1", BLS), activity("a2", SOB)));

        assertThatCode(() -> policy.checkEditAuthorization(BLS, oldTrain, newTrain))
            .doesNotThrowAnyException();
    }

    @Test
    void assignments_activityForSharedPartner_isAllowed() {
        Train train = train(location("CH-3527", activity("a1", BLS), activity("a2", SOB)));

        assertThatCode(() -> policy.checkActivityAssignments(train, List.of(LEAD_RU, BLS, SOB)))
            .doesNotThrowAnyException();
    }

    @Test
    void assignments_activityForNonSharedPartner_isRejected() {
        Train train = train(location("CH-3527", activity("a1", BLS), activity("a2", SOB)));

        assertThatThrownBy(() -> policy.checkActivityAssignments(train, List.of(LEAD_RU, BLS)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("a2")
            .hasMessageContaining(SOB);
    }

    @Test
    void assignments_activityWithoutResponsibility_isIgnored() {
        Train train = train(location("CH-3527", new Activity().id("a1").relations(new ArrayList<>())));

        assertThatCode(() -> policy.checkActivityAssignments(train, List.of(LEAD_RU)))
            .doesNotThrowAnyException();
    }

    // --- builders ---------------------------------------------------------------------------

    private static Train train(Journeylocation... journeylocations) {
        return new Train().journey(new Journey().journeylocations(new ArrayList<>(List.of(journeylocations))));
    }

    private static Journeylocation location(String code, Activity... activities) {
        return new Journeylocation()
            .locations(new ArrayList<>(List.of(
                new Location().type(LocationType.PLC).code(code).name(code))))
            .activities(new ArrayList<>(List.of(activities)));
    }

    private static Activity activity(String id, String owner) {
        return new Activity()
            .id(id)
            .relations(new ArrayList<>())
            .responsibility(new Responsibility().type(ResponsibilityType.RRU).id(owner).status(PlanStatus.CONFIRMED));
    }

    /** Mutates the activity in place (same instance, same id and owner) to simulate a content change. */
    private static Activity modify(Activity activity) {
        activity.getResponsibility().status(PlanStatus.REQUESTED);
        return activity;
    }
}
