package ch.rail.r2p.web;

import ch.rail.r2p.api.dto.Activity;
import ch.rail.r2p.api.dto.Journeylocation;
import ch.rail.r2p.api.dto.PlanStatus;
import ch.rail.r2p.api.dto.Train;
import ch.rail.r2p.domain.TrainVersionEnriched;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "r2p.test-data.enabled", havingValue = "true")
public class TestDataService {

    private final TrainService trainService;
    private final ObjectMapper objectMapper;
    private final String instanceOwner;

    public TestDataService(TrainService trainService,
                           ObjectMapper objectMapper,
                           @Value("${r2p.instance-owner}") String instanceOwner) {
        this.trainService = trainService;
        this.objectMapper = objectMapper;
        this.instanceOwner = instanceOwner;
    }

    public TestDataResult generateTrains(@Valid TestDataRequest request) {
        validateCapacity(request);
        Random random = new Random();
        Map<LocalDate, Set<String>> usedTrainNumbers = new HashMap<>();

        int trainsCreated = 0;
        int branchesCreated = 0;
        int branchesDeleted = 0;
        int mainBranchesDeleted = 0;
        int versionsCreated = 0;
        int mergeRequestsCreated = 0;

        for (int i = 0; i < request.numberOfTrains(); i++) {
            // 1a. Pick a random operational day in the range
            LocalDate operationalDay = TestDataGenerator.createOperationDay(random, request.dateFrom(), request.dateTo());

            // 1b. Generate a unique train number for this day
            String trainNumber = TestDataGenerator.createUniqueTrainNumber(random, operationalDay, usedTrainNumbers);

            // 1c. Decide which partners the train will be shared with. Activities may only be assigned to
            //     shared partners, so this has to be known before activities are distributed.
            List<String> chosenPartners = choosePartners(request.partners(), random);
            // All partners that have access (instance owner + shared partners)
            List<String> allPartners = new ArrayList<>();
            allPartners.add(instanceOwner);
            allPartners.addAll(chosenPartners);

            // 1d. Build the train payload. At creation the train is shared with no one yet, so every
            //     activity is initially assigned to the Lead RU (instance owner).
            Train payload = createRandomTrain(random, operationalDay, trainNumber);
            constrainToPartners(payload, List.of(instanceOwner), random);

            // 1e. Create the train (main branch, owned by instanceOwner / Lead RU)
            TrainVersionEnriched created = trainService.createTrain(instanceOwner, "Test data", payload);
            UUID trainId = created.trainVersion().getTrainId();
            UUID mainBranchId = created.branch().getBranchId();
            trainsCreated++;
            versionsCreated++;

            // 1f. Share with the chosen partners
            for (String partner : chosenPartners) {
                trainService.shareTrain(instanceOwner, trainId, partner, "Test data – shared");
            }

            // 1g. The Lead RU now distributes activities across all shared partners
            Train mainPayload = created.trainVersion().getPayload();
            if (!chosenPartners.isEmpty()) {
                Train distributed = deepCopy(mainPayload);
                distributeToPartners(distributed, allPartners, random);
                trainService.updateTrainBranch(instanceOwner, trainId, mainBranchId, "Test data – assign partners", distributed);
                mainPayload = distributed;
                versionsCreated++;
            }

            // 1h. ~10% chance to delete main branch (cascades to all other branches)
            if (random.nextDouble() < 0.1) {
                trainService.deleteTrainBranch(instanceOwner, trainId, mainBranchId, "Test data – deleted");
                mainBranchesDeleted++;
                branchesDeleted++;
                // Skip branch creation since cascading deletion already happened
                continue;
            }

            // 1i. Create 0–2 non-main branches
            int branchCount = random.nextInt(3); // 0, 1 or 2
            Set<String> usedBranchNames = new HashSet<>();
            for (int b = 0; b < branchCount; b++) {
                String branchOwner = allPartners.get(random.nextInt(allPartners.size()));
                String branchName;
                do {
                    branchName = "branch-" + (100 + random.nextInt(900));
                } while (!usedBranchNames.add(branchName));
                // The Lead RU may rewrite the train freely; other partners may only edit their own
                // activities and must leave the route (Streckenverlauf) and other partners' activities intact.
                Train branchPayload = branchPayloadFor(random, branchOwner, mainPayload, allPartners, operationalDay, trainNumber);

                TrainVersionEnriched branch = trainService.createTrainBranch(
                    branchOwner, trainId, branchName, "Test data", branchPayload);
                UUID branchId = branch.branch().getBranchId();
                Train branchCurrent = branchPayload;
                branchesCreated++;
                versionsCreated++;

                // 1–3 additional updates on this branch
                int updateCount = 1 + random.nextInt(3); // 1, 2 or 3
                for (int u = 0; u < updateCount; u++) {
                    Train updatePayload = branchPayloadFor(random, branchOwner, branchCurrent, allPartners, operationalDay, trainNumber);
                    trainService.updateTrainBranch(branchOwner, trainId, branchId, "Update " + (u + 1), updatePayload);
                    branchCurrent = updatePayload;
                    versionsCreated++;
                }

                // ~10% chance to delete, ~45% chance to request a merge, ~45% chance to leave open
                double roll = random.nextDouble();
                if (roll < 0.1) {
                    trainService.deleteTrainBranch(branchOwner, trainId, branchId, "Test data – deleted");
                    branchesDeleted++;
                } else if (roll < 0.55) {
                    // ~45% chance to request a merge on open branches
                    trainService.requestMerge(branchOwner, trainId, branchId, "Test data – merge request");
                    mergeRequestsCreated++;
                }
            }
        }

        return new TestDataResult(trainsCreated, branchesCreated, branchesDeleted, mainBranchesDeleted, versionsCreated, mergeRequestsCreated);
    }
    
    private Train createRandomTrain(Random random, LocalDate operationalDay, String trainNumber) {
        return TestDataGenerator.createTrain(random, operationalDay, trainNumber);
    }

    /**
     * Builds a branch/update payload that respects the edit authorization rules: the Lead RU may use a freshly
     * generated train (it is allowed to change anything), while any other partner derives the payload from
     * {@code basePayload} and only mutates the activities assigned to it, keeping the route and other partners'
     * activities untouched.
     */
    private Train branchPayloadFor(Random random, String owner, Train basePayload, List<String> allowedPartners,
                                   LocalDate operationalDay, String trainNumber) {
        if (owner.equals(instanceOwner)) {
            // Lead RU may rewrite the train; only constraint is that activities stay within shared partners.
            Train fresh = createRandomTrain(random, operationalDay, trainNumber);
            constrainToPartners(fresh, allowedPartners, random);
            return fresh;
        }
        Train copy = deepCopy(basePayload);
        mutateOwnedActivities(copy, owner, random);
        return copy;
    }

    /** Changes the plan status of every activity assigned to {@code owner}, leaving the route and others intact. */
    private static void mutateOwnedActivities(Train train, String owner, Random random) {
        forEachActivity(train, activity -> {
            if (activity.getResponsibility() != null && owner.equals(activity.getResponsibility().getId())) {
                activity.getResponsibility().setStatus(PlanStatus.values()[random.nextInt(PlanStatus.values().length)]);
            }
        });
    }

    /** Reassigns only the activities whose owner is not in {@code allowed} to a random allowed partner. */
    private static void constrainToPartners(Train train, List<String> allowed, Random random) {
        forEachActivity(train, activity -> {
            if (activity.getResponsibility() == null) {
                return;
            }
            String id = activity.getResponsibility().getId();
            if (id == null || !allowed.contains(id)) {
                activity.getResponsibility().setId(allowed.get(random.nextInt(allowed.size())));
            }
        });
    }

    /** Randomly (re)assigns every activity to one of the allowed partners. */
    private static void distributeToPartners(Train train, List<String> allowed, Random random) {
        forEachActivity(train, activity -> {
            if (activity.getResponsibility() != null) {
                activity.getResponsibility().setId(allowed.get(random.nextInt(allowed.size())));
            }
        });
    }

    private static void forEachActivity(Train train, java.util.function.Consumer<Activity> action) {
        if (train.getJourney() == null || train.getJourney().getJourneylocations() == null) {
            return;
        }
        for (Journeylocation journeylocation : train.getJourney().getJourneylocations()) {
            if (journeylocation.getActivities() == null) {
                continue;
            }
            journeylocation.getActivities().forEach(action);
        }
    }

    private Train deepCopy(Train train) {
        try {
            return objectMapper.readValue(objectMapper.writeValueAsBytes(train), Train.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to copy train payload", e);
        }
    }

    /**
     * Fails fast if the requested number of trains cannot possibly be satisfied by the available
     * date range and the fixed pool of 9 000 train numbers (1000–9999) per operational day.
     *
     * <p>Without this check {@link TestDataGenerator#createUniqueTrainNumber} would throw an
     * {@link IllegalStateException} once the pool for a given day is exhausted, but only after
     * potentially many costly retry attempts as the pool nears exhaustion. Validating upfront
     * provides a clearer error message before any trains are created.
     */
    private static void validateCapacity(TestDataRequest request) {
        long rangeDays = request.dateTo().toEpochDay() - request.dateFrom().toEpochDay() + 1;
        if (rangeDays <= 0) {
            // date-range validation is handled elsewhere; nothing to check here
            return;
        }
        long maxTrains = rangeDays * TestDataGenerator.TRAIN_NUMBERS_PER_DAY;
        if (request.numberOfTrains() > maxTrains) {
            throw new IllegalArgumentException(
                "numberOfTrains (" + request.numberOfTrains() + ") exceeds the maximum capacity of " +
                maxTrains + " for the given date range (" + rangeDays + " day(s) × " +
                TestDataGenerator.TRAIN_NUMBERS_PER_DAY + " numbers/day).");
        }
    }

    /**
     * Returns an empty list if the provided list is empty.
     */
    private List<String> choosePartners(List<String> available, Random random) {
        if (available == null || available.isEmpty()) {
            return List.of();
        }
        if (available.size() == 1) {
            return List.copyOf(available);
        }
        // Shuffle a copy and take 1 or 2
        List<String> shuffled = new ArrayList<>(available);
        java.util.Collections.shuffle(shuffled, random);
        int count = 1 + random.nextInt(2);
        return shuffled.subList(0, count);
    }

}
