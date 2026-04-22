package ch.rail.r2p.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.rail.r2p.api.dto.Activity;
import ch.rail.r2p.api.dto.TimingType;
import ch.rail.r2p.api.dto.Train;
import java.time.Duration;
import java.time.LocalDate;
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
     * Hard-coded contract: exactly these three organisation codes are allowed in PoC data.
     * If the production constant {@link TestDataGenerator#POC_RESPONSIBILITY_TYPES} is changed
     * the test {@code poc_responsibility_types_must_match_contract} will catch the drift.
     * <ul>
     *   <li>1385 – SBB Personenverkehr</li>
     *   <li>1163 – BLS</li>
     *   <li>5458 – SOB Transport EVU</li>
     * </ul>
     */
    private static final Set<String> EXPECTED_POC_ORGANISATION_CODES = Set.of("1385", "1163", "5458");

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

        locations.getFirst().getActivities().forEach(activity -> {
            assertThat(activity.getTimings()).hasSize(1);
            assertThat(activity.getTimings().getFirst().getType()).isEqualTo(TimingType.DEPARTURE);
        });

        locations.getLast().getActivities().forEach(activity -> {
            assertThat(activity.getTimings()).hasSize(1);
            assertThat(activity.getTimings().getFirst().getType()).isEqualTo(TimingType.ARRIVAL);
        });
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
    void responsibilities_must_only_use_poc_organisation_codes() {
        Train train = TestDataGenerator.createTrain(LocalDate.of(2026, 7, 1), "7001");

        train.getJourney().getJourneylocations().forEach(jl ->
            jl.getActivities().forEach(activity -> {
                String orgCode = activity.getResponsibility().getId();
                assertThat(EXPECTED_POC_ORGANISATION_CODES)
                    .as("organisation code '%s' is not in the PoC contract set {1385=SBB, 1163=BLS, 5458=SOB}", orgCode)
                    .contains(orgCode);
            })
        );
    }

    @Test
    void poc_responsibility_types_must_match_contract() {
        Set<String> actual = TestDataGenerator.POC_RESPONSIBILITY_TYPES
            .stream()
            .map(TestDataGenerator.KnownResponsibilityType::getOrganisationCode)
            .collect(Collectors.toUnmodifiableSet());

        assertThat(actual)
            .as("POC_RESPONSIBILITY_TYPES organisation codes must exactly match the contract set")
            .containsExactlyInAnyOrderElementsOf(EXPECTED_POC_ORGANISATION_CODES);
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
