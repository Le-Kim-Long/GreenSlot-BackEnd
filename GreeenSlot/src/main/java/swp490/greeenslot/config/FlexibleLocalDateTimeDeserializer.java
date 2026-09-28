package swp490.greeenslot.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Flexible deserializer for LocalDateTime that gracefully handles:
 * - UTC timestamps ending in 'Z' (e.g. 2026-09-10T23:25:24.947Z) -> converts to local zone
 * - Offset timestamps (e.g. 2026-09-11T06:25:24+07:00) -> converts to local zone
 * - Standard local ISO (e.g. 2026-09-11T00:00:00)
 * - Date-only strings (e.g. 2026-09-11) -> start of day
 */
public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String text = parser.getText();
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        text = text.trim();

        try {
            // Case 1: UTC ISO string ending with 'Z'
            if (text.endsWith("Z") || text.endsWith("z")) {
                Instant instant = Instant.parse(text);
                return LocalDateTime.ofInstant(instant, getTargetZone());
            }

            // Case 2: ISO string with timezone offset (e.g. +07:00, -0400)
            if (text.matches(".*[+-]\\d{2}:?\\d{2}$")) {
                OffsetDateTime offsetDateTime = OffsetDateTime.parse(text);
                return offsetDateTime.atZoneSameInstant(getTargetZone()).toLocalDateTime();
            }

            // Case 3: Date only (e.g. 2026-09-11)
            if (text.length() == 10 && text.indexOf('T') == -1) {
                return LocalDate.parse(text).atStartOfDay();
            }

            // Case 4: Standard local ISO string without timezone (e.g. 2026-09-11T00:00:00)
            return LocalDateTime.parse(text);
        } catch (Exception e) {
            // Fallback: try parsing as Instant then local
            try {
                Instant instant = Instant.parse(text);
                return LocalDateTime.ofInstant(instant, getTargetZone());
            } catch (Exception ex) {
                throw new IOException("Unable to parse date-time string: " + text, e);
            }
        }
    }

    private ZoneId getTargetZone() {
        try {
            return ZoneId.systemDefault();
        } catch (Exception e) {
            return DEFAULT_ZONE;
        }
    }
}
