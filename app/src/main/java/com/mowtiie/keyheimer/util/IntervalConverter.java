package com.mowtiie.keyheimer.util;

import com.mowtiie.keyheimer.data.Secret;

import java.util.Calendar;

public final class IntervalConverter {

    private static final int MAX_DAYS = 3650;
    private static final int MAX_WEEKS = 520;
    private static final int MAX_MONTHS = 120;

    private IntervalConverter() {
    }

    public static long computeNextTriggerAt(int intervalValue, Secret.IntervalUnit unit, int reminderHour, int reminderMinute) {
        return computeNextTriggerFrom(System.currentTimeMillis(), intervalValue, unit,
                reminderHour, reminderMinute);
    }

    public static long computeNextAfterFire(long scheduledMillis, long nowMillis, int intervalValue,
                                            Secret.IntervalUnit unit, int reminderHour, int reminderMinute) {
        long fromSchedule = computeNextTriggerFrom(scheduledMillis, intervalValue, unit, reminderHour, reminderMinute);
        if (fromSchedule > nowMillis) {
            return fromSchedule;
        }
        return computeNextTriggerFrom(nowMillis, intervalValue, unit, reminderHour, reminderMinute);
    }

    public static long alignToReminderTime(long triggerMillis, int reminderHour, int reminderMinute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(triggerMillis);
        calendar.set(Calendar.HOUR_OF_DAY, reminderHour);
        calendar.set(Calendar.MINUTE, reminderMinute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_YEAR, -1);

        long best = calendar.getTimeInMillis();
        for (int day = 0; day < 2; day++) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            long candidate = calendar.getTimeInMillis();
            if (Math.abs(candidate - triggerMillis) < Math.abs(best - triggerMillis)) {
                best = candidate;
            }
        }
        return best;
    }

    public static int maxIntervalValue(Secret.IntervalUnit unit) {
        switch (unit) {
            case DAY:
                return MAX_DAYS;
            case WEEK:
                return MAX_WEEKS;
            case MONTH:
                return MAX_MONTHS;
            default:
                throw new IllegalArgumentException("Unknown interval unit: " + unit);
        }
    }

    public static int clampInterval(int intervalValue, Secret.IntervalUnit unit) {
        return Math.max(1, Math.min(intervalValue, maxIntervalValue(unit)));
    }

    public static boolean isValidInterval(int intervalValue, Secret.IntervalUnit unit) {
        return intervalValue >= 1 && intervalValue <= maxIntervalValue(unit);
    }

    static long computeNextTriggerFrom(long fromMillis, int intervalValue, Secret.IntervalUnit unit, int reminderHour, int reminderMinute) {
        intervalValue = clampInterval(intervalValue, unit);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(fromMillis);
        calendar.set(Calendar.HOUR_OF_DAY, reminderHour);
        calendar.set(Calendar.MINUTE, reminderMinute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        switch (unit) {
            case DAY:
                calendar.add(Calendar.DAY_OF_YEAR, intervalValue);
                break;
            case WEEK:
                calendar.add(Calendar.WEEK_OF_YEAR, intervalValue);
                break;
            case MONTH:
                calendar.add(Calendar.MONTH, intervalValue);
                break;
            default:
                throw new IllegalArgumentException("Unknown interval unit: " + unit);
        }

        return calendar.getTimeInMillis();
    }
}