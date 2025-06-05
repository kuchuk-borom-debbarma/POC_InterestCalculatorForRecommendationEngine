package dev.kuku.interestcalculator.secondIteration.services;

import dev.kuku.interestcalculator.secondIteration.fakeDatabase.UserInteractionsDb;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserActivityCalculator {

    private final UserInteractionsDb userInteractionsDb;

    @Getter
    public enum ActivityType {
        POWER_USER(2.0),
        ACTIVE_USER(1.5),
        REGULAR_USER(1.0),
        CASUAL_USER(0.7),
        INACTIVE_USER(0.3);

        private final double multiplier;

        ActivityType(double multiplier) {
            this.multiplier = multiplier;
        }

    }

    public record ActivityMetrics(ActivityType activityType, int totalInteractions, double dailyAverage, int uniqueDays,
                                  double engagementScore) {
    }

    /**
     * Calculates user activity type based on interaction patterns
     *
     * @param userId User ID to analyze
     * @param from   Start timestamp (inclusive)
     * @param to     End timestamp (inclusive)
     * @return ActivityMetrics containing the user's activity classification
     */
    public ActivityMetrics calculateUserActivityType(String userId, long from, long to) {
        List<UserInteractionsDb.UserInteractionRow> interactions =
                userInteractionsDb.getInteractionsOfUserFromTo(userId, from, to);

        if (interactions.isEmpty()) {
            return new ActivityMetrics(ActivityType.INACTIVE_USER, 0, 0.0, 0, 0.0);
        }

        // Calculate basic metrics
        int totalInteractions = interactions.size();
        long timeRangeInDays = Math.max(1, (to - from) / (24 * 60 * 60 * 1000L));
        double dailyAverage = (double) totalInteractions / timeRangeInDays;

        // Calculate unique active days
        int uniqueDays = (int) interactions.stream()
                .map(interaction -> interaction.interactionTime / (24 * 60 * 60 * 1000L))
                .distinct()
                .count();

        // Calculate engagement score based on interaction types
        double engagementScore = calculateEngagementScore(interactions);

        // Determine activity type
        ActivityType activityType = classifyUserActivity(
                totalInteractions, dailyAverage, uniqueDays, timeRangeInDays, engagementScore
        );

        return new ActivityMetrics(activityType, totalInteractions, dailyAverage, uniqueDays, engagementScore);
    }

    /**
     * Calculate weighted engagement score based on interaction types
     */
    private double calculateEngagementScore(List<UserInteractionsDb.UserInteractionRow> interactions) {
        Map<UserInteractionsDb.InteractionType, Long> interactionCounts = interactions.stream()
                .collect(Collectors.groupingBy(
                        interaction -> interaction.interactionType,
                        Collectors.counting()
                ));

        double score = 0.0;
        // Weight different interaction types
        score += interactionCounts.getOrDefault(UserInteractionsDb.InteractionType.LIKE, 0L) * 1.0;
        score += interactionCounts.getOrDefault(UserInteractionsDb.InteractionType.DISLIKE, 0L) * 1.2; // Slightly higher as it shows engagement
        score += interactionCounts.getOrDefault(UserInteractionsDb.InteractionType.COMMENT, 0L) * 3.0; // High engagement
        score += interactionCounts.getOrDefault(UserInteractionsDb.InteractionType.REPORT, 0L) * 2.0; // Moderate engagement

        return score;
    }

    /**
     * Classify user based on various activity metrics
     */
    private ActivityType classifyUserActivity(int totalInteractions, double dailyAverage,
                                              int uniqueDays, long timeRangeInDays, double engagementScore) {

        // Power User: High daily average, high engagement, consistent activity
        if (dailyAverage >= 8.0 && engagementScore >= 50.0 && uniqueDays >= (timeRangeInDays * 0.6)) {
            return ActivityType.POWER_USER;
        }

        // Active User: Good daily activity and engagement
        if (dailyAverage >= 4.0 && engagementScore >= 20.0 && uniqueDays >= (timeRangeInDays * 0.4)) {
            return ActivityType.ACTIVE_USER;
        }

        // Regular User: Moderate activity
        if (dailyAverage >= 1.5 && totalInteractions >= 10 && uniqueDays >= (timeRangeInDays * 0.2)) {
            return ActivityType.REGULAR_USER;
        }

        // Casual User: Low but present activity
        if (totalInteractions >= 3 && uniqueDays >= 2) {
            return ActivityType.CASUAL_USER;
        }

        // Inactive User: Very low activity
        return ActivityType.INACTIVE_USER;
    }

    /**
     * Convenience method to get just the activity type
     */
    public ActivityType getUserActivityType(String userId, long from, long to) {
        return calculateUserActivityType(userId, from, to).activityType;
    }

    /**
     * Convenience method to get the multiplier for interest scoring
     */
    public double getActivityMultiplier(String userId, long from, long to) {
        return calculateUserActivityType(userId, from, to).activityType.getMultiplier();
    }

    // Helper methods for common time ranges
    private long getCurrentTimeMillis() {
        return Instant.now().toEpochMilli();
    }

    private long getDaysAgoMillis(int days) {
        return getCurrentTimeMillis() - (days * 24L * 60L * 60L * 1000L);
    }

    /**
     * Get activity metrics for the last N days from a specific point in time
     */
    public ActivityMetrics getLastNDaysActivity(String userId, long fromTimestamp, int days) {
        long from = fromTimestamp - (days * 24L * 60L * 60L * 1000L);
        return calculateUserActivityType(userId, from, fromTimestamp);
    }

    /**
     * Get activity metrics for the last N days from current time
     */
    public ActivityMetrics getLastNDaysActivityFromNow(String userId, int days) {
        long now = getCurrentTimeMillis();
        return getLastNDaysActivity(userId, now, days);
    }

    /**
     * Get daily activity (last 24 hours) from a specific timestamp
     */
    public ActivityMetrics getDailyActivity(String userId, long fromTimestamp) {
        return getLastNDaysActivity(userId, fromTimestamp, 1);
    }

    /**
     * Get daily activity (last 24 hours) from current time
     */
    public ActivityMetrics getDailyActivityFromNow(String userId) {
        return getLastNDaysActivityFromNow(userId, 1);
    }

    /**
     * Get weekly activity (last 7 days) from a specific timestamp
     */
    public ActivityMetrics getWeeklyActivity(String userId, long fromTimestamp) {
        return getLastNDaysActivity(userId, fromTimestamp, 7);
    }

    /**
     * Get weekly activity (last 7 days) from current time
     */
    public ActivityMetrics getWeeklyActivityFromNow(String userId) {
        return getLastNDaysActivityFromNow(userId, 7);
    }

    /**
     * Get monthly activity (last 30 days) from a specific timestamp
     */
    public ActivityMetrics getMonthlyActivity(String userId, long fromTimestamp) {
        return getLastNDaysActivity(userId, fromTimestamp, 30);
    }

    /**
     * Get monthly activity (last 30 days) from current time
     */
    public ActivityMetrics getMonthlyActivityFromNow(String userId) {
        return getLastNDaysActivityFromNow(userId, 30);
    }

    /**
     * Get 3-month activity (last 90 days) from a specific timestamp
     */
    public ActivityMetrics get3MonthActivity(String userId, long fromTimestamp) {
        return getLastNDaysActivity(userId, fromTimestamp, 90);
    }

    /**
     * Get 3-month activity (last 90 days) from current time
     */
    public ActivityMetrics get3MonthActivityFromNow(String userId) {
        return getLastNDaysActivityFromNow(userId, 90);
    }

    /**
     * Get 6-month activity (last 180 days) from a specific timestamp
     */
    public ActivityMetrics get6MonthActivity(String userId, long fromTimestamp) {
        return getLastNDaysActivity(userId, fromTimestamp, 180);
    }

    /**
     * Get 6-month activity (last 180 days) from current time
     */
    public ActivityMetrics get6MonthActivityFromNow(String userId) {
        return getLastNDaysActivityFromNow(userId, 180);
    }

    /**
     * Get 9-month activity (last 270 days) from a specific timestamp
     */
    public ActivityMetrics get9MonthActivity(String userId, long fromTimestamp) {
        return getLastNDaysActivity(userId, fromTimestamp, 270);
    }

    /**
     * Get 9-month activity (last 270 days) from current time
     */
    public ActivityMetrics get9MonthActivityFromNow(String userId) {
        return getLastNDaysActivityFromNow(userId, 270);
    }

    /**
     * Get yearly activity (last 365 days) from a specific timestamp
     */
    public ActivityMetrics getYearlyActivity(String userId, long fromTimestamp) {
        return getLastNDaysActivity(userId, fromTimestamp, 365);
    }

    /**
     * Get yearly activity (last 365 days) from current time
     */
    public ActivityMetrics getYearlyActivityFromNow(String userId) {
        return getLastNDaysActivityFromNow(userId, 365);
    }

    /**
     * Get activity for a custom date range
     *
     * @param userId   User ID
     * @param fromDate Start date in "yyyy-MM-dd" format
     * @param toDate   End date in "yyyy-MM-dd" format
     */
    public ActivityMetrics getActivityForDateRange(String userId, String fromDate, String toDate) {
        try {
            java.time.LocalDate from = java.time.LocalDate.parse(fromDate);
            java.time.LocalDate to = java.time.LocalDate.parse(toDate);

            long fromMillis = from.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            long toMillis = to.atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

            return calculateUserActivityType(userId, fromMillis, toMillis);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format. Use yyyy-MM-dd format.", e);
        }
    }
}