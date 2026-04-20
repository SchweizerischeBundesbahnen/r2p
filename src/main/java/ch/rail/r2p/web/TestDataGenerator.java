package ch.rail.r2p.web;

import ch.rail.r2p.api.dto.Activity;
import ch.rail.r2p.api.dto.ActivityType;
import ch.rail.r2p.api.dto.BrakeType;
import ch.rail.r2p.api.dto.Capability;
import ch.rail.r2p.api.dto.CapabilityFeature;
import ch.rail.r2p.api.dto.CapabilityType;
import ch.rail.r2p.api.dto.Featureset;
import ch.rail.r2p.api.dto.Focus;
import ch.rail.r2p.api.dto.Path;
import ch.rail.r2p.api.dto.RollingStock;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import lombok.Getter;

public final class TestDataGenerator {

    private static final Map<LocalDate, Set<String>> usedTrainNumbers = new HashMap<>();

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

    public static String createUniqueTrainNumber(@NotNull Random random, @NotNull LocalDate operationalDay) {
        Set<String> usedForDay = usedTrainNumbers.computeIfAbsent(operationalDay, _ -> new HashSet<>());
        String trainNumber;
        do {
            trainNumber = String.valueOf((1000 + random.nextInt(9000)));
        } while (!usedForDay.add(trainNumber));
        return trainNumber;
    }

    public static Train createTrain(LocalDate operationalDay, String trainNumber) {
        return RandomTrain.of(new Random(), operationalDay, trainNumber);
    }

    private static int randomInt(Random random, int min, int max) {
        return min + random.nextInt(max-min+1);
    }

    private static int randomCapitalLetter(Random random) {
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
            // references und links werden später implementiert und daher hier nicht berücksichtigt.
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

        private RandomJourney(List<@Valid Featureset> featuresets, List<@Valid Journeylocation> journeylocations) {
            super(featuresets, journeylocations);
        }

        static RandomJourney of(Random random) {
            // featuresets werden später implementiert und daher hier nicht berücksichtigt.
            return new RandomJourney(Collections.emptyList(), RandomJourneylocation.listOf(random));
        }

    }

    static class RandomJourneylocation extends Journeylocation {

        private RandomJourneylocation(List<@Valid Location> locations, List<@Valid Activity> activities) {
            super(locations, activities);
        }

        static List<Journeylocation> listOf(Random random) {
            var strecke = Strecken.values()[random.nextInt(Strecken.values().length)];
            List<Journeylocation> locations = new ArrayList<>();
            for (int i = 0; i < strecke.getHaltepunkte().size(); i++) {
                Halt halt = strecke.getHaltepunkte().get(i);
                List<Location> location = RandomLocation.listOf(halt);
                locations.add(new RandomJourneylocation(location, RandomActivity.listOf(random, halt, i, strecke.getHaltepunkte().size())));
            }
            return locations;
        }

    }

    static class RandomLocation extends Location {

        private RandomLocation(LocationType type, String code, String name) {
            super(type, code, name);
        }

        static List<Location> listOf(Halt halt) {
            return List.of(new RandomLocation(LocationType.UIC, halt.getDidokCode(), halt.getName()));
        }
    }

    static class RandomActivity extends Activity {

        private RandomActivity(String id, ActivityType type, List<@Valid Timing> timings, Responsibility responsibility, Capability capability, List<@Valid Relation> relations) {
            super(id, type, timings, responsibility, relations);
            setCapability(capability);
        }

        static List<Activity> listOf(Random random, Halt halt, int haltestellePosition, int laengeDerStrecke) {
            /*
            beim ersten punkt wird ein path, ein rolling stock und ein staff erstellt. bei allen wird nur ein departure timing angegeben.
            bei allen punkten dazwischen wird zufällig ein staff, path oder rolling stock generiert. es wird sowohl die arrival, als auch die departure time angegeben.
            beim letzten punkt wird nur ein staff generiert und nur ein arrival timing angegeben.
             */
            List<RandomCapability> capabilities = new ArrayList<>();
            if (haltestellePosition == 0) {
                capabilities.add(RandomCapability.path(random));
                capabilities.add(RandomCapability.rollingStock(random));
                capabilities.add(RandomCapability.staff(random));
            } else if (haltestellePosition == laengeDerStrecke - 1) {
                capabilities.add(RandomCapability.staff(random));
            } else {
                capabilities.add(RandomCapability.of(random));
            }

            List<Activity> activities = new ArrayList<>();
            for (RandomCapability capability : capabilities) {
                activities.add(of(random, halt, haltestellePosition, laengeDerStrecke, capability));
            }
            return activities;
        }

        private static RandomActivity of(Random random, Halt halt, int haltestellePosition, int laengeDerStrecke, RandomCapability capability) {
            return new RandomActivity(
                generateId(halt, capability.getType()),
                // ActivityType wird voraussichtlich entfernt
                null,
                RandomTiming.listOf(random, haltestellePosition, laengeDerStrecke),
                RandomResponsibility.of(random),
                capability,
                Collections.emptyList()
            );
        }

        private static String generateId(Halt halt, CapabilityType capabilityType) {
            // TODO eindeutig machen
            return halt.name + " " + capabilityType.getValue();
        }
    }

    static class RandomTiming extends Timing {

        private RandomTiming(TimingType type, String duration, Focus focus) {
            super(type, duration, focus);
        }

        static List<Timing> listOf(Random random, int haltestellePosition, int laengeDerStrecke) {
            /*
            TODO diese Zeiten sollten eigentlich die differenz zum betriebstag des zuges darstellen.
            aktuell werden die Zeiten so generiert, als würden sie sich auf die fachlich vorherige zeit beziehen.
            bspw. eine departure zeit ist nur eine kurze differenz von ein paar minuten.
             */
            List<Timing> timings = new ArrayList<>();
            if (haltestellePosition == 0) {
                timings.add(firstDeparture(random));
            } else if (haltestellePosition == laengeDerStrecke - 1) {
                timings.add(arrival(random));
            } else {
                timings.add(arrival(random));
                timings.add(departure(random));
            }
            return timings;
        }

        private static RandomTiming firstDeparture(Random random) {
            TimingType type = TimingType.DEPARTURE;
            int hours = TestDataGenerator.randomInt(random, 7, 10);
            int minutes = TestDataGenerator.randomInt(random, 0, 30);
            String duration = "PT" + hours + "H" + minutes + "M";
            return of(random, type, duration);
        }

        private static RandomTiming departure(Random random) {
            TimingType type = TimingType.DEPARTURE;
            int minutes = TestDataGenerator.randomInt(random, 1, 6);
            String duration = "PT" + minutes + "M";
            return of(random, type, duration);
        }

        private static RandomTiming arrival(Random random) {
            TimingType type = TimingType.ARRIVAL;
            int minutes = TestDataGenerator.randomInt(random, 20, 50);
            String duration = "PT" + minutes + "M";
            return of(random, type, duration);
        }

        private static RandomTiming of(Random random, TimingType type, String duration) {
            Focus focus = Focus.values()[random.nextInt(Focus.values().length)];
            return new RandomTiming(type, duration, focus);
        }

    }

    static class RandomResponsibility extends Responsibility {

        private RandomResponsibility(ResponsibilityType type, String id, PlanStatus status) {
            super(type, id, status);
        }

        static RandomResponsibility of(Random random) {
            // TODO im PoC wird bisher nur mit den werten SBB, BLS und SOB gearbeitet. muss das hier auch gemacht werden?
            ResponsibilityType type = ResponsibilityType.values()[random.nextInt(ResponsibilityType.values().length)];
            KnownResponsibilityType[] knownTypes = KnownResponsibilityType.values();
            String id = knownTypes[random.nextInt(knownTypes.length)].getOrganisationCode();
            PlanStatus status = PlanStatus.values()[random.nextInt(PlanStatus.values().length)];
            return new RandomResponsibility(type, id, status);
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
            capability.staffs(RandomStaff.listOf(random));
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
            // aktuell erstellen wir noch keine CapabilityFeatures
            return new RandomCapability(type, Collections.emptyList());
        }

    }

    static class RandomStaff extends Staff {

        private RandomStaff(StaffType type) {
            super(type);
        }

        static List<Staff> listOf(Random random) {
            StaffType type = StaffType.values()[random.nextInt(StaffType.values().length)];
            return List.of(new RandomStaff(type));
        }
    }

    static class RandomRollingStock extends RollingStock {

        private RandomRollingStock(List<Unit> units) {
            super(units);
        }

        static RandomRollingStock of(Random random) {
            int unitCount = TestDataGenerator.randomInt(random, 1, 3);
            List<Unit> units = new ArrayList<>();
            for (int i = 0; i < unitCount; i++) {
                units.add(RandomUnit.of(random, i + 1));
            }
            var rollingStock = new RandomRollingStock(units);
            rollingStock.setLength(TestDataGenerator.randomInt(random, 50000, 500000));
            rollingStock.setWeight(TestDataGenerator.randomInt(random, 100000, 2000000));
            rollingStock.setMaxSpeed(TestDataGenerator.randomInt(random, 80, 200));
            rollingStock.setBrakeType(BrakeType.values()[random.nextInt(BrakeType.values().length)]);
            rollingStock.setBrakeWeight(TestDataGenerator.randomInt(random, 50000, 500000));
            rollingStock.setBrakingRatio(20.0f + random.nextFloat() * 80.0f);
            return rollingStock;
        }
    }

    static class RandomUnit extends Unit {

        private RandomUnit(String id) {
            super(id, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }

        static RandomUnit of(Random random, int index) {
            var unit = new RandomUnit(String.valueOf(index));
            unit.setName(randomStringOfCapitalLetters(random, 4) + " " + TestDataGenerator.randomInt(random, 100, 999));
            unit.setLength(TestDataGenerator.randomInt(random, 10000, 200000));
            unit.setWeight(TestDataGenerator.randomInt(random, 30000, 800000));
            return unit;
        }
    }

    static class RandomPath extends Path {

        private RandomPath(String id, Integer length, Integer weight, Integer maxSpeed) {
            super(id, length, weight, maxSpeed);
        }

        static RandomPath of(Random random) {
            String id = TestDataGenerator.randomInt(random, 10000, 99999) + "-" + String.format("%03d", TestDataGenerator.randomInt(random, 1, 999));
            int length = TestDataGenerator.randomInt(random, 50000, 500000);
            int weight = TestDataGenerator.randomInt(random, 100000, 2000000);
            int maxSpeed = TestDataGenerator.randomInt(random, 80, 200);
            var path = new RandomPath(id, length, weight, maxSpeed);
            path.setTrainCategory(randomStringOfCapitalLetters(random, 1) + TestDataGenerator.randomInt(random, 10, 99));
            path.setRouteClass(RouteClassType.values()[random.nextInt(RouteClassType.values().length)]);
            return path;
        }
    }

    @Getter
    enum KnownResponsibilityType {
        BLS("1163"),
        BLS_CARGO("3356"),
        RHAETISCHE_BAHN("3267"),
        SBB_PERSONENVERKEHR("1385"),
        SBB_CARGO("2185"),
        SBB_INFRASTRUKTUR("5186"),
        SOB_TRANSPORT_EVU("5458")
        ;

        private final String organisationCode;

        KnownResponsibilityType(String organisationCode) {
            this.organisationCode = organisationCode;
        }
    }

    @Getter
    enum Strecken {
        BASEL_OLTEN(List.of(Halt.BASEL_SBB, Halt.OLTEN)),
        BERN_OLTEN(List.of(Halt.BERN, Halt.OLTEN)),
        ZUERICH_OLTEN(List.of(Halt.ZUERICH_HB, Halt.OLTEN)),
        BASEL_ZUERICH(List.of(Halt.BASEL_SBB, Halt.OLTEN, Halt.ZUERICH_HB)),
        BERN_ZUERICH(List.of(Halt.BERN, Halt.OLTEN, Halt.ZUERICH_HB)),
        ZUERICH_ROMANSHORN(List.of(Halt.ZUERICH_HB, Halt.WINTERTHUR, Halt.WEINFELDEN, Halt.ROMANSHORN)),
        BASEL_LUGANO(List.of(Halt.BASEL_SBB, Halt.OLTEN, Halt.LUZERN, Halt.ARTH_GOLDAU, Halt.BELLINZONA, Halt.LUGANO)),
        GENEVE_BERN(List.of(Halt.GENEVE, Halt.RENENS_VD, Halt.FRIBOURG_FREIBURG, Halt.BERN)),
        LAUSANNE_AARAU(List.of(Halt.LAUSANNE, Halt.RENENS_VD, Halt.YVERDON_LES_BAINS, Halt.NEUCHATEL, Halt.BIEL_BIENNE, Halt.SOLOTHURN, Halt.OLTEN, Halt.AARAU)),
        ;

        private final List<Halt> haltepunkte;

        Strecken(List<Halt> haltepunkte) {
            this.haltepunkte = haltepunkte;
        }
    }

    @Getter
    enum Halt {
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

        private final String didokCode;
        private final String name;

        Halt(String didokCode, String name) {
            this.didokCode = didokCode;
            this.name = name;
        }
    }

}
