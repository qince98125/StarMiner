package com.qince.starminer.api;

import net.minecraft.util.Mth;

/**
 * How quickly a Gravity Core builds its star shell.
 *
 * <p>The 1.7.10 mod had a single hard-coded rate (one block per 40 ticks, i.e. one block per two
 * seconds), which makes even a modest star take several minutes. These presets keep the slow end
 * identical to the original while letting the player pick a faster build.
 */
public enum BuildSpeed {
    /** The original 1.7.10 rate: one block every two seconds. */
    SLOW("slow", 40, 1),
    /** Default: one work cycle per second, four blocks each. */
    NORMAL("normal", 20, 4),
    /** Large stars only: four cycles per second, sixteen blocks each. */
    FAST("fast", 5, 16);

    public static final BuildSpeed[] VALUES = values();

    private final String id;
    private final int cycleTicks;
    private final int blocksPerCycle;

    BuildSpeed(String id, int cycleTicks, int blocksPerCycle) {
        this.id = id;
        this.cycleTicks = cycleTicks;
        this.blocksPerCycle = blocksPerCycle;
    }

    /** Stable name used in NBT and translation keys. */
    public String id() {
        return this.id;
    }

    public int cycleTicks() {
        return this.cycleTicks;
    }

    public int blocksPerCycle() {
        return this.blocksPerCycle;
    }

    /** Rough blocks placed per second, used to explain the preset to the player. */
    public double blocksPerSecond() {
        return this.blocksPerCycle * 20.0D / this.cycleTicks;
    }

    public BuildSpeed next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public static BuildSpeed byIndex(int index) {
        return VALUES[Mth.clamp(index, 0, VALUES.length - 1)];
    }

    public static BuildSpeed byId(String id) {
        for (BuildSpeed speed : VALUES) {
            if (speed.id.equals(id)) {
                return speed;
            }
        }
        return NORMAL;
    }

    public String translationKey() {
        return "starInfo.speed." + this.id;
    }
}
