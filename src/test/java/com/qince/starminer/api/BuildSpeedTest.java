package com.qince.starminer.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers the build speed presets.
 *
 * <p>The original 1.7.10 mod had a single hard-coded rate of one block per 40 ticks, which makes even a
 * small star take minutes. {@link BuildSpeed#SLOW} must keep that exact rate so the fastest and slowest
 * settings bracket the original behaviour.
 */
class BuildSpeedTest {

    @Test
    @DisplayName("the slow preset reproduces the original 1.7.10 rate exactly")
    void slowMatchesTheOriginal() {
        assertEquals(40, BuildSpeed.SLOW.cycleTicks(), "the original used a 40 tick work cycle");
        assertEquals(1, BuildSpeed.SLOW.blocksPerCycle(), "the original placed one block per cycle");
        assertEquals(0.5D, BuildSpeed.SLOW.blocksPerSecond(), 1.0E-9D);
    }

    @Test
    @DisplayName("each preset is faster than the one before it")
    void presetsAreOrderedBySpeed() {
        for (int i = 1; i < BuildSpeed.VALUES.length; i++) {
            double previous = BuildSpeed.VALUES[i - 1].blocksPerSecond();
            double current = BuildSpeed.VALUES[i].blocksPerSecond();
            assertTrue(current > previous,
                    BuildSpeed.VALUES[i] + " (" + current + "/s) must be faster than "
                            + BuildSpeed.VALUES[i - 1] + " (" + previous + "/s)");
        }
    }

    @Test
    @DisplayName("the default preset is a usable speed rather than the original crawl")
    void defaultIsUsable() {
        assertSame(BuildSpeed.NORMAL, BuildSpeed.byIndex(BuildSpeed.NORMAL.ordinal()));
        assertTrue(BuildSpeed.NORMAL.blocksPerSecond() >= 4.0D,
                "the default should build at least four blocks a second");
    }

    @Test
    @DisplayName("cycling wraps around and survives an out of range index")
    void cyclingAndLookupAreSafe() {
        BuildSpeed last = BuildSpeed.VALUES[BuildSpeed.VALUES.length - 1];
        assertSame(BuildSpeed.VALUES[0], last.next(), "cycling past the end wraps to the start");
        for (BuildSpeed speed : BuildSpeed.VALUES) {
            assertSame(speed, speed.next().next().next(), "three steps return to the same preset");
        }
        assertSame(BuildSpeed.VALUES[0], BuildSpeed.byIndex(-5));
        assertSame(BuildSpeed.VALUES[BuildSpeed.VALUES.length - 1], BuildSpeed.byIndex(999));
    }

    @Test
    @DisplayName("presets round trip through their persisted id")
    void idsRoundTrip() {
        for (BuildSpeed speed : BuildSpeed.VALUES) {
            assertSame(speed, BuildSpeed.byId(speed.id()), "id " + speed.id() + " must round trip");
        }
        assertSame(BuildSpeed.NORMAL, BuildSpeed.byId("not-a-speed"), "an unknown id falls back to the default");
    }
}
