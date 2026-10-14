package com.qince.starminer.gravity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qince.starminer.api.GravityDirection;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers the arbitrary-direction motion model.
 *
 * <p>The strongest check available is that the generic code reproduces vanilla exactly when the gravity
 * direction is ordinary downward gravity, so that case is asserted against the numbers vanilla itself
 * uses rather than against whatever this implementation happens to produce.
 */
class GravityMotionTest {

    private static final double EPSILON = 1.0E-9D;

    @Test
    @DisplayName("downward gravity reproduces vanilla's per-tick acceleration")
    void downwardGravityMatchesVanilla() {
        // Vanilla: d2 -= 0.08 then d2 * 0.98, from rest.
        Vec3 afterOneTick = GravityMotion.update(Vec3.ZERO, GravityDirection.UP_TO_DOWN_Y_N, false);
        assertEquals(0.0D, afterOneTick.x, EPSILON);
        assertEquals(0.0D, afterOneTick.z, EPSILON);
        assertEquals(-0.08D * 0.98D, afterOneTick.y, EPSILON);
    }

    @Test
    @DisplayName("an ordinary fall converges on vanilla's terminal velocity")
    void fallReachesVanillaTerminalSpeed() {
        Vec3 velocity = Vec3.ZERO;
        for (int tick = 0; tick < 1200; tick++) {
            velocity = GravityMotion.update(velocity, GravityDirection.UP_TO_DOWN_Y_N, false);
        }
        // Vanilla settles at 0.08 * 0.98 / (1 - 0.98) = 3.92 blocks per tick. The approach is geometric
        // with ratio 0.98, so it takes a few hundred ticks to get there.
        assertEquals(-GravityMotion.TERMINAL_SPEED, velocity.y, 1.0E-4D);
        assertTrue(velocity.y > -4.0D, "the fall must not exceed the terminal speed");
        assertTrue(velocity.y < -3.9D, "the fall must approach the terminal speed");
    }

    @Test
    @DisplayName("a sideways pull accelerates along that axis and leaves the others alone")
    void sidewaysPullAcceleratesAlongItsAxis() {
        Vec3 velocity = Vec3.ZERO;
        for (int tick = 0; tick < 40; tick++) {
            velocity = GravityMotion.update(velocity, GravityDirection.EAST_TO_WEST_X_N, false);
        }
        // Pulling west: negative X grows, Y and Z stay untouched.
        assertTrue(velocity.x < -1.0D, "the body must build up westward speed");
        assertEquals(0.0D, velocity.y, EPSILON, "sideways gravity must not introduce vertical motion");
        assertEquals(0.0D, velocity.z, EPSILON);
    }

    @Test
    @DisplayName("every direction reaches the same terminal speed along its own axis")
    void everyDirectionHasTheSameTerminalSpeed() {
        for (GravityDirection direction : GravityDirection.VALUES) {
            Vec3 velocity = Vec3.ZERO;
            for (int tick = 0; tick < 1200; tick++) {
                velocity = GravityMotion.update(velocity, direction, false);
            }
            double along = velocity.dot(direction.vector());
            assertEquals(GravityMotion.TERMINAL_SPEED, along, 1.0E-4D,
                    direction + " should converge on the terminal speed");
        }
    }

    @Test
    @DisplayName("standing on a surface cancels the pull and slows the slide")
    void groundedCancelsThePullAndAppliesFriction() {
        Vec3 velocity = new Vec3(1.0D, -0.5D, 0.0D);
        Vec3 next = GravityMotion.update(velocity, GravityDirection.UP_TO_DOWN_Y_N, true);

        assertEquals(0.0D, next.y, EPSILON, "the surface supports the body, so no downward speed remains");
        // Only the tangential part survives, damped by 0.6 * 0.91 = 0.546.
        assertEquals(0.546D, next.x, 1.0E-6D);
        assertEquals(0.0D, next.z, EPSILON);
    }

    @Test
    @DisplayName("a body resting on a surface stays at rest")
    void restingBodyStaysStill() {
        for (GravityDirection direction : GravityDirection.VALUES) {
            Vec3 next = GravityMotion.update(Vec3.ZERO, direction, true);
            assertEquals(0.0D, next.length(), 1.0E-9D,
                    direction + " must not creep along its surface");
        }
    }

    @Test
    @DisplayName("a body sliding along a sideways surface decays instead of being pushed by gravity")
    void sidewaysSurfaceSupportsTheBody() {
        // Moving along the surface (Z), while gravity pulls down the X axis. Gravity must contribute
        // nothing along X, and the slide must decay.
        Vec3 sliding = new Vec3(0.0D, 0.0D, 1.0D);
        Vec3 first = GravityMotion.update(sliding, GravityDirection.EAST_TO_WEST_X_N, true);
        Vec3 second = GravityMotion.update(first, GravityDirection.EAST_TO_WEST_X_N, true);

        assertEquals(0.0D, first.x, EPSILON, "the supporting axis must stay exactly zero");
        assertEquals(0.0D, second.x, EPSILON);
        assertTrue(first.z < sliding.z, "the slide must decay");
        assertTrue(second.z < first.z, "the slide must keep decaying");
    }

    @Test
    @DisplayName("velocity pointing into a sideways surface is removed, not reversed")
    void velocityIntoTheSurfaceIsRemoved() {
        Vec3 intoSurface = new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 next = GravityMotion.update(intoSurface, GravityDirection.EAST_TO_WEST_X_N, true);
        assertEquals(0.0D, next.x, EPSILON, "the body must not be pushed back out of the surface");
        assertEquals(0.0D, next.y, EPSILON);
        assertEquals(0.0D, next.z, EPSILON);
    }

    @Test
    @DisplayName("the grounded flag is read from the collision that opposes the pull")
    void groundedComesFromTheOpposingCollision() {
        // Downward pull is supported by a vertical collision.
        assertTrue(GravityMotion.grounded(GravityDirection.UP_TO_DOWN_Y_N, false, true));
        assertFalse(GravityMotion.grounded(GravityDirection.UP_TO_DOWN_Y_N, true, false));
        // Inverted pull is also a vertical collision.
        assertTrue(GravityMotion.grounded(GravityDirection.DOWN_TO_UP_Y_P, false, true));
        // A sideways pull is supported by a horizontal collision.
        assertTrue(GravityMotion.grounded(GravityDirection.EAST_TO_WEST_X_N, true, false));
        assertFalse(GravityMotion.grounded(GravityDirection.EAST_TO_WEST_X_N, false, true));
        assertTrue(GravityMotion.grounded(GravityDirection.NORTH_TO_SOUTH_Z_P, true, false));
    }

    @Test
    @DisplayName("projecting onto the pull axis removes exactly that component")
    void directionChangeDoesNotInheritSpeed() {
        // Falling fast, then the field switches to a sideways pull: projecting onto the new axis must
        // leave the fall alone, because the fall is perpendicular to that axis.
        Vec3 falling = new Vec3(0.0D, -3.0D, 1.0D);
        Vec3 sideways = GravityMotion.projectOntoPlane(falling, GravityDirection.EAST_TO_WEST_X_N);
        assertEquals(0.0D, sideways.dot(GravityDirection.EAST_TO_WEST_X_N.vector()), EPSILON,
                "the new pull axis must carry no inherited velocity");
        assertEquals(-3.0D, sideways.y, EPSILON, "the fall is perpendicular to a sideways axis, so it stays");
        assertEquals(1.0D, sideways.z, EPSILON);

        // Projecting onto a vertical axis is what strips a fall.
        Vec3 ontoVertical = GravityMotion.projectOntoPlane(falling, GravityDirection.DOWN_TO_UP_Y_P);
        assertEquals(0.0D, ontoVertical.y, EPSILON, "the pull axis component is removed");
        assertEquals(1.0D, ontoVertical.z, EPSILON);
    }

    @Test
    @DisplayName("projecting onto the plane keeps velocities that are already flat")
    void projectIsIdempotentForFlatVelocities() {
        Vec3 flat = new Vec3(0.4D, 0.0D, -0.7D);
        Vec3 once = GravityMotion.projectOntoPlane(flat, GravityDirection.SOUTH_TO_NORTH_Z_N);
        Vec3 twice = GravityMotion.projectOntoPlane(once, GravityDirection.SOUTH_TO_NORTH_Z_N);
        assertEquals(once.x, twice.x, EPSILON);
        assertEquals(once.y, twice.y, EPSILON);
        assertEquals(once.z, twice.z, EPSILON);
    }
}
