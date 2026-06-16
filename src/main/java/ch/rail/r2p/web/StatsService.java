package ch.rail.r2p.web;

import ch.rail.r2p.persistence.impl.BranchSpringDataRepository;
import ch.rail.r2p.persistence.impl.TrainSpringDataRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final TrainSpringDataRepository trainSpringDataRepository;
    private final BranchSpringDataRepository branchSpringDataRepository;

    @Value("${r2p.instance-owner}")
    private String instanceOwner;

    public List<PartnerStats> getStats() {
        Map<String, Integer> trainsByPartner = trainSpringDataRepository.countTrainsPerPartner()
            .stream()
            .collect(Collectors.toMap(
                TrainSpringDataRepository.PartnerCount::partner,
                TrainSpringDataRepository.PartnerCount::cnt));

        Map<String, Integer> openBranchesByOwner = branchSpringDataRepository.countOpenNonMainBranchesByOwner()
            .stream()
            .collect(Collectors.toMap(
                BranchSpringDataRepository.OwnerCount::owner,
                BranchSpringDataRepository.OwnerCount::cnt));

        Map<String, Integer> mergeRequestsByOwner = branchSpringDataRepository.countMergeRequestedBranchesByOwner()
            .stream()
            .collect(Collectors.toMap(
                BranchSpringDataRepository.OwnerCount::owner,
                BranchSpringDataRepository.OwnerCount::cnt));

        int totalTrainsCount = trainsByPartner.getOrDefault(instanceOwner, 0);

        int totalOpenBranches = openBranchesByOwner.values().stream().mapToInt(Integer::intValue).sum();

        int totalMergeRequests = mergeRequestsByOwner.values().stream().mapToInt(Integer::intValue).sum();

        List<PartnerStats> perPartner = trainsByPartner.keySet().stream()
            .sorted(Comparator.naturalOrder())
            .map(partner -> new PartnerStats(
                partner,
                trainsByPartner.getOrDefault(partner, 0),
                openBranchesByOwner.getOrDefault(partner, 0),
                mergeRequestsByOwner.getOrDefault(partner, 0)))
            .toList();

        PartnerStats totals = new PartnerStats(null, totalTrainsCount, totalOpenBranches, totalMergeRequests);

        List<PartnerStats> result = new ArrayList<>();
        result.add(totals);
        result.addAll(perPartner);
        return result;
    }
}






