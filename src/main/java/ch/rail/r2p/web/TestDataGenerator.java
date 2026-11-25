package ch.rail.r2p.web;

import ch.rail.r2p.api.dto.Activity;
import ch.rail.r2p.api.dto.BrakeType;
import ch.rail.r2p.api.dto.Capability;
import ch.rail.r2p.api.dto.CapabilityFeature;
import ch.rail.r2p.api.dto.CapabilityType;
import ch.rail.r2p.api.dto.Feature;
import ch.rail.r2p.api.dto.Focus;
import ch.rail.r2p.api.dto.PartnerToken;
import ch.rail.r2p.api.dto.Path;
import ch.rail.r2p.api.dto.PathActivityType;
import ch.rail.r2p.api.dto.RollingStock;
import ch.rail.r2p.api.dto.RollingStockActivityType;
import ch.rail.r2p.api.dto.RouteClassType;
import ch.rail.r2p.api.dto.Unit;
import ch.rail.r2p.api.dto.Journey;
import ch.rail.r2p.api.dto.Journeylocation;
import ch.rail.r2p.api.dto.Link;
import ch.rail.r2p.api.dto.Location;
import ch.rail.r2p.api.dto.LocationType;
import ch.rail.r2p.api.dto.PlanStatus;
import ch.rail.r2p.api.dto.Reference;
import ch.rail.r2p.api.dto.Relation;
import ch.rail.r2p.api.dto.Responsibility;
import ch.rail.r2p.api.dto.ResponsibilityType;
import ch.rail.r2p.api.dto.Staff;
import ch.rail.r2p.api.dto.StaffActivityType;
import ch.rail.r2p.api.dto.StaffType;
import ch.rail.r2p.api.dto.Timing;
import ch.rail.r2p.api.dto.TimingType;
import ch.rail.r2p.api.dto.Train;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import lombok.Getter;

public final class TestDataGenerator {

    private TestDataGenerator() {
    }

    public static LocalDate createOperationDay(@NotNull Random random, @NotNull LocalDate dateFrom, @NotNull LocalDate dateTo) {
        long rangeDays = dateFrom.toEpochDay();
        long rangeEnd = dateTo.toEpochDay();
        long rangeLength = rangeEnd - rangeDays + 1;
        if (rangeLength <= 0) {
            throw new IllegalArgumentException("dateTo must be on or after dateFrom");
        }
        return LocalDate.ofEpochDay(rangeDays + (long) (random.nextDouble() * rangeLength));
    }

    /** Total number of distinct train numbers available per operational day (1000–9999). */
    static final int TRAIN_NUMBERS_PER_DAY = 9000;

    public static String createUniqueTrainNumber(@NotNull Random random, @NotNull LocalDate operationalDay,
                                                  @NotNull Map<LocalDate, Set<String>> usedTrainNumbers) {
        Set<String> usedForDay = usedTrainNumbers.computeIfAbsent(operationalDay, _ -> new HashSet<>());
        if (usedForDay.size() >= TRAIN_NUMBERS_PER_DAY) {
            throw new IllegalStateException(
                "All " + TRAIN_NUMBERS_PER_DAY + " train numbers (1000–9999) have been exhausted for operational day " + operationalDay);
        }
        String trainNumber;
        do {
            trainNumber = String.valueOf((1000 + random.nextInt(TRAIN_NUMBERS_PER_DAY)));
        } while (!usedForDay.add(trainNumber));
        return trainNumber;
    }

    public static Train createTrain(@NotNull Random random, LocalDate operationalDay, String trainNumber) {
        return RandomTrain.of(random, operationalDay, trainNumber);
    }

    public static Train createTrain(LocalDate operationalDay, String trainNumber) {
        return createTrain(new Random(), operationalDay, trainNumber);
    }

    private static int randomInt(Random random, int min, int max) {
        return min + random.nextInt(max-min+1);
    }

    private static char randomCapitalLetter(Random random) {
        return (char) ('A' + random.nextInt(26));
    }

    private static String randomStringOfCapitalLetters(Random random, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(randomCapitalLetter(random));
        }
        return sb.toString();
    }

    static class RandomTrain extends Train {

        private RandomTrain(String id, List<@Valid Reference> references, List<@Valid Link> links, String name, LocalDate startdate, OffsetDateTime timestamp, Journey journey) {
            super(id, references, links, name, timestamp, journey);
            setStartdate(startdate);
        }
        
        static RandomTrain of(Random random, LocalDate operationalDay, String trainNumber) {
            // references and links are not yet implemented and are therefore omitted here.
            return new RandomTrain(trainNumber, Collections.emptyList(), Collections.emptyList(), randomName(random), operationalDay, OffsetDateTime.now(), RandomJourney.of(random));
        }

        private static String randomName(Random random) {
            int letterCount = TestDataGenerator.randomInt(random, 2, 3);
            StringBuilder sb = new StringBuilder();
            sb.append(randomStringOfCapitalLetters(random, letterCount));
            sb.append(' ');
            int digitCount = TestDataGenerator.randomInt(random, 1, 3);
            for (int i = 0; i < digitCount; i++) {
                sb.append(random.nextInt(10));
            }
            return sb.toString();
        }

    }

    static class RandomJourney extends Journey {

        private RandomJourney(List<@Valid Journeylocation> journeylocations) {
            super(journeylocations);
        }

        static RandomJourney of(Random random) {
            return new RandomJourney(RandomJourneylocation.listOf(random));
        }

    }

    static class RandomJourneylocation extends Journeylocation {

        private RandomJourneylocation(List<@Valid Location> locations, List<@Valid Activity> activities) {
            super(locations, activities);
        }

        static List<Journeylocation> listOf(Random random) {
            var route = Route.values()[random.nextInt(Route.values().length)];
            List<Journeylocation> locations = new ArrayList<>();
            // offsetMinutes[0] holds the cumulative offset in minutes from midnight of the operational day.
            // It is advanced by RandomTiming.listOf() for each stop.
            int[] offsetMinutes = {0};
            for (int i = 0; i < route.getStops().size(); i++) {
                Stop stop = route.getStops().get(i);
                List<Location> location = RandomLocation.listOf(stop);
                locations.add(new RandomJourneylocation(location, RandomActivity.listOf(random, stop, i, route.getStops().size(), offsetMinutes)));
            }
            return locations;
        }

    }

    static class RandomLocation extends Location {

        private RandomLocation(LocationType type, String code, String name) {
            super(type, code, name);
        }

        static List<Location> listOf(Stop stop) {
            return List.of(new RandomLocation(LocationType.UIC, stop.getUicCode(), stop.getName()));
        }
    }

    static class RandomActivity extends Activity {

        private RandomActivity(String id, List<@Valid Timing> timings, Responsibility responsibility, Capability capability, List<@Valid Relation> relations) {
            super(id, timings, responsibility, relations);
            setCapability(capability);
        }

        static List<Activity> listOf(Random random, Stop stop, int stopPosition, int routeLength, int[] offsetMinutes) {
            // At the first stop: create a path, a rolling stock, and a staff entry; only a departure timing is recorded.
            // At intermediate stops: randomly create one of staff, path, or rolling stock; both arrival and departure timings are recorded.
            // At the last stop: only a staff entry is created; only an arrival timing is recorded.
            List<RandomCapability> capabilities = new ArrayList<>();
            if (stopPosition == 0) {
                capabilities.add(RandomCapability.path(random));
                capabilities.add(RandomCapability.rollingStock(random));
                capabilities.add(RandomCapability.staff(random));
            } else if (stopPosition == routeLength - 1) {
                capabilities.add(RandomCapability.staff(random));
            } else {
                capabilities.add(RandomCapability.of(random));
            }

            // Timings are computed once per stop and shared by path/staff activities.
            // RollingStock activities carry no timing of their own.
            List<Timing> timings = RandomTiming.listOf(random, stopPosition, routeLength, offsetMinutes);

            List<Activity> activities = new ArrayList<>();
            for (int i = 0; i < capabilities.size(); i++) {
                RandomCapability cap = capabilities.get(i);
                List<Timing> activityTimings = cap.getType() == CapabilityType.ROLLING_STOCK
                        ? Collections.emptyList() : timings;
                activities.add(of(random, stop, stopPosition, cap, activityTimings, i));
            }
            return activities;
        }

        private static RandomActivity of(Random random, Stop stop, int stopPosition, RandomCapability capability, List<Timing> timings, int activityIndex) {
            return new RandomActivity(
                generateId(stop, capability.getType(), stopPosition, activityIndex),
                timings,
                RandomResponsibility.of(random, capability.getType()),
                capability,
                Collections.emptyList()
            );
        }

        private static String generateId(Stop stop, CapabilityType capabilityType, int stopPosition, int activityIndex) {
            return stop.name() + "-" + stopPosition + "-" + capabilityType.getValue() + "-" + activityIndex;
        }
    }

    static class RandomTiming extends Timing {

        private RandomTiming(TimingType type, String duration, Focus focus) {
            super(type, duration, focus);
        }

        static List<Timing> listOf(Random random, int stopPosition, int routeLength, int[] offsetMinutes) {
            // Each timing duration is an absolute offset from midnight of the operational day (ISO 8601).
            // offsetMinutes[0] is advanced per stop and shared across all stops by the caller (RandomJourneylocation).
            List<Timing> timings = new ArrayList<>();
            if (stopPosition == 0) {
                timings.add(firstDeparture(random, offsetMinutes));
            } else if (stopPosition == routeLength - 1) {
                timings.add(arrival(random, offsetMinutes));
            } else {
                timings.add(arrival(random, offsetMinutes));
                timings.add(departure(random, offsetMinutes));
            }
            return timings;
        }

        private static RandomTiming firstDeparture(Random random, int[] offsetMinutes) {
            int hours = TestDataGenerator.randomInt(random, 7, 10);
            int minutes = TestDataGenerator.randomInt(random, 0, 30);
            offsetMinutes[0] = hours * 60 + minutes;
            return of(random, TimingType.DEPARTURE, offsetMinutes[0]);
        }

        private static RandomTiming departure(Random random, int[] offsetMinutes) {
            int dwell = TestDataGenerator.randomInt(random, 1, 5);
            offsetMinutes[0] += dwell;
            return of(random, TimingType.DEPARTURE, offsetMinutes[0]);
        }

        private static RandomTiming arrival(Random random, int[] offsetMinutes) {
            int travel = TestDataGenerator.randomInt(random, 20, 50);
            offsetMinutes[0] += travel;
            return of(random, TimingType.ARRIVAL, offsetMinutes[0]);
        }

        private static RandomTiming of(Random random, TimingType type, int totalMinutes) {
            Focus focus = Focus.values()[random.nextInt(Focus.values().length)];
            int h = totalMinutes / 60;
            int m = totalMinutes % 60;
            String duration = (h > 0) ? "PT" + h + "H" + m + "M" : "PT" + m + "M";
            var timing = new RandomTiming(type, duration, focus);
            timing.setSource("generated");
            return timing;
        }

    }

    static class RandomResponsibility extends Responsibility {

        private RandomResponsibility(ResponsibilityType type, String id, PlanStatus status) {
            super(type, id, status);
        }

        static RandomResponsibility of(Random random, CapabilityType capabilityType) {
            ResponsibilityType type = responsibilityTypeFor(random, capabilityType);
            PartnerToken partner = PartnerToken.values()[random.nextInt(PartnerToken.values().length)];
            PlanStatus status = PlanStatus.values()[random.nextInt(PlanStatus.values().length)];
            return new RandomResponsibility(type, partner.getValue(), status);
        }

        private static ResponsibilityType responsibilityTypeFor(Random random, CapabilityType capabilityType) {
            return switch (capabilityType) {
                case PATH -> random.nextBoolean() ? ResponsibilityType.PIM : ResponsibilityType.RIM;
                case ROLLING_STOCK -> random.nextBoolean() ? ResponsibilityType.TRU : ResponsibilityType.RRU;
                case STAFF -> ResponsibilityType.PRU;
                default -> throw new IllegalStateException("Unexpected capability type: " + capabilityType);
            };
        }
    }

    static class RandomCapability extends Capability {

        private static final CapabilityType[] ALLOWED_TYPES = {
                CapabilityType.STAFF, CapabilityType.ROLLING_STOCK, CapabilityType.PATH
        };

        private RandomCapability(CapabilityType type, List<@Valid CapabilityFeature> capabilityFeatures) {
            super(type, capabilityFeatures);
        }

        static RandomCapability of(Random random) {
            CapabilityType type = ALLOWED_TYPES[random.nextInt(ALLOWED_TYPES.length)];
            return switch (type) {
                case STAFF -> staff(random);
                case ROLLING_STOCK -> rollingStock(random);
                case PATH -> path(random);
                default -> throw new IllegalStateException("Unexpected value: " + type);
            };
        }
        static RandomCapability staff(Random random) {
            var capability = empty(CapabilityType.STAFF);
            capability.staff(RandomStaff.of(random));
            return capability;
        }

        static RandomCapability rollingStock(Random random) {
            var capability = empty(CapabilityType.ROLLING_STOCK);
            capability.rollingStock(RandomRollingStock.of(random));
            return capability;
        }

        static RandomCapability path(Random random) {
            var capability = empty(CapabilityType.PATH);
            capability.path(RandomPath.of(random));
            return capability;
        }

        private static RandomCapability empty(CapabilityType type) {
            // CapabilityFeatures are not yet created
            return new RandomCapability(type, Collections.emptyList());
        }

    }

    static class RandomStaff extends Staff {

        private RandomStaff(StaffType type, StaffActivityType activityType) {
            super(type, activityType);
        }

        static Staff of(Random random) {
            StaffType type = StaffType.values()[random.nextInt(StaffType.values().length)];
            return new RandomStaff(type, StaffActivityType.INSPECT_TRAIN_OPERATIONAL);
        }
    }

    static class RandomRollingStock extends RollingStock {

        private RandomRollingStock(List<Unit> units) {
            super(RollingStockActivityType.ROLLING_STOCK, units);
        }

        static RandomRollingStock of(Random random) {
            int unitCount = TestDataGenerator.randomInt(random, 1, 3);
            List<Unit> units = new ArrayList<>();
            for (int i = 0; i < unitCount; i++) {
                units.add(RandomUnit.of(random, i + 1));
            }
            var rollingStock = new RandomRollingStock(units);
            // Aggregate length and weight are derived from units to satisfy the spec invariant
            int totalLength = units.stream().mapToInt(u -> u.getLength() != null ? u.getLength() : 0).sum();
            int totalWeight = units.stream().mapToInt(u -> u.getWeight() != null ? u.getWeight() : 0).sum();
            rollingStock.setLength(totalLength);
            rollingStock.setWeight(totalWeight);
            rollingStock.setMaxSpeed(TestDataGenerator.randomInt(random, 8, 20) * 10);
            rollingStock.setBrakeType(BrakeType.values()[random.nextInt(BrakeType.values().length)]);
            rollingStock.setBrakeWeight(TestDataGenerator.randomInt(random, 50000, 500000));
            rollingStock.setBrakingRatio(20.0f + random.nextFloat() * 80.0f);
            return rollingStock;
        }
    }

    static class RandomUnit extends Unit {

        private RandomUnit(String id, List<CapabilityFeature> capabilityFeatures) {
            super(id, Collections.emptyList(), capabilityFeatures, Collections.emptyList());
        }

        static RandomUnit of(Random random, int index) {
            var unit = new RandomUnit(String.valueOf(index), RandomCapabilityFeature.listForUnit(random));
            unit.setName(randomStringOfCapitalLetters(random, 4) + " " + TestDataGenerator.randomInt(random, 100, 999));
            unit.setLength(TestDataGenerator.randomInt(random, 10000, 200000));
            unit.setWeight(TestDataGenerator.randomInt(random, 30000, 800000));
            return unit;
        }
    }

    static class RandomCapabilityFeature {

        // Boolean CUSTOMER_SERVICE features for rolling stock units
        private static final String[] CUSTOMER_SERVICE_FEATURES = {
            "FEAT_CS_WV",  // WLAN Hotspot
            "FEAT_CS_WR",  // Restaurant
            "FEAT_CS_RZ",  // Ruhezone 1. Klasse
            "FEAT_CS_NF",  // Niederflureinstieg
            "FEAT_CS_TT",  // Neigezug
        };

        static List<CapabilityFeature> listForUnit(Random random) {
            List<CapabilityFeature> features = new ArrayList<>();

            // 2–4 random Boolean customer service features
            List<String> pool = new ArrayList<>(List.of(CUSTOMER_SERVICE_FEATURES));
            Collections.shuffle(pool, random);
            int count = TestDataGenerator.randomInt(random, 2, 4);
            for (int i = 0; i < count; i++) {
                features.add(booleanFeature(pool.get(i), random.nextBoolean()));
            }

            long vehicleTypeId = 100_000_000_000L + Long.remainderUnsigned(random.nextLong(), 900_000_000_000L);
            PartnerToken ru = PartnerToken.values()[random.nextInt(PartnerToken.values().length)];
            features.add(stringFeature("VC_FOS_VehicleType_ru", ru.getValue()));

            return features;
        }

        private static CapabilityFeature booleanFeature(String type, boolean value) {
            return new CapabilityFeature().feature(new Feature().type(type).offeredservice(String.valueOf(value)));
        }

        private static CapabilityFeature stringFeature(String type, String value) {
            return new CapabilityFeature().feature(new Feature().type(type).offeredservice(value));
        }
    }

    static class RandomPath extends Path {

        private RandomPath(String id, Integer length, Integer weight, Integer maxSpeed) {
            super(id, PathActivityType.PATH_RUNTHROUGH, length, weight, maxSpeed);
        }

        private static final String[] TRAIN_CATEGORIES = {"00", "01", "02", "03", "04", "05", "06"};

        static RandomPath of(Random random) {
            String id = TestDataGenerator.randomInt(random, 10000, 99999) + "-" + String.format("%03d", TestDataGenerator.randomInt(random, 1, 999));
            int length = TestDataGenerator.randomInt(random, 50000, 500000);
            int weight = TestDataGenerator.randomInt(random, 100000, 2000000);
            int maxSpeed = TestDataGenerator.randomInt(random, 8, 20) * 10;
            var path = new RandomPath(id, length, weight, maxSpeed);
            path.setTrainCategory(TRAIN_CATEGORIES[random.nextInt(TRAIN_CATEGORIES.length)]);
            path.setRouteClass(RouteClassType.values()[random.nextInt(RouteClassType.values().length)]);
            return path;
        }
    }

    @Getter
    enum Route {
        BASEL_OLTEN(List.of(Stop.BASEL_SBB, Stop.OLTEN)),
        BERN_OLTEN(List.of(Stop.BERN, Stop.OLTEN)),
        ZUERICH_OLTEN(List.of(Stop.ZUERICH_HB, Stop.OLTEN)),
        BASEL_ZUERICH(List.of(Stop.BASEL_SBB, Stop.OLTEN, Stop.ZUERICH_HB)),
        BERN_ZUERICH(List.of(Stop.BERN, Stop.OLTEN, Stop.ZUERICH_HB)),
        ZUERICH_ROMANSHORN(List.of(Stop.ZUERICH_HB, Stop.WINTERTHUR, Stop.WEINFELDEN, Stop.ROMANSHORN)),
        BASEL_LUGANO(List.of(Stop.BASEL_SBB, Stop.OLTEN, Stop.LUZERN, Stop.ARTH_GOLDAU, Stop.BELLINZONA, Stop.LUGANO)),
        GENEVE_BERN(List.of(Stop.GENEVE, Stop.RENENS_VD, Stop.FRIBOURG_FREIBURG, Stop.BERN)),
        LAUSANNE_AARAU(List.of(Stop.LAUSANNE, Stop.RENENS_VD, Stop.YVERDON_LES_BAINS, Stop.NEUCHATEL, Stop.BIEL_BIENNE, Stop.SOLOTHURN, Stop.OLTEN, Stop.AARAU)),
        ;

        private final List<Stop> stops;

        Route(List<Stop> stops) {
            this.stops = stops;
        }
    }

    @Getter
    enum Stop {
        AARAU("8502113", "Aarau"),
        ARTH_GOLDAU("8505004", "Arth-Goldau"),
        BASEL_SBB("8500010", "Basel SBB"),
        BELLINZONA("8505213", "Bellinzona"),
        BERN("8507000", "Bern"),
        BIEL_BIENNE("8504300", "Biel/Bienne"),
        FRIBOURG_FREIBURG("8504100", "Fribourg/Freiburg"),
        GENEVE("8501008", "Genève"),
        LAUSANNE("8501120", "Lausanne"),
        LUGANO("8505300", "Lugano"),
        LUZERN("8505000", "Luzern"),
        NEUCHATEL("8504221", "Neuchâtel"),
        OLTEN("8500218", "Olten"),
        ROMANSHORN("8506121", "Romanshorn"),
        RENENS_VD("8501118", "Renens VD"),
        SOLOTHURN("8500207", "Solothurn"),
        WEINFELDEN("8506105", "Weinfelden"),
        WINTERTHUR("8506000", "Winterthur"),
        YVERDON_LES_BAINS("8504200", "Yverdon-les-Bains"),
        ZUERICH_HB("8503000", "Zürich HB");

        private final String uicCode;
        private final String name;

        Stop(String uicCode, String name) {
            this.uicCode = uicCode;
            this.name = name;
        }
    }

}
