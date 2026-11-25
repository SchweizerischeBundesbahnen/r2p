package ch.rail.r2p.domain;

import java.util.List;

/**
 * TrainVersion enriched with information of corresponding branch and list of partners of the train.
 */
public record TrainVersionEnriched(TrainVersion trainVersion, Branch branch, List<String> partners) {

}
