package com.aritzia.availability.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SafeTextTest {

    @Test
    void leavesShortTextUnchanged() {
        assertThat(SafeText.of("abc123")).isEqualTo("abc123");
    }

    @Test
    void truncatesLongInput() {
        assertThat(SafeText.of("9".repeat(500))).isEqualTo("9".repeat(50) + "...");
    }

    @Test
    void replacesLineBreaksSoCallersCannotForgeLogLines() {
        assertThat(SafeText.of("123\nINFO fake entry")).isEqualTo("123_INFO fake entry");
    }

    @Test
    void handlesNull() {
        assertThat(SafeText.of(null)).isEqualTo("null");
    }
}
