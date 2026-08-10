package com.mowtiie.keyheimer.util;

import com.mowtiie.keyheimer.data.Secret;

import java.util.Calendar;

public final class IntervalConverter {

    private IntervalConverter() {
    }

    public static long computeNextTriggerAt(int intervalValue, Secret.IntervalUnit unit) {
        return computeNextTriggerFrom(System.currentTimeMillis(), intervalValue, unit);
    }

    static long computeNextTriggerFrom(long fromMillis, int intervalValue, Secret.IntervalUnit unit) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(fromMillis);

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