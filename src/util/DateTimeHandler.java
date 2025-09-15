package util;

import java.time.Duration;
import java.time.LocalDateTime;

public class DateTimeHandler {
	public static long ceilHours(Duration d) {
        long minutes = d.toMinutes();
        return (minutes + 59) / 60;
    }
    public static Duration safeDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return null;
        return Duration.between(start, end);
    }
}

