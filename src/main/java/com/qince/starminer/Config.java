package com.qince.starminer;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config for StarMiner.
 *
 * <p>Values mirror the tunables the original 1.7.10 mod exposed (gravity radius cap, star radius cap,
 * attract/terraform tick rates).
 */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MAX_GRAVITY_RANGE = BUILDER
            .comment("Maximum gravity field radius a Gravity Core can be configured to (in blocks).")
            .defineInRange("maxGravityRange", 64, 1, 256);

    public static final ModConfigSpec.IntValue MAX_STAR_RADIUS = BUILDER
            .comment("Maximum star radius (the radius of the solid shell a Gravity Core builds).")
            .defineInRange("maxStarRadius", 48, 0, 192);

    public static final ModConfigSpec.IntValue MAX_TERRAFORM_STEP_PER_TICK = BUILDER
            .comment("How many blocks a Gravity Core may place per work cycle. Higher = faster terraforming.")
            .defineInRange("terraforming.blocksPerCycle", 1, 1, 64);

    public static final ModConfigSpec.IntValue TERRAFORM_CYCLE_TICKS = BUILDER
            .comment("Ticks between terraforming work cycles.")
            .defineInRange("terraforming.cycleTicks", 40, 1, 200);

    public static final ModConfigSpec.BooleanValue APPLY_GRAVITY_ATTRIBUTE = BUILDER
            .comment("Enable the player's six-direction gravity physics inside Gravity Core fields.")
            .define("gravity.applyAttribute", true);

    public static final ModConfigSpec.DoubleValue FLIPPED_GRAVITY_MULTIPLIER = BUILDER
            .comment("Gravity multiplier applied while the gravity direction is flipped (downTOup_YP).",
                    "Negative-looking values are unsigned: 1.0 restores vanilla strength.")
            .defineInRange("gravity.flippedMultiplier", 1.0D, 0.0D, 8.0D);

    public static final ModConfigSpec.DoubleValue ZERO_GRAVITY_MULTIPLIER = BUILDER
            .comment("Legacy setting retained for old configs. Sideways strength now uses gravity.fieldStrength.")
            .defineInRange("gravity.sidewaysMultiplier", 0.0D, 0.0D, 8.0D);

    public static final ModConfigSpec.DoubleValue FIELD_GRAVITY_STRENGTH = BUILDER
            .comment("Per-tick acceleration applied along a sideways gravity direction.",
                    "The vanilla value along Y is 0.08; the same value here makes a vertical wall feel like a floor.")
            .defineInRange("gravity.fieldStrength", 0.08D, 0.0D, 1.0D);

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
