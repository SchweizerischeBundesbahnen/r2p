package ch.rail.r2p.web;

import ch.rail.r2p.api.dto.Train;
import ch.rail.r2p.domain.TrainVersionEnriched;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "r2p.test-data.enabled", havingValue = "true")
public class TestDataService {

    private final TrainService trainService;
    private final ObjectMapper objectMapper;
    private final String instanceOwner;

    private Train sampleTrain;

    public TestDataService(TrainService trainService, ObjectMapper objectMapper,
                           @Value("${r2p.instance-owner}") String instanceOwner) {
        this.trainService = trainService;
        this.objectMapper = objectMapper;
        this.instanceOwner = instanceOwner;
    }

    @PostConstruct
    void loadSampleTrain() {
        try {
            var resource = new ClassPathResource("openapi/sampleTrain.json");
            sampleTrain = objectMapper.readValue(resource.getInputStream(), Train.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load sampleTrain.json", e);
        }
    }

    public TestDataResult generate(TestDataRequest request) {
        Random random = new Random();
        // Track used train numbers per date to ensure uniqueness within this generation run
        Map<LocalDate, Set<String>> usedTrainNumbers = new HashMap<>();

        int trainsCreated = 0;
        int branchesCreated = 0;
        int branchesDeleted = 0;
        int mainBranchesDeleted = 0;
        int versionsCreated = 0;
        int mergeRequestsCreated = 0;

        long rangeDays = request.dateFrom().toEpochDay();
        long rangeEnd = request.dateTo().toEpochDay();
        long rangeLength = rangeEnd - rangeDays + 1;
        if (rangeLength <= 0) {
            throw new IllegalArgumentException("dateTo must be on or after dateFrom");
        }

        for (int i = 0; i < request.numberOfTrains(); i++) {
            // 1a. Pick a random operational day in the range
            LocalDate operationalDay = LocalDate.ofEpochDay(rangeDays + (long) (random.nextDouble() * rangeLength));

            // 1b. Generate a unique train number for this day
            Set<String> usedForDay = usedTrainNumbers.computeIfAbsent(operationalDay, _ -> new HashSet<>());
            String trainNumber;
            do {
                trainNumber = String.valueOf((1000 + random.nextInt(9000)));
            } while (!usedForDay.add(trainNumber));

            // 1c. Build the train payload from the sample
            Train payload = cloneTrain(operationalDay, trainNumber);

            // 1d. Create the train (main branch, owned by instanceOwner)
            TrainVersionEnriched created = trainService.createTrain(instanceOwner, "Test data", payload);
            UUID trainId = created.trainVersion().getTrainId();
            UUID mainBranchId = created.branch().getBranchId();
            trainsCreated++;
            versionsCreated++;

            // 1e. Share with 1 or 2 partners from the request list
            List<String> chosenPartners = choosePartners(request.partners(), random);
            for (String partner : chosenPartners) {
                trainService.shareTrain(instanceOwner, trainId, partner);
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
            for (int b = 0; b < branchCount; b++) {
                String branchOwner = allPartners.get(random.nextInt(allPartners.size()));
                String branchName = "branch-" + (100 + random.nextInt(900));
                Train branchPayload = cloneTrain(operationalDay, trainNumber);

                TrainVersionEnriched branch = trainService.createTrainBranch(
                    branchOwner, trainId, branchName, "Test data", branchPayload);
                UUID branchId = branch.branch().getBranchId();
                branchesCreated++;
                versionsCreated++;

                // 1–3 additional updates on this branch
                int updateCount = 1 + random.nextInt(3); // 1, 2 or 3
                for (int u = 0; u < updateCount; u++) {
                    Train updatePayload = cloneTrain(operationalDay, trainNumber);
                    trainService.updateTrainBranch(branchOwner, trainId, branchId, "Update " + (u + 1), updatePayload);
                    versionsCreated++;
                }

                // ~10% chance to delete this branch
                if (random.nextDouble() < 0.1) {
                    trainService.deleteTrainBranch(branchOwner, trainId, branchId, "Test data – deleted");
                    branchesDeleted++;
                } else if (random.nextDouble() < 0.5) {
                    // ~50% chance to request a merge on open branches
                    trainService.requestMerge(branchOwner, trainId, branchId, "Test data – merge request");
                    mergeRequestsCreated++;
                }
            }
        }

        return new TestDataResult(trainsCreated, branchesCreated, branchesDeleted, mainBranchesDeleted, versionsCreated, mergeRequestsCreated);
    }

    /**
     * Returns a deep copy of the sample Train with the given operational day and train number applied.
     */
    private Train cloneTrain(LocalDate operationalDay, String trainNumber) {
        // Deep copy via JSON round-trip
        Train copy = objectMapper.convertValue(sampleTrain, Train.class);
        copy.setId(trainNumber);
        copy.setStartdate(operationalDay);
        copy.setTimestamp(OffsetDateTime.now());
        return copy;
    }

    /**
     * Randomly picks 1 or 2 partners from the provided list.
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



