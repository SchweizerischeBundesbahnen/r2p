package ch.rail.r2p.web;

import static org.assertj.core.api.Assertions.assertThat;

import ch.rail.r2p.api.dto.Activity;
import ch.rail.r2p.api.dto.TimingType;
import ch.rail.r2p.api.dto.Train;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class TestDataGeneratorTests {

    private static final Set<String> POC_ORGANISATION_CODES = TestDataGenerator.POC_RESPONSIBILITY_TYPES
        .stream()
        .map(TestDataGenerator.KnownResponsibilityType::getOrganisationCode)
        .collect(Collectors.toUnmodifiableSet());

    @Test
    void activity_ids_within_a_train_must_be_unique_and_not_blank() {
        Train train = TestDataGenerator.createTrain(LocalDate.of(2026, 5, 1), "1234");

        List<String> allIds = train.getJourney().getJourneylocations().stream()
            .flatMap(jl -> jl.getActivities().stream())
            .map(Activity::getId)
            .toList();

        allIds.forEach(id ->
            assertThat(id).as("Activity-ID darf nicht leer sein").isNotBlank()
        );

        Set<String> uniqueIds = Set.copyOf(allIds);
        assertThat(uniqueIds)
            .as("Alle Activity-IDs innerhalb eines Zuges müssen eindeutig sein")
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

        // Alle Timings müssen gültiges ISO-8601-Format haben und monoton steigen
        List<Duration> offsets = locations.stream()
            .flatMap(jl -> jl.getActivities().stream())
            .flatMap(a -> a.getTimings().stream())
            .map(t -> Duration.parse(t.getDuration()))
            .toList();

        for (int i = 1; i < offsets.size(); i++) {
            assertThat(offsets.get(i))
                .as("Offset bei Index %d muss >= Vorgänger sein", i)
                .isGreaterThanOrEqualTo(offsets.get(i - 1));
        }
    }

    @Test
    void responsibilities_must_only_use_poc_organisation_codes() {
        Train train = TestDataGenerator.createTrain(LocalDate.of(2026, 7, 1), "7001");

        train.getJourney().getJourneylocations().forEach(jl ->
            jl.getActivities().forEach(activity -> {
                String orgCode = activity.getResponsibility().getId();
                assertThat(POC_ORGANISATION_CODES)
                    .as("Organisations-Code '%s' ist nicht im PoC-Set (SBB, BLS, SOB)", orgCode)
                    .contains(orgCode);
            })
        );
    }

}
