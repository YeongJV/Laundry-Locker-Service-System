package util;

import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class CodeGenerator {
	private static final Random RND = new Random();

    public static String unique6Digits(Set<String> existing) {
        for (int i = 0; i < 1000; i++) {
            String code = String.format("%06d", RND.nextInt(1_000_000));
            if (!existing.contains(code)) return code;
        }
        return String.format("%06d", RND.nextInt(1_000_000));
    }

    public static String reservationId() {
        return "R-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}