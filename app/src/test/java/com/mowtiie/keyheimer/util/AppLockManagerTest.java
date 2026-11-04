package com.mowtiie.keyheimer.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AppLockManagerTest {

    @Test
    public void delayForFailures_isZeroForTheFirstFourAttempts() {
        for (int failures = 0; failures <= 4; failures++) {
            assertEquals(0L, AppLockManager.delayForFailures(failures));
        }
    }

    @Test
    public void delayForFailures_startsAtThirtySecondsAndDoubles() {
        assertEquals(30_000L, AppLockManager.delayForFailures(5));
        assertEquals(60_000L, AppLockManager.delayForFailures(6));
        assertEquals(120_000L, AppLockManager.delayForFailures(7));
        assertEquals(240_000L, AppLockManager.delayForFailures(8));
    }

    @Test
    public void delayForFailures_isCappedAtFifteenMinutes() {
        long cap = 15 * 60_000L;

        assertEquals(cap, AppLockManager.delayForFailures(10));
        assertEquals(cap, AppLockManager.delayForFailures(1_000));
        assertTrue(AppLockManager.delayForFailures(Integer.MAX_VALUE) <= cap);
    }
}
