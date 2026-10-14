package com.qince.starminer.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.qince.starminer.api.GravityDirection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pins down the camera roll that makes gravity point down the screen.
 *
 * <p>The roll is derived from a cross product, and hand-deriving the sign for every yaw is exactly the
 * kind of thing this project has already got wrong once (the Y axis of the gravity enum), so the
 * expected values below were worked out from the screen basis and are asserted explicitly.
 */
class GravityCameraTest {

    private static final float TOLERANCE = 1.0E-3F;

    /** Facing north: screen-right is east (positive X), screen-down is south (positive Z). */
    private static final float YAW_NORTH = 0.0F;
    /** Facing west: screen-right is north (negative Z), screen-down is west (negative X). */
    private static final float YAW_WEST = 90.0F;

    @Test
    @DisplayName("vertical gravity never rolls the camera")
    void verticalDirectionsNeedNoRoll() {
        assertEquals(0.0F, GravityCamera.rollFor(GravityDirection.UP_TO_DOWN_Y_N, YAW_NORTH), TOLERANCE);
        assertEquals(0.0F, GravityCamera.rollFor(GravityDirection.DOWN_TO_UP_Y_P, YAW_NORTH), TOLERANCE);
        // The yaw must not matter either.
        assertEquals(0.0F, GravityCamera.rollFor(GravityDirection.UP_TO_DOWN_Y_N, 137.0F), TOLERANCE);
        assertEquals(0.0F, GravityCamera.rollFor(GravityDirection.DOWN_TO_UP_Y_P, -42.0F), TOLERANCE);
    }

    @Test
    @DisplayName("pulling north or south needs no roll when facing north")
    void northSouthPullFacingNorth() {
        // Facing north: screen-down is south, which is exactly where SOUTH_TO_NORTH... wait, that pulls
        // north. NORTH_TO_SOUTH_Z_P pulls south, so it already matches screen-down.
        assertEquals(0.0F, GravityCamera.rollFor(GravityDirection.NORTH_TO_SOUTH_Z_P, YAW_NORTH), TOLERANCE);
        // A northward pull is the opposite of screen-down, so the view turns a half circle.
        assertEquals(180.0F, Math.abs(GravityCamera.rollFor(GravityDirection.SOUTH_TO_NORTH_Z_N, YAW_NORTH)),
                TOLERANCE);
    }

    @Test
    @DisplayName("pulling west or east rolls the view a quarter turn when facing north")
    void eastWestPullRollsAQuarterTurn() {
        // Facing north, screen-right is east. A roll of +90 turns screen-down towards screen-right,
        // which is east; a roll of -90 turns it towards screen-left, which is west.
        assertEquals(-90.0F, GravityCamera.rollFor(GravityDirection.EAST_TO_WEST_X_N, YAW_NORTH), TOLERANCE);
        assertEquals(90.0F, GravityCamera.rollFor(GravityDirection.WEST_TO_EAST_X_P, YAW_NORTH), TOLERANCE);
    }

    @Test
    @DisplayName("the roll follows the camera as the player turns")
    void rollFollowsYaw() {
        // Facing west, screen-down is west, which is exactly what EAST_TO_WEST_X_N pulls towards.
        assertEquals(0.0F, GravityCamera.rollFor(GravityDirection.EAST_TO_WEST_X_N, YAW_WEST), TOLERANCE);
        // Pulling east is then a half turn away from screen-down.
        assertEquals(180.0F, Math.abs(GravityCamera.rollFor(GravityDirection.WEST_TO_EAST_X_P, YAW_WEST)), TOLERANCE);
        // Facing west, screen-right is north, so a roll of +90 turns screen-down towards north.
        assertEquals(-90.0F, GravityCamera.rollFor(GravityDirection.SOUTH_TO_NORTH_Z_N, YAW_WEST), TOLERANCE);
        assertEquals(90.0F, GravityCamera.rollFor(GravityDirection.NORTH_TO_SOUTH_Z_P, YAW_WEST), TOLERANCE);
    }

    /**
     * The definition of correct: after the roll, the screen-down axis must point along the pull. This is
     * checked directly rather than only through listed angles, so it also covers yaw values not listed
     * above, and it is the assertion that fixes the sign convention.
     */
    @Test
    @DisplayName("after the roll, screen-down points along gravity")
    void rolledScreenDownAlignsWithGravity() {
        float[] yaws = {0.0F, 45.0F, 90.0F, 135.0F, 180.0F, -90.0F, -137.0F, 271.0F};
        for (GravityDirection direction : GravityDirection.VALUES) {
            if (!direction.isSideways()) {
                continue;
            }
            for (float yaw : yaws) {
                double rollRad = Math.toRadians(GravityCamera.rollFor(direction, yaw));
                double yawRad = Math.toRadians(yaw);

                double downX = -Math.sin(yawRad);
                double downZ = Math.cos(yawRad);
                double rightX = Math.cos(yawRad);
                double rightZ = Math.sin(yawRad);

                // A positive roll turns the camera's basis clockwise about the view axis, so screen-down
                // becomes right*sin(theta) + down*cos(theta). Asserting this against the pull is what
                // pins the sign convention down; writing the same expression here as the implementation
                // would make the test vacuous.
                double rolledX = rightX * Math.sin(rollRad) + downX * Math.cos(rollRad);
                double rolledZ = rightZ * Math.sin(rollRad) + downZ * Math.cos(rollRad);

                assertEquals(direction.stepX(), rolledX, 1.0E-6D,
                        direction + " x at yaw " + yaw);
                assertEquals(direction.stepZ(), rolledZ, 1.0E-6D,
                        direction + " z at yaw " + yaw);
            }
        }
    }
}
