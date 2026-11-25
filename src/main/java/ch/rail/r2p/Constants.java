package ch.rail.r2p;

import java.time.ZoneId;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Constants {

    public static final ZoneId ZURICH_ZONE_ID = ZoneId.of("Europe/Zurich");

}
