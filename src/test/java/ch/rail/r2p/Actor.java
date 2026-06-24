package ch.rail.r2p;

import static ch.rail.r2p.api.dto.PartnerToken.SBB;
import static ch.rail.r2p.domain.Branch.MAIN_BRANCH_NAME;

import ch.rail.r2p.api.dto.Branch;
import ch.rail.r2p.api.dto.PartnerToken;
import ch.rail.r2p.api.dto.Train;
import ch.rail.r2p.api.dto.TrainListElement;
import ch.rail.r2p.persistence.impl.BranchSpringDataRepository;
import ch.rail.r2p.persistence.impl.ChangelogSpringDataRepository;
import ch.rail.r2p.persistence.impl.TrainSpringDataRepository;
import ch.rail.r2p.web.PartnerStats;
import ch.rail.r2p.web.StatsController;
import ch.rail.r2p.web.TrainController;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Actor {
    @Autowired
    private ObjectMapper objectMapper;


    @Autowired
    private TrainController trainController;

    @Autowired
    private StatsController statsController;

    @Autowired
    private TrainSpringDataRepository trainSpringDataRepository;

    @Autowired
    private BranchSpringDataRepository branchSpringDataRepository;

    @Autowired
    private ChangelogSpringDataRepository changelogSpringDataRepository;

    private Train sampleTrain;

    Set<UUID> trainIdsForCleanup = new HashSet<>();

    public static final String PREFIXED_MAIN_BRANCH_NAME = getPrefixedBranchName(SBB, MAIN_BRANCH_NAME);

    public Train loadSampleTrain() {
        if (sampleTrain == null) {
            sampleTrain = loadTrain("/openapi/sampleTrain.json");
        }
        return sampleTrain;
    }

    /** Loads a fresh (non-cached) Train from the given classpath resource. */
    public Train loadTrain(String resourcePath) {
        try (var in = Objects.requireNonNull(this.getClass().getResourceAsStream(resourcePath),
            "Train resource not found on classpath: " + resourcePath)) {
            return Objects.requireNonNull(objectMapper.readValue(in, Train.class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load train from " + resourcePath, e);
        }
    }

    public static String getPrefixedBranchName(PartnerToken owner, String branchName) {
        return "%s-%s".formatted(owner.name(), branchName);
    }

    public TrainListElement createTrain(PartnerToken owner, String comment) {
        return createTrain(owner, comment, loadSampleTrain());
    }

    public TrainListElement createTrain(PartnerToken owner, String comment, Train train) {
        TrainListElement created = trainController.createTrain(owner, comment, train).getBody();
        Objects.requireNonNull(created, "createTrain returned null body");
        trainIdsForCleanup.add(created.getTrainId());
        return created;
    }

    public void shareTrain(PartnerToken owner, UUID trainId, PartnerToken partner, String comment) {
        trainController.shareTrain(owner, trainId, partner.name(), comment);
    }

    public Branch createBranch(PartnerToken owner, UUID trainId, String branchName, String comment) {
        return createBranch(owner, trainId, branchName, comment, loadSampleTrain());
    }

    public Branch createBranch(PartnerToken owner, UUID trainId, String branchName, String comment, Train train) {
        Branch branch = trainController.createTrainBranch(owner, trainId, branchName, comment, train).getBody();
        Objects.requireNonNull(branch, "createTrainBranch returned null body");
        return branch;
    }

    public void setup() {
        if (!trainIdsForCleanup.isEmpty()) {
            log.warn("Actor.cleanup() was not called after the previous test — running cleanup now. "
                + "Leftover train IDs: {}", trainIdsForCleanup);
            cleanup();
        }
    }

    public void cleanup() {
        // Delete all associated data for each train using repository methods
        // Order: partners, changelog, then branches (which cascades to train_version via ON DELETE CASCADE)
        trainIdsForCleanup.forEach(trainId -> {
            trainSpringDataRepository.deleteTrainPartnersByTrainId(trainId);
            changelogSpringDataRepository.deleteByTrainId(trainId);
            branchSpringDataRepository.deleteBranchesByTrainId(trainId);
        });
        trainIdsForCleanup.clear();
    }

    public List<PartnerStats> getStats() {
        return statsController.getStats();
    }
}
