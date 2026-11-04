package com.mowtiie.keyheimer.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.mowtiie.keyheimer.data.Secret;

import org.junit.Test;

import java.util.Calendar;

public class IntervalConverterTest {

    private static long at(int year, int month, int day, int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month, day, hour, minute, second);
        return calendar.getTimeInMillis();
    }

    private static Calendar calendarOf(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return calendar;
    }

    @Test
    public void day_stampsReminderTimeAndAddsDays() {
        long from = at(2026, Calendar.MARCH, 10, 15, 45, 30);

        Calendar result = calendarOf(IntervalConverter.computeNextTriggerFrom(
                from, 3, Secret.IntervalUnit.DAY, 9, 5));

        assertEquals(2026, result.get(Calendar.YEAR));
        assertEquals(Calendar.MARCH, result.get(Calendar.MONTH));
        assertEquals(13, result.get(Calendar.DAY_OF_MONTH));
        assertEquals(9, result.get(Calendar.HOUR_OF_DAY));
        assertEquals(5, result.get(Calendar.MINUTE));
        assertEquals(0, result.get(Calendar.SECOND));
        assertEquals(0, result.get(Calendar.MILLISECOND));
    }

    @Test
    public void week_addsSevenDaysPerUnit() {
        long from = at(2026, Calendar.MARCH, 10, 8, 0, 0);

        Calendar result = calendarOf(IntervalConverter.computeNextTriggerFrom(
                from, 2, Secret.IntervalUnit.WEEK, 8, 0));

        assertEquals(Calendar.MARCH, result.get(Calendar.MONTH));
        assertEquals(24, result.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void month_clampsToEndOfShorterMonth() {
        long from = at(2026, Calendar.JANUARY, 31, 8, 0, 0);

        Calendar result = calendarOf(IntervalConverter.computeNextTriggerFrom(
                from, 1, Secret.IntervalUnit.MONTH, 8, 0));

        assertEquals(Calendar.FEBRUARY, result.get(Calendar.MONTH));
        assertEquals(28, result.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void oversizedInterval_isClampedToTheMaximum() {
        long from = at(2026, Calendar.MARCH, 10, 8, 0, 0);

        long clamped = IntervalConverter.computeNextTriggerFrom(
                from, Integer.MAX_VALUE, Secret.IntervalUnit.DAY, 9, 0);
        long atMaximum = IntervalConverter.computeNextTriggerFrom(
                from, IntervalConverter.maxIntervalValue(Secret.IntervalUnit.DAY), Secret.IntervalUnit.DAY, 9, 0);

        assertEquals(atMaximum, clamped);
        assertTrue(clamped > from);
    }

    @Test
    public void nonPositiveInterval_isTreatedAsOne() {
        long from = at(2026, Calendar.MARCH, 10, 8, 0, 0);

        long expected = IntervalConverter.computeNextTriggerFrom(from, 1, Secret.IntervalUnit.WEEK, 9, 0);

        assertEquals(expected, IntervalConverter.computeNextTriggerFrom(from, 0, Secret.IntervalUnit.WEEK, 9, 0));
        assertEquals(expected, IntervalConverter.computeNextTriggerFrom(from, -5, Secret.IntervalUnit.WEEK, 9, 0));
    }

    @Test
    public void isValidInterval_enforcesBoundsPerUnit() {
        for (Secret.IntervalUnit unit : Secret.IntervalUnit.values()) {
            int max = IntervalConverter.maxIntervalValue(unit);
            assertFalse(IntervalConverter.isValidInterval(0, unit));
            assertFalse(IntervalConverter.isValidInterval(-1, unit));
            assertTrue(IntervalConverter.isValidInterval(1, unit));
            assertTrue(IntervalConverter.isValidInterval(max, unit));
            assertFalse(IntervalConverter.isValidInterval(max + 1, unit));
        }
    }

    @Test
    public void clampInterval_pullsValuesIntoTheValidRange() {
        for (Secret.IntervalUnit unit : Secret.IntervalUnit.values()) {
            int max = IntervalConverter.maxIntervalValue(unit);
            assertEquals(1, IntervalConverter.clampInterval(Integer.MIN_VALUE, unit));
            assertEquals(1, IntervalConverter.clampInterval(0, unit));
            assertEquals(Math.min(5, max), IntervalConverter.clampInterval(5, unit));
            assertEquals(max, IntervalConverter.clampInterval(Integer.MAX_VALUE, unit));
        }
    }
}
