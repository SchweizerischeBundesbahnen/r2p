package ch.rail.r2p.web;

/**
 * Statistics for a single partner, or totals when {@code partner} is {@code null}.
 *
 * @param partner            partner name; {@code null} for the aggregated totals row
 * @param leadRU             {@code true} if this partner is the instance owner (Lead RU)
 * @param trains             number of trains (total for null partner, shared-with count for a specific partner)
 * @param openBranches       number of open branches (total for null partner, owned-by count for a specific partner)
 * @param openMergeRequests  number of open merge requests (MERGE_REQUESTED state)
 */
public record PartnerStats(
    String partner,
    boolean leadRU,
    int trains,
    int openBranches,
    int openMergeRequests
) {}

