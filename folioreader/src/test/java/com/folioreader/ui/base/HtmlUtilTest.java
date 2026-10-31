package com.folioreader.ui.base;

import static org.junit.Assert.assertEquals;


import org.junit.Test;

public class HtmlUtilTest {

    // Play vitals: NPE in getHtmlContent when a reload ran before the chapter HTML arrived.
    @Test
    public void nullHtmlReturnsEmptyInsteadOfCrashing() {
        assertEquals("", HtmlUtil.getHtmlContent(null, null, null));
    }
}
