package ch.rail.r2p.web;

import ch.rail.r2p.api.dto.Activity;
import ch.rail.r2p.api.dto.Journeylocation;
import ch.rail.r2p.api.dto.Location;
import ch.rail.r2p.api.dto.Train;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Component;

/**
 * Enforces the two demo authorization rules when a partner submits train content
 * (via {@code createTrainBranch} / {@code updateTrainBranch}):
 * <ol>
 *   <li>The Lead RU (the instance owner) may edit everything: all activities and the route.</li>
 *   <li>Any other partner may not change the route (Streckenverlauf) and may only
 *       add/modify/remove activities assigned to them, i.e. where
 *       {@code activity.responsibility.id} equals the partner.</li>
 * </ol>
 * On violation an {@link AuthorizationDeniedException} (mapped to HTTP 403) is thrown with a
 * detailed message listing exactly which route or activity changes were rejected, to aid debugging.
 */
@Component
public class TrainEditPolicy {

    private final String instanceOwner;

    public TrainEditPolicy(@Value("${r2p.instance-owner}") String instanceOwner) {
        this.instanceOwner = instanceOwner;
    }

    /**
     * Verifies that the transition from {@code oldTrain} to {@code newTrain} is permitted for {@code partner}.
     *
     * @throws AuthorizationDeniedException if the partner is not the Lead RU and the changes touch the route
     *                                      or activities not assigned to them
     */
    public void checkEditAuthorization(String partner, Train oldTrain, Train newTrain) {
        // Rule 1: the Lead RU (instance owner) may edit everything.
        if (partner.equals(instanceOwner)) {
            return;
        }
        List<String> violations = new ArrayList<>();
        violations.addAll(findRouteViolations(oldTrain, newTrain));
        violations.addAll(findActivityViolations(partner, oldTrain, newTrain));
        if (!violations.isEmpty()) {
            throw new AuthorizationDeniedException(
                "Partner '" + partner + "' is not allowed to make the following changes: "
                    + String.join("; ", violations));
        }
    }

    /**
     * Validates that every activity is only assigned to a partner the train is shared with. An activity is
     * assigned via its {@code responsibility.id}; that id must be contained in {@code sharedPartners} (which
     * includes the Lead RU / instance owner). Activities without a responsibility id are unassigned and ignored.
     *
     * <p>Applies to every write (create, branch create, branch update) regardless of the acting partner.
     *
     * @throws IllegalArgumentException (mapped to HTTP 400) listing each activity assigned to a non-shared partner
     */
    public void checkActivityAssignments(Train train, Collection<String> sharedPartners) {
        List<String> violations = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        Set<String> duplicateIds = new TreeSet<>();
        for (Journeylocation journeylocation : journeylocations(train)) {
            String locationLabel = label(journeylocation);
            List<Activity> locationActivities = journeylocation.getActivities();
            if (locationActivities == null) {
                continue;
            }
            for (Activity activity : locationActivities) {
                // Activity.id is unique within a train; duplicates would let later id-indexed logic
                // (e.g. move/modify detection) silently drop one of the activities, so reject them.
                String id = activity == null ? null : activity.getId();
                if (id != null && !seenIds.add(id)) {
                    duplicateIds.add(id);
                }
                String owner = responsibilityId(activity);
                if (owner != null && !sharedPartners.contains(owner)) {
                    violations.add("activity '" + (activity == null ? null : activity.getId())
                        + "' (location " + locationLabel + ") is assigned to partner '" + owner
                        + "' the train is not shared with");
                }
            }
        }
        if (!duplicateIds.isEmpty()) {
            throw new IllegalArgumentException(
                "Train contains duplicate activity ids: " + String.join(", ", duplicateIds));
        }
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(
                "Train references partners it is not shared with: " + String.join("; ", violations));
        }
    }

    /**
     * Rule 2a: a non-Lead-RU partner must not change the route (Streckenverlauf). The route is the
     * ordered sequence of journeylocations and, per location, the {@code locations} and {@code requirement}
     * (everything except the {@code activities}).
     */
    private List<String> findRouteViolations(Train oldTrain, Train newTrain) {
        List<String> violations = new ArrayList<>();
        List<Journeylocation> oldLocs = journeylocations(oldTrain);
        List<Journeylocation> newLocs = journeylocations(newTrain);
        if (oldLocs.size() != newLocs.size()) {
            violations.add("route: journeylocation count changed from " + oldLocs.size() + " to " + newLocs.size());
        }
        int common = Math.min(oldLocs.size(), newLocs.size());
        for (int i = 0; i < common; i++) {
            Journeylocation oldLoc = oldLocs.get(i);
            Journeylocation newLoc = newLocs.get(i);
            if (!Objects.equals(oldLoc.getLocations(), newLoc.getLocations())) {
                violations.add("route: locations changed at position " + i
                    + " (" + label(oldLoc) + " -> " + label(newLoc) + ")");
            }
            if (!Objects.equals(oldLoc.getRequirement(), newLoc.getRequirement())) {
                violations.add("route: requirement changed at position " + i + " (" + label(newLoc) + ")");
            }
        }
        return violations;
    }

    /**
     * Rule 2b: a non-Lead-RU partner may only add, modify or remove activities assigned to them. An activity
     * counts as touched when it is added, removed, has its content changed, or is moved to another location.
     * For touched activities the partner must own them in both their old and new form (so they can neither
     * grab another partner's activity nor reassign their own away).
     */
    private List<String> findActivityViolations(String partner, Train oldTrain, Train newTrain) {
        Map<String, ActivityRef> oldActivities = indexActivities(oldTrain);
        Map<String, ActivityRef> newActivities = indexActivities(newTrain);
        List<String> violations = new ArrayList<>();
        Set<String> allIds = new TreeSet<>();
        allIds.addAll(oldActivities.keySet());
        allIds.addAll(newActivities.keySet());
        for (String id : allIds) {
            ActivityRef oldRef = oldActivities.get(id);
            ActivityRef newRef = newActivities.get(id);
            if (oldRef == null) {
                // added
                if (!ownedBy(newRef.activity(), partner)) {
                    violations.add("activity '" + id + "' added (responsibility '"
                        + responsibilityId(newRef.activity()) + "', location " + newRef.locationLabel()
                        + ") is not assigned to partner '" + partner + "'");
                }
            } else if (newRef == null) {
                // removed
                if (!ownedBy(oldRef.activity(), partner)) {
                    violations.add("activity '" + id + "' removed (responsibility '"
                        + responsibilityId(oldRef.activity()) + "', location " + oldRef.locationLabel()
                        + ") is not assigned to partner '" + partner + "'");
                }
            } else if (!Objects.equals(oldRef.activity(), newRef.activity())
                || oldRef.journeylocationIndex() != newRef.journeylocationIndex()) {
                // modified or moved -> must be owned by the partner before and after
                // (compare by journeylocation index so moves are detected even when two
                //  journeylocations share the same location code)
                if (!ownedBy(oldRef.activity(), partner) || !ownedBy(newRef.activity(), partner)) {
                    violations.add("activity '" + id + "' modified (responsibility '"
                        + responsibilityId(oldRef.activity()) + "' -> '" + responsibilityId(newRef.activity())
                        + "', location " + newRef.locationLabel() + ") is not assigned to partner '" + partner + "'");
                }
            }
        }
        return violations;
    }

    private Map<String, ActivityRef> indexActivities(Train train) {
        Map<String, ActivityRef> activities = new LinkedHashMap<>();
        List<Journeylocation> journeylocations = journeylocations(train);
        for (int index = 0; index < journeylocations.size(); index++) {
            Journeylocation journeylocation = journeylocations.get(index);
            String locationLabel = label(journeylocation);
            List<Activity> locationActivities = journeylocation.getActivities();
            if (locationActivities == null) {
                continue;
            }
            for (Activity activity : locationActivities) {
                if (activity == null || activity.getId() == null) {
                    continue;
                }
                activities.put(activity.getId(), new ActivityRef(activity, locationLabel, index));
            }
        }
        return activities;
    }

    private static List<Journeylocation> journeylocations(Train train) {
        if (train == null || train.getJourney() == null || train.getJourney().getJourneylocations() == null) {
            return List.of();
        }
        return train.getJourney().getJourneylocations();
    }

    private static boolean ownedBy(Activity activity, String partner) {
        return responsibilityId(activity) != null && partner.equals(responsibilityId(activity));
    }

    private static String responsibilityId(Activity activity) {
        if (activity == null || activity.getResponsibility() == null) {
            return null;
        }
        return activity.getResponsibility().getId();
    }

    private static String label(Journeylocation journeylocation) {
        if (journeylocation == null || journeylocation.getLocations() == null
            || journeylocation.getLocations().isEmpty()) {
            return "?";
        }
        Location first = journeylocation.getLocations().getFirst();
        return first != null && first.getCode() != null ? first.getCode() : "?";
    }

    /**
     * An activity together with where it currently sits: {@code locationLabel} is the (possibly non-unique)
     * location code used for human-readable messages, while {@code journeylocationIndex} is the position in
     * the journey and is what move detection compares against (so moves between two journeylocations sharing
     * the same location code are still detected).
     */
    private record ActivityRef(Activity activity, String locationLabel, int journeylocationIndex) {
    }
}
