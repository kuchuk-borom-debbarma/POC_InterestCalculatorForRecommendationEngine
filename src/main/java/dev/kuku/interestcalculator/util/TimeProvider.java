package dev.kuku.interestcalculator.util;

import java.time.Instant;

public interface TimeProvider {
    Instant now();

    default long nowMillis() {
        return now().toEpochMilli();
    }
}