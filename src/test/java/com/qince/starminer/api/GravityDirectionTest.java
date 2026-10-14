package com.qince.starminer.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Covers the pure gravity logic: dominant axis selection, the anti-flicker hysteresis and the five
 * field shapes. These need no game instance, so they run as plain unit tests.
 */
class GravityDirectionTest {

    /** A field whose centre block is the origin, so its centre point sits at (0.5, 0.5, 0.5). */
    private static GravityField field(int type, double radius) {
        return new GravityField(BlockPos.ZERO, radius, type);
    }

    private static Vec3 at(double x, double y, double z) {
        return new Vec3(x, y, z);
    }

    @Nested
    @DisplayName("dominant axis selection")
    class DominantAxis {

        @Test
        void thePullFollowsTheDominantAxis() {
            // Y names state where the body is: above the core means ordinary downward gravity.
            assertEquals(GravityDirection.UP_TO_DOWN_Y_N,
                    GravityDirection.pickByDominantAxis(0.0, 10.0, 0.0, null, false));
            // Below the core: inverted.
            assertEquals(GravityDirection.DOWN_TO_UP_Y_P,
                    GravityDirection.pickByDominantAxis(0.0, -10.0, 0.0, null, false));
            // X and Z names state where the body is pulled to.
            assertEquals(GravityDirection.EAST_TO_WEST_X_N,
                    GravityDirection.pickByDominantAxis(9.0, 0.0, 0.0, null, false));
            assertEquals(GravityDirection.WEST_TO_EAST_X_P,
                    GravityDirection.pickByDominantAxis(-9.0, 0.0, 0.0, null, false));
            assertEquals(GravityDirection.SOUTH_TO_NORTH_Z_N,
                    GravityDirection.pickByDominantAxis(0.0, 0.0, 9.0, null, false));
            assertEquals(GravityDirection.NORTH_TO_SOUTH_Z_P,
                    GravityDirection.pickByDominantAxis(0.0, 0.0, -9.0, null, false));
        }

        /**
         * The axis convention is not uniform and this is the test that pins it down: X and Z name the
         * pull direction, while Y names where the body is. Reading Y like X would make a body above the
         * core fall upwards, which breaks the whole planet mechanic.
         */
        @Test
        void theAxisConventionIsPositionForYAndPullForXZ() {
            assertSame(GravityDirection.UP_TO_DOWN_Y_N, GravityDirection.axisDirection(0, 1, 0),
                    "a body above the core must fall down");
            assertSame(GravityDirection.DOWN_TO_UP_Y_P, GravityDirection.axisDirection(0, -1, 0),
                    "a body below the core must fall up");
            assertSame(GravityDirection.EAST_TO_WEST_X_N, GravityDirection.axisDirection(1, 0, 0),
                    "a body east of the core falls west");
            assertSame(GravityDirection.WEST_TO_EAST_X_P, GravityDirection.axisDirection(-1, 0, 0),
                    "a body west of the core falls east");
            assertSame(GravityDirection.SOUTH_TO_NORTH_Z_N, GravityDirection.axisDirection(0, 0, 1),
                    "a body south of the core falls north");
            assertSame(GravityDirection.NORTH_TO_SOUTH_Z_P, GravityDirection.axisDirection(0, 0, -1),
                    "a body north of the core falls south");
        }

        /** The direction vectors must agree with the axis convention, or motion would oppose it. */
        @Test
        void theDirectionVectorMatchesTheConvention() {
            assertVector(GravityDirection.UP_TO_DOWN_Y_N, 0, -1, 0);
            assertVector(GravityDirection.DOWN_TO_UP_Y_P, 0, 1, 0);
            assertVector(GravityDirection.EAST_TO_WEST_X_N, -1, 0, 0);
            assertVector(GravityDirection.WEST_TO_EAST_X_P, 1, 0, 0);
            assertVector(GravityDirection.SOUTH_TO_NORTH_Z_N, 0, 0, -1);
            assertVector(GravityDirection.NORTH_TO_SOUTH_Z_P, 0, 0, 1);
        }

        private static void assertVector(GravityDirection direction, double x, double y, double z) {
            Vec3 vector = direction.vector();
            assertEquals(x, vector.x, 1.0E-9D, direction + " x");
            assertEquals(y, vector.y, 1.0E-9D, direction + " y");
            assertEquals(z, vector.z, 1.0E-9D, direction + " z");
        }

        @Test
        void onlyTheLargestOffsetMatters() {
            assertEquals(GravityDirection.EAST_TO_WEST_X_N,
                    GravityDirection.pickByDominantAxis(9.0, 2.0, 1.0, null, false));
            assertEquals(GravityDirection.SOUTH_TO_NORTH_Z_N,
                    GravityDirection.pickByDominantAxis(1.0, 2.0, 9.0, null, false));
            assertEquals(GravityDirection.UP_TO_DOWN_Y_N,
                    GravityDirection.pickByDominantAxis(1.0, 9.0, 2.0, null, false));
        }

        @Test
        void exactlyOnAnAxisBoundaryStillResolves() {
            // |x| == |y| == |z|: ties resolve X, then Z, then Y, so positive X wins here.
            assertEquals(GravityDirection.EAST_TO_WEST_X_N,
                    GravityDirection.pickByDominantAxis(4.0, 4.0, 4.0, null, false));
            // Ties between Y and Z resolve to Z.
            assertEquals(GravityDirection.SOUTH_TO_NORTH_Z_N,
                    GravityDirection.pickByDominantAxis(0.0, 4.0, 4.0, null, false));
            // A pure Y tie falls through to the Y branch.
            assertEquals(GravityDirection.UP_TO_DOWN_Y_N,
                    GravityDirection.pickByDominantAxis(0.0, 4.0, 0.0, null, false));
        }
    }

    @Nested
    @DisplayName("anti-flicker hysteresis")
    class Buffer {

        @Test
        void holdsTheCurrentAxisWhileTheLeadBarelyWins() {
            GravityDirection current = GravityDirection.SOUTH_TO_NORTH_Z_N;
            // X leads at 3.0 but the current Z axis is 2.6, inside the 1.5 block buffer.
            assertEquals(current, GravityDirection.pickByDominantAxis(3.0, 2.0, 2.6, current, true));
        }

        @Test
        void switchesOnceTheLeadIsDecisive() {
            GravityDirection current = GravityDirection.SOUTH_TO_NORTH_Z_N;
            // X leads at 6.0 against a current Z axis of 2.6, well beyond the buffer.
            assertEquals(GravityDirection.EAST_TO_WEST_X_N,
                    GravityDirection.pickByDominantAxis(6.0, 2.0, 2.6, current, true));
        }

        @Test
        void neverHoldsAnAxisThatIsNotTheCurrentOne() {
            GravityDirection current = GravityDirection.DOWN_TO_UP_Y_P;
            // Y is a negligible 1.0 while X leads at 5.0, so the entity is pulled onto the X axis.
            assertEquals(GravityDirection.EAST_TO_WEST_X_N,
                    GravityDirection.pickByDominantAxis(5.0, 1.0, 0.0, current, true));
        }

        @Test
        void theSameOffsetSwitchesWhenTheBufferIsOff() {
            GravityDirection current = GravityDirection.SOUTH_TO_NORTH_Z_N;
            assertEquals(GravityDirection.EAST_TO_WEST_X_N,
                    GravityDirection.pickByDominantAxis(3.0, 2.0, 2.6, current, false));
        }

        @Test
        void bufferIsIgnoredWithoutAPreviousDirection() {
            assertEquals(GravityDirection.EAST_TO_WEST_X_N,
                    GravityDirection.pickByDominantAxis(3.0, 2.0, 2.6, null, true));
        }
    }

    @Nested
    @DisplayName("direction helpers")
    class Helpers {

        @Test
        void oppositeIsAnInvolution() {
            for (GravityDirection dir : GravityDirection.VALUES) {
                assertEquals(dir, dir.opposite().opposite());
                assertEquals(dir, GravityDirection.byIndex(dir.ordinal()));
            }
        }

        @Test
        void stepVectorMatchesThePull() {
            assertEquals(new Vec3(0, -1, 0), GravityDirection.UP_TO_DOWN_Y_N.vector());
            assertEquals(new Vec3(1, 0, 0), GravityDirection.WEST_TO_EAST_X_P.vector());
            assertTrue(GravityDirection.UP_TO_DOWN_Y_N.isVanilla());
            assertTrue(GravityDirection.EAST_TO_WEST_X_N.isSideways());
        }
    }

    @Nested
    @DisplayName("field shapes")
    class FieldShapes {

        @Test
        void sphereUsesEuclideanDistance() {
            GravityField sphere = field(GravityConst.FIELD_TYPE_SPHERE, 5.0);
            assertTrue(sphere.contains(at(5.5, 0.5, 0.5)), "on the surface counts as inside");
            assertFalse(sphere.contains(at(5.6, 0.5, 0.5)), "just outside must not count");
            // (3,4,0) relative offset is exactly radius 5.
            assertTrue(sphere.contains(at(3.5, 4.5, 0.5)));
        }

        @Test
        void squareUsesChebyshevDistance() {
            GravityField square = field(GravityConst.FIELD_TYPE_SQUARE, 5.0);
            assertTrue(square.contains(at(5.5, 5.5, 5.5)), "corner of the cube is inside");
            assertFalse(square.contains(at(5.6, 0.5, 0.5)));
            assertFalse(square.contains(at(0.5, -4.6, 0.5)));
        }

        @Test
        void xCylinderIsBoundedAlongXAndRoundAroundIt() {
            GravityField cylinder = field(GravityConst.FIELD_TYPE_X_CYLINDER, 5.0);
            assertTrue(cylinder.contains(at(5.5, 5.5, 0.5)));
            assertTrue(cylinder.contains(at(-4.5, 3.5, 4.5)));
            assertFalse(cylinder.contains(at(6.5, 0.5, 0.5)), "beyond the length");
            assertFalse(cylinder.contains(at(0.5, 6.5, 0.5)), "outside the radius");
        }

        @Test
        void yCylinderIsBoundedAlongY() {
            GravityField cylinder = field(GravityConst.FIELD_TYPE_Y_CYLINDER, 5.0);
            assertTrue(cylinder.contains(at(3.5, 5.5, 4.5)));
            assertFalse(cylinder.contains(at(3.5, 6.5, 4.5)));
            assertFalse(cylinder.contains(at(6.5, 0.5, 0.5)));
        }

        @Test
        void zCylinderIsBoundedAlongZ() {
            GravityField cylinder = field(GravityConst.FIELD_TYPE_Z_CYLINDER, 5.0);
            assertTrue(cylinder.contains(at(3.5, 4.5, 5.5)));
            assertFalse(cylinder.contains(at(3.5, 4.5, 6.5)));
            assertFalse(cylinder.contains(at(3.5, 6.5, 0.5)));
        }

        @Test
        void directionAtPointsTowardsTheCentre() {
            GravityField sphere = field(GravityConst.FIELD_TYPE_SPHERE, 16.0);
            assertEquals(GravityDirection.UP_TO_DOWN_Y_N, sphere.directionAt(at(0.5, 15.5, 0.5), null, false));
            assertEquals(GravityDirection.DOWN_TO_UP_Y_P, sphere.directionAt(at(0.5, -15.5, 0.5), null, false));
            assertEquals(GravityDirection.EAST_TO_WEST_X_N, sphere.directionAt(at(15.5, 0.5, 0.5), null, false));
            assertEquals(GravityDirection.SOUTH_TO_NORTH_Z_N, sphere.directionAt(at(0.5, 0.5, 15.5), null, false));
        }

        @Test
        void directionAtHonoursTheBufferFlag() {
            GravityField sphere = field(GravityConst.FIELD_TYPE_SPHERE, 16.0);
            GravityDirection current = GravityDirection.SOUTH_TO_NORTH_Z_N;
            // 15 blocks along X against a 2.6 block Z axis: the lead is far too large for the buffer to hold Z.
            Vec3 farAlongX = at(15.5, 0.5, 2.6);
            assertEquals(GravityDirection.EAST_TO_WEST_X_N, sphere.directionAt(farAlongX, current, false));
            assertEquals(GravityDirection.EAST_TO_WEST_X_N, sphere.directionAt(farAlongX, current, true));
        }
    }
}
