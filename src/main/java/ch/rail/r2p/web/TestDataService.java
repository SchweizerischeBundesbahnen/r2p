package ch.rail.r2p.web;

import ch.rail.r2p.api.dto.Train;
import ch.rail.r2p.domain.TrainVersionEnriched;
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
    private final String instanceOwner;

    public TestDataService(TrainService trainService,
                           @Value("${r2p.instance-owner}") String instanceOwner) {
        this.trainService = trainService;
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

            // 1c. Build the train payload from the sample
            Train payload = createRandomTrain(random, operationalDay, trainNumber);

            // 1d. Create the train (main branch, owned by instanceOwner)
            TrainVersionEnriched created = trainService.createTrain(instanceOwner, "Test data", payload);
            UUID trainId = created.trainVersion().getTrainId();
            UUID mainBranchId = created.branch().getBranchId();
            trainsCreated++;
            versionsCreated++;

            // 1e. Share with 1 or 2 partners from the request list
            List<String> chosenPartners = choosePartners(request.partners(), random);
            for (String partner : chosenPartners) {
                trainService.shareTrain(instanceOwner, trainId, partner, "Test data – shared");
            }
            // All partners that have access (for branch creation)
            List<String> allPartners = new ArrayList<>();
            allPartners.add(instanceOwner);
            allPartners.addAll(chosenPartners);

            // 1f. ~10% chance to delete main branch (cascades to all other branches)
            if (random.nextDouble() < 0.1) {
                trainService.deleteTrainBranch(instanceOwner, trainId, mainBranchId, "Test data – deleted");
                mainBranchesDeleted++;
                branchesDeleted++;
                // Skip branch creation since cascading deletion already happened
                continue;
            }

            // 1g. Create 0–2 non-main branches
            int branchCount = random.nextInt(3); // 0, 1 or 2
            Set<String> usedBranchNames = new HashSet<>();
            for (int b = 0; b < branchCount; b++) {
                String branchOwner = allPartners.get(random.nextInt(allPartners.size()));
                String branchName;
                do {
                    branchName = "branch-" + (100 + random.nextInt(900));
                } while (!usedBranchNames.add(branchName));
                Train branchPayload = createRandomTrain(random, operationalDay, trainNumber);

                TrainVersionEnriched branch = trainService.createTrainBranch(
                    branchOwner, trainId, branchName, "Test data", branchPayload);
                UUID branchId = branch.branch().getBranchId();
                branchesCreated++;
                versionsCreated++;

                // 1–3 additional updates on this branch
                int updateCount = 1 + random.nextInt(3); // 1, 2 or 3
                for (int u = 0; u < updateCount; u++) {
                    Train updatePayload = createRandomTrain(random, operationalDay, trainNumber);
                    trainService.updateTrainBranch(branchOwner, trainId, branchId, "Update " + (u + 1), updatePayload);
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
