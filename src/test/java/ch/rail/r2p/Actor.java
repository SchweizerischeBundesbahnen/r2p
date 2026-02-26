package ch.rail.r2p;

import static ch.rail.r2p.api.dto.PartnerToken.SBB;
import static ch.rail.r2p.domain.Branch.MAIN_BRANCH_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import ch.rail.r2p.api.dto.PartnerToken;
import ch.rail.r2p.api.dto.Train;
import ch.rail.r2p.domain.TrainVersion;
import ch.rail.r2p.persistence.TrainPersistence;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Actor {
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TrainPersistence trainPersistence;

    private Train sampleTrain;

    Set<UUID> trainIdsForCleanup = new HashSet<>();

    public static final String PREFIXED_MAIN_BRANCH_NAME = getPrefixedBranchName(SBB, MAIN_BRANCH_NAME);

    public Train loadSampleTrain() {
        if (sampleTrain == null) {
            try {
                sampleTrain = objectMapper.readValue(
                    this.getClass().getResourceAsStream("/openapi/sampleTrain.json"), Train.class);
                Objects.requireNonNull(sampleTrain);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load sample train", e);
            }
        }
        return sampleTrain;
    }

    public static String getPrefixedBranchName(PartnerToken owner, String branchName) {
        return "%s-%s".formatted(owner.name(), branchName);
    }

    public void setup() {
        // Collect all trainIds of trains inserted by trainPersistence.insertVersion
        doAnswer(invocation -> {
            invocation.callRealMethod();
            trainIdsForCleanup.add(((TrainVersion) invocation.getArgument(0)).getTrainId());
            return null;
        }).when(trainPersistence).insertVersion(any());
    }

    public void cleanup() {
        trainPersistence.deleteTrains(trainIdsForCleanup);
        trainIdsForCleanup.clear();
    }
}
