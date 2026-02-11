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
        TrainVersionEnriched main = trains.stream().filter(train -> train.branch().isMainBranch()).findAny().orElseThrow();
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
                .comment(train.getComment()));
    }

    public Change toChange(Changelog changelog) {
        return new Change()
            .action(changelog.getAction())
            .partner(changelog.getPartner())
            .concernedPartner(changelog.getConcernedPartner())
            .trainId(changelog.getTrainId())
            .branchId(changelog.getBranchId())
            .branchName(changelog.getBranchName())
            .version(changelog.getVersion())
            .timestamp(changelog.getTimestamp())
            .comment(changelog.getComment());
    }
}
