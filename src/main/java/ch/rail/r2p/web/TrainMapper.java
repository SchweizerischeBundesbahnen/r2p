package ch.rail.r2p.web;

import ch.rail.r2p.api.dto.Branch;
import ch.rail.r2p.api.dto.BranchBusinessData;
import ch.rail.r2p.api.dto.BranchTechnicalData;
import ch.rail.r2p.api.dto.Change;
import ch.rail.r2p.api.dto.TrainListElement;
import ch.rail.r2p.domain.Changelog;
import ch.rail.r2p.domain.TrainVersion;
import ch.rail.r2p.domain.TrainVersionEnriched;
import java.util.Collection;
import org.springframework.stereotype.Component;

@Component
public class TrainMapper {

    TrainListElement toTrainListElement(Collection<TrainVersionEnriched> trains) {
        String trainIdDescription = trains.stream()
            .findAny()
            .map(train -> train.trainVersion().getTrainId().toString())
            .orElse("unknown train (no versions provided)");

        TrainVersionEnriched main = trains.stream()
            .filter(train -> train.branch().isMainBranch())
            .findAny()
            .orElseThrow(() -> new IllegalStateException(
                "No main branch found for " + trainIdDescription));
        return new TrainListElement()
            .trainId(main.trainVersion().getTrainId())
            .partners(main.partners())
            .mainTimestamp(main.trainVersion().getTimestamp())
            .branches(trains.stream().map(this::toBranch).toList());
    }

    Branch toBranch(TrainVersionEnriched trainVersionEnriched) {
        TrainVersion train = trainVersionEnriched.trainVersion();
        return new Branch()
            .branchId(train.getBranchId())
            .branchName(trainVersionEnriched.branch().getPrefixedName())
            .business(new BranchBusinessData()
                .trainNumber(train.getTrainNumber())
                .operationalDay(train.getOperationalDay()))
            .technical(new BranchTechnicalData()
                .branchState(trainVersionEnriched.branch().getState())
                .owner(trainVersionEnriched.branch().getOwner())
                .version(train.getVersion())
                .timestamp(train.getTimestamp())
                .comment(train.getComment())
                .mainBranch(trainVersionEnriched.branch().isMainBranch()));
    }

    /**
     * Maps a domain {@link ch.rail.r2p.domain.Branch} directly to the API DTO,
     * without requiring a {@link ch.rail.r2p.domain.TrainVersion}.
     * Used by operations that must not deserialize the payload (e.g. branch deletion).
     */
    Branch toBranch(ch.rail.r2p.domain.Branch branch) {
        return new Branch()
            .branchId(branch.getBranchId())
            .branchName(branch.getPrefixedName())
            .business(new BranchBusinessData())
            .technical(new BranchTechnicalData()
                .branchState(branch.getState())
                .owner(branch.getOwner())
                .mainBranch(branch.isMainBranch()));
    }

    public Change toChange(Changelog changelog) {
        return new Change()
            .action(changelog.getAction())
            .partner(changelog.getPartner())
            .affectedPartner(changelog.getAffectedPartner())
            .trainId(changelog.getTrainId())
            .branchId(changelog.getBranchId())
            .branchName(changelog.getBranchName())
            .version(changelog.getVersion())
            .timestamp(changelog.getTimestamp())
            .comment(changelog.getComment());
    }
}
