package com.qince.starminer.gravity;

import com.qince.starminer.api.GravityDirection;

/**
 * Turns "which way does gravity pull" into an {@code Attributes.GRAVITY} modifier amount.
 *
 * <p>Living entities read their gravity straight out of that attribute, whose default is
 * {@code 0.08} for players and which is an <em>acceleration</em>: for a body already falling,
 * {@code travel()} computes {@code dy = (dy - g) * 0.98}, so the value is subtracted. That is why
 * negative values pull upwards and why ordinary gravity needs no modifier at all.
 *
 * <p>Pure math, so it is unit tested directly.
 */
public final class GravityAttributeApplicator {

    /**
     * The amount to add to the entity's gravity attribute.
     *
     * @param direction          the direction gravity should pull
     * @param vanillaGravity     the entity's attribute base value, normally 0.08
     * @param flippedMultiplier  scaling for inverted gravity, 1.0 reproduces the original mod's strength
     * @param sidewaysMultiplier scaling for gravity that has no vertical component
     */
    public static double modifierAmount(GravityDirection direction, double vanillaGravity,
                                        double flippedMultiplier, double sidewaysMultiplier) {
        return switch (direction) {
            // Downward pull is already the default, so the modifier must not be present at all.
            case UP_TO_DOWN_Y_N -> 0.0D;
            // -g - g inverts the pull while keeping its magnitude.
            case DOWN_TO_UP_Y_P -> -2.0D * vanillaGravity * flippedMultiplier;
            case EAST_TO_WEST_X_N, WEST_TO_EAST_X_P, NORTH_TO_SOUTH_Z_P, SOUTH_TO_NORTH_Z_N ->
                    -vanillaGravity * sidewaysMultiplier;
        };
    }

    /** True when a modifier is needed at all for this direction. */
    public static boolean needsModifier(GravityDirection direction) {
        return direction != GravityDirection.UP_TO_DOWN_Y_N;
    }

    private GravityAttributeApplicator() {
    }
}
