package dev.kuku.interestcalculator.util;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Test class to set "current" time manually for testing purposes.
 */
@Component
@Profile("test")
public class TestTimeProvider implements TimeProvider {
    private Instant currentTime;

    public TestTimeProvider(Instant initialTime) {
        this.currentTime = initialTime;
    }

    public TestTimeProvider() {
        this(Instant.now());
    }

    @Override
    public Instant now() {
        return currentTime;
    }

    // Test control methods
    public void setTime(Instant time) {
        this.currentTime = time;
    }

    public void advanceDays(int days) {
        this.currentTime = currentTime.plusSeconds((long) days * 24 * 60 * 60);
    }

    public void advanceHours(int hours) {
        this.currentTime = currentTime.plusSeconds((long) hours * 60 * 60);
    }

    public void advanceMinutes(int minutes) {
        this.currentTime = currentTime.plusSeconds(minutes * 60L);
    }
}