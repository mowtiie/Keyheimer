package com.mowtiie.keyheimer.util;

import com.mowtiie.keyheimer.data.Secret;

import java.util.Calendar;

public final class IntervalConverter {

    private IntervalConverter() {
    }

    public static long computeNextTriggerAt(int intervalValue, Secret.IntervalUnit unit, int reminderHour, int reminderMinute) {
        return computeNextTriggerFrom(System.currentTimeMillis(), intervalValue, unit,
                reminderHour, reminderMinute);
    }

    static long computeNextTriggerFrom(long fromMillis, int intervalValue, Secret.IntervalUnit unit, int reminderHour, int reminderMinute) {
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