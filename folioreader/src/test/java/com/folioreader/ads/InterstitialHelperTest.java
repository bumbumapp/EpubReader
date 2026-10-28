package com.folioreader.ads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class InterstitialHelperTest {
    private static final long MIN = 60_000L;
    private static final long T0 = 1_700_000_000_000L;

    @Test
    public void capConstants() {
        assertEquals(20 * MIN, InterstitialHelper.MIN_INTERVAL_MS);
        assertEquals(3 * MIN, InterstitialHelper.FIRST_GRACE_MS);
    }

    @Test
    public void noAdRightAfterInstall() {
        assertFalse(InterstitialHelper.capAllows(T0 + 3 * MIN - 1, 0L, T0));
        assertTrue(InterstitialHelper.capAllows(T0 + 3 * MIN, 0L, T0));
    }

    @Test
    public void gapBetweenAds() {
        long shown = T0 + 120 * MIN;
        assertFalse(InterstitialHelper.capAllows(shown + 20 * MIN - 1, shown, T0));
        assertTrue(InterstitialHelper.capAllows(shown + 20 * MIN, shown, T0));
    }
}
