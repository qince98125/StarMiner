package com.qince.starminer.gravity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qince.starminer.api.GravityDirection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies the gravity attribute math.
 *
 * <p>{@code Attributes.GRAVITY} defaults to 0.08 and is an acceleration: vanilla's {@code travel()}
 * computes {@code dy = (dy - g) * 0.98} for a falling body, so the value is subtracted. Ordinary gravity
 * therefore needs no modifier, and inverting the pull requires {@code -2g}.
 */
class GravityAttributeApplicatorTest {

    private static final double VANILLA = 0.08D;

    @Test
    @DisplayName("ordinary gravity adds nothing, because it is the attribute default")
    void ordinaryGravityNeedsNoModifier() {
        assertEquals(0.0D, GravityAttributeApplicator.modifierAmount(
                GravityDirection.UP_TO_DOWN_Y_N, VANILLA, 1.0D, 0.0D));
        assertFalse(GravityAttributeApplicator.needsModifier(GravityDirection.UP_TO_DOWN_Y_N));
    }

    @Test
    @DisplayName("inverted gravity subtracts twice the base value, so the pull flips but keeps its strength")
    void invertedGravityFlipsThePull() {
        double amount = GravityAttributeApplicator.modifierAmount(
                GravityDirection.DOWN_TO_UP_Y_P, VANILLA, 1.0D, 0.0D);
        assertEquals(-0.16D, amount, 1.0E-9D);
        // The resulting attribute value is the negative of the vanilla one.
        assertEquals(-VANILLA, VANILLA + amount, 1.0E-9D);
        assertTrue(GravityAttributeApplicator.needsModifier(GravityDirection.DOWN_TO_UP_Y_P));
    }

    @Test
    @DisplayName("a flipped multiplier of zero leaves vanilla gravity alone")
    void flippedMultiplierScalesTheEffect() {
        assertEquals(0.0D, GravityAttributeApplicator.modifierAmount(
                GravityDirection.DOWN_TO_UP_Y_P, VANILLA, 0.0D, 0.0D), 1.0E-9D);
        assertEquals(-0.08D, GravityAttributeApplicator.modifierAmount(
                GravityDirection.DOWN_TO_UP_Y_P, VANILLA, 0.5D, 0.0D), 1.0E-9D);
    }

    @Test
    @DisplayName("sideways directions cancel the vertical pull by default")
    void sidewaysDirectionsCancelVerticalGravity() {
        for (GravityDirection dir : new GravityDirection[] {
                GravityDirection.EAST_TO_WEST_X_N, GravityDirection.WEST_TO_EAST_X_P,
                GravityDirection.NORTH_TO_SOUTH_Z_P, GravityDirection.SOUTH_TO_NORTH_Z_N}) {
            double amount = GravityAttributeApplicator.modifierAmount(dir, VANILLA, 1.0D, 1.0D);
            assertEquals(-VANILLA, amount, 1.0E-9D, "sideways gravity should cancel the default pull");
            assertEquals(0.0D, VANILLA + amount, 1.0E-9D, "the resulting attribute value should be zero");
        }
    }
}
