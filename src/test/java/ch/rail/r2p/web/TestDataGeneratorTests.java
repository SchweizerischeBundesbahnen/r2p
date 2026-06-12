package ch.rail.r2p.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.rail.r2p.api.dto.Activity;
import ch.rail.r2p.api.dto.ActivityType;
import ch.rail.r2p.api.dto.CapabilityType;
import ch.rail.r2p.api.dto.PartnerToken;
import ch.rail.r2p.api.dto.ResponsibilityType;
import ch.rail.r2p.api.dto.TimingType;
import ch.rail.r2p.api.dto.Train;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class TestDataGeneratorTests {

    /**
     * Partner abbreviations derived from {@link PartnerToken} used as Responsibility.id in PoC data.
     * Intentionally not RICS codes — the webclient uses EVU abbreviations for the PoC.
     */
    private static final Set<String> EXPECTED_POC_PARTNER_ABBREVIATIONS = Arrays.stream(PartnerToken.values())
        .map(PartnerToken::getValue)
        .collect(Collectors.toUnmodifiableSet());

    @Test
    void activity_ids_within_a_train_must_be_unique_and_not_blank() {
        Train train = TestDataGenerator.createTrain(LocalDate.of(2026, 5, 1), "1234");

        List<String> allIds = train.getJourney().getJourneylocations().stream()
            .flatMap(jl -> jl.getActivities().stream())
            .map(Activity::getId)
            .toList();

        allIds.forEach(id ->
            assertThat(id).as("activity ID must not be blank").isNotBlank()
        );

        Set<String> uniqueIds = Set.copyOf(allIds);
        assertThat(uniqueIds)
            .as("all activity IDs within a train must be unique")
            .hasSize(allIds.size());
    }

    @Test
    void terminal_stops_must_have_exactly_one_timing_of_correct_type() {
        Train train = TestDataGenerator.createTrain(LocalDate.of(2026, 5, 1), "2001");
        var locations = train.getJourney().getJourneylocations();

        // RollingStock activities carry no timing; only path and staff activities do.
        locations.getFirst().getActivities().stream()
            .filter(a -> a.getCapability() != null && a.getCapability().getType() != CapabilityType.ROLLING_STOCK)
            .forEach(activity -> {
                assertThat(activity.getTimings()).hasSize(1);
                assertThat(activity.getTimings().getFirst().getType()).isEqualTo(TimingType.DEPARTURE);
            });

        locations.getLast().getActivities().forEach(activity -> {
            assertThat(activity.getTimings()).hasSize(1);
            assertThat(activity.getTimings().getFirst().getType()).isEqualTo(TimingType.ARRIVAL);
        });
    }

    @Test
    void rolling_stock_activities_must_have_no_timings() {
        Train train = TestDataGenerator.createTrain(LocalDate.of(2026, 5, 1), "3001");

        train.getJourney().getJourneylocations().forEach(jl ->
            jl.getActivities().stream()
                .filter(a -> a.getCapability() != null && a.getCapability().getType() == CapabilityType.ROLLING_STOCK)
                .forEach(activity ->
                    assertThat(activity.getTimings())
                        .as("rollingStock activity '%s' must have no timings", activity.getId())
                        .isEmpty()
                )
        );
    }

    @Test
    void timings_must_have_a_source() {
        Train train = TestDataGenerator.createTrain(LocalDate.of(2026, 5, 1), "4001");

        train.getJourney().getJourneylocations().forEach(jl ->
            jl.getActivities().forEach(activity ->
                activity.getTimings().forEach(timing ->
                    assertThat(timing.getSource())
                        .as("timing source must not be null or blank")
                        .isNotBlank()
                )
            )
        );
    }

    @Test
    void activity_type_must_be_consistent_with_capability_type() {
        Train train = TestDataGenerator.createTrain(LocalDate.of(2026, 5, 1), "5001");

        train.getJourney().getJourneylocations().forEach(jl ->
            jl.getActivities().forEach(activity -> {
                if (activity.getCapability() == null) return;
                CapabilityType capType = activity.getCapability().getType();
                ActivityType actType = activity.getType();
                switch (capType) {
                    case PATH -> assertThat(actType.getValue()).startsWith("path.");
                    case ROLLING_STOCK -> assertThat(actType.getValue()).startsWith("rollingStock");
                    case STAFF -> assertThat(actType).isNotNull();
                    default -> { /* no assertion for unknown types */ }
                }
            })
        );
    }

    @Test
    void responsibility_type_must_match_capability_type() {
        Train train = TestDataGenerator.createTrain(LocalDate.of(2026, 5, 1), "6001");

        train.getJourney().getJourneylocations().forEach(jl ->
            jl.getActivities().forEach(activity -> {
                if (activity.getCapability() == null || activity.getResponsibility() == null) return;
                CapabilityType capType = activity.getCapability().getType();
                ResponsibilityType respType = activity.getResponsibility().getType();
                switch (capType) {
                    case PATH -> assertThat(respType).isIn(ResponsibilityType.PIM, ResponsibilityType.RIM);
                    case ROLLING_STOCK -> assertThat(respType).isIn(ResponsibilityType.TRU, ResponsibilityType.RRU);
                    case STAFF -> assertThat(respType).isEqualTo(ResponsibilityType.PRU);
                    default -> { /* no assertion for unknown types */ }
                }
            })
        );
    }

    @Test
    void max_speed_must_be_a_multiple_of_10() {
        Train train = TestDataGenerator.createTrain(LocalDate.of(2026, 5, 1), "8001");

        train.getJourney().getJourneylocations().forEach(jl ->
            jl.getActivities().forEach(activity -> {
                if (activity.getCapability() == null) return;
                var rs = activity.getCapability().getRollingStock();
                if (rs != null && rs.getMaxSpeed() != null) {
                    assertThat(rs.getMaxSpeed() % 10)
                        .as("rollingStock maxSpeed %d must be a multiple of 10", rs.getMaxSpeed())
                        .isZero();
                }
                var path = activity.getCapability().getPath();
                if (path != null && path.getMaxSpeed() != null) {
                    assertThat(path.getMaxSpeed() % 10)
                        .as("path maxSpeed %d must be a multiple of 10", path.getMaxSpeed())
                        .isZero();
                }
            })
        );
    }

    @Test
    void generated_timings_must_be_valid_absolute_offsets() {
        Train train = TestDataGenerator.createTrain(LocalDate.of(2026, 5, 1), "9001");
        var locations = train.getJourney().getJourneylocations();

        // All timings must be valid ISO-8601 durations and must increase monotonically
        List<Duration> offsets = locations.stream()
            .flatMap(jl -> jl.getActivities().stream())
            .flatMap(a -> a.getTimings().stream())
            .map(t -> Duration.parse(t.getDuration()))
            .toList();

        for (int i = 1; i < offsets.size(); i++) {
            assertThat(offsets.get(i))
                .as("offset at index %d must be >= its predecessor", i)
                .isGreaterThanOrEqualTo(offsets.get(i - 1));
        }
    }

    @Test
    void responsibilities_must_only_use_poc_partner_abbreviations() {
        Train train = TestDataGenerator.createTrain(LocalDate.of(2026, 7, 1), "7001");

        train.getJourney().getJourneylocations().forEach(jl ->
            jl.getActivities().forEach(activity -> {
                String abbreviation = activity.getResponsibility().getId();
                assertThat(EXPECTED_POC_PARTNER_ABBREVIATIONS)
                    .as("partner abbreviation '%s' is not in the PoC contract set {SBB, BLS, SOB}", abbreviation)
                    .contains(abbreviation);
            })
        );
    }

    @Test
    void createUniqueTrainNumber_throws_when_pool_exhausted() {
        LocalDate day = LocalDate.of(2026, 1, 1);
        Map<LocalDate, Set<String>> usedTrainNumbers = new HashMap<>();
        // Pre-fill all 9 000 numbers (1000–9999)
        Set<String> full = IntStream.rangeClosed(1000, 9999)
            .mapToObj(Integer::toString)
            .collect(Collectors.toCollection(HashSet::new));
        usedTrainNumbers.put(day, full);

        assertThatThrownBy(() -> TestDataGenerator.createUniqueTrainNumber(new Random(), day, usedTrainNumbers))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("exhausted")
            .hasMessageContaining(day.toString());
    }

}
