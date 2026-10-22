package com.folioreader.mediaoverlay;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Regression: TTS onInit fired before the field was set (no engine) and crashed opening books. */
public class MediaControllerTest {
    @Test
    public void ttsNotUsableWhenNullOrFailed() {
        assertFalse(MediaController.isTtsUsable(null, 0));
        assertFalse(MediaController.isTtsUsable(new Object(), -1));
        assertTrue(MediaController.isTtsUsable(new Object(), 0));
    }
}
