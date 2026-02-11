package ch.rail.r2p.web;

import static ch.rail.r2p.Constants.MAIN_BRANCH_NAME;

import ch.rail.r2p.api.dto.Branch;
import ch.rail.r2p.api.dto.BranchBusinessData;
import ch.rail.r2p.api.dto.BranchTechnicalData;
import ch.rail.r2p.api.dto.Change;
import ch.rail.r2p.api.dto.TrainListElement;
import ch.rail.r2p.domain.Changelog;
import ch.rail.r2p.domain.TrainVersion;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TrainMapper {

    TrainListElement toTrainListElement(Collection<TrainVersion> trains, List<String> partners) {
        TrainVersion main = trains.stream().filter(train -> train.getBranchName().equals(MAIN_BRANCH_NAME)).findAny().orElseThrow();
        return new TrainListElement()
            .trainId(main.getTrainId())
            .partners(partners)
            .mainTimestamp(main.getTimestamp())
            .branches(trains.stream().map(this::toBranch).toList());
    }

    Branch toBranch(TrainVersion train) {
        return new Branch()
            .branchId(train.getBranchId())
            .branchName(train.getPrefixedBranchName())
            .business(new BranchBusinessData()
                .trainNumber(train.getTrainNumber())
                .operationalDay(train.getOperationalDay()))
            .technical(new BranchTechnicalData()
                .branchState(train.getBranchState())
                .owner(train.getOwner())
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
