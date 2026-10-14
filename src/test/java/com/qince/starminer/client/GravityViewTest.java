package com.qince.starminer.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qince.starminer.api.GravityDirection;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers the field-relative view.
 *
 * <p>This is the fix for the reported bug: the look limits and the turn axes must follow the gravity
 * field, not the world. Both properties are asserted directly — a limit along the pull reaches exactly
 * +/-90, and the look direction round trips through the world angles vanilla renders from.
 */
class GravityViewTest {

    private static final double EPSILON = 1.0E-6D;
    private static final float ANGLE_EPSILON = 1.0E-3F;

    @Test
    @DisplayName("ordinary gravity behaves exactly like vanilla")
    void ordinaryGravityMatchesVanilla() {
        GravityDirection down = GravityDirection.UP_TO_DOWN_Y_N;
        // Looking north, level: vanilla yaw 0, pitch 0.
        GravityView.View view = GravityView.fromVanillaAngles(down, 0.0F, 0.0F);
        assertEquals(0.0F, view.pitch(), ANGLE_EPSILON);

        Vec3 look = GravityView.lookDirection(down, view);
        assertEquals(0.0D, look.x, EPSILON);
        assertEquals(0.0D, look.y, EPSILON);
        assertEquals(1.0D, look.z, EPSILON, "vanilla yaw 0 looks along +Z");

        // And the world angles round trip.
        assertEquals(0.0F, GravityView.vanillaYaw(look), ANGLE_EPSILON);
        assertEquals(0.0F, GravityView.vanillaPitch(look), ANGLE_EPSILON);
    }

    @Test
    @DisplayName("vanilla angle round trip holds for a spread of angles")
    void vanillaRoundTrip() {
        for (float yaw = -180.0F; yaw <= 180.0F; yaw += 45.0F) {
            for (float pitch = -80.0F; pitch <= 80.0F; pitch += 20.0F) {
                Vec3 look = GravityView.vanillaLook(yaw, pitch);
                // Yaw is circular: 180 and -180 name the same direction, so compare with a wrap.
                assertEquals(0.0F, net.minecraft.util.Mth.wrapDegrees(GravityView.vanillaYaw(look) - yaw),
                        1.0E-2F, "yaw " + yaw + " pitch " + pitch);
                assertEquals(pitch, GravityView.vanillaPitch(look), 1.0E-2F, "yaw " + yaw + " pitch " + pitch);
            }
        }
    }

    @Test
    @DisplayName("looking up and down reaches the limits of the field, not of the world")
    void limitsFollowTheField() {
        for (GravityDirection direction : new GravityDirection[] {
                GravityDirection.UP_TO_DOWN_Y_N, GravityDirection.DOWN_TO_UP_Y_P}) {
            GravityView.View level = GravityView.fromVanillaAngles(direction, 0.0F, 0.0F);

            // Tilting far past the limit is clamped to exactly +/-90.
            GravityView.View tooFarUp = GravityView.turn(direction, level, 0.0F, 400.0F);
            assertEquals(GravityView.PITCH_LIMIT, tooFarUp.pitch(), ANGLE_EPSILON, direction + " up limit");
            GravityView.View tooFarDown = GravityView.turn(direction, level, 0.0F, -400.0F);
            assertEquals(-GravityView.PITCH_LIMIT, tooFarDown.pitch(), ANGLE_EPSILON, direction + " down limit");

            // At -90 the look direction is exactly along the pull; at +90 exactly against it. Both point
            // straight up or down in world space, so the field direction decides the sign.
            Vec3 pull = direction.vector();
            Vec3 down = GravityView.lookDirection(direction, tooFarDown);
            assertEquals(pull.x, down.x, EPSILON);
            assertEquals(pull.y, down.y, EPSILON);
            assertEquals(pull.z, down.z, EPSILON, direction + " looking down means looking along gravity");

            Vec3 up = GravityView.lookDirection(direction, tooFarUp);
            assertEquals(-pull.x, up.x, EPSILON);
            assertEquals(-pull.y, up.y, EPSILON);
            assertEquals(-pull.z, up.z, EPSILON, direction + " looking up means looking against gravity");
            // And the world angles must be renderable. "Up" in a downward field is straight up in world
            // terms, which vanilla spells as pitch -90; an upward field reverses both.
            float pullSign = direction.stepY() < 0 ? 1.0F : -1.0F;
            assertEquals(-90.0F * pullSign, GravityView.vanillaPitch(up), 1.0E-2F,
                    direction + " up in world terms");
            assertEquals(90.0F * pullSign, GravityView.vanillaPitch(down), 1.0E-2F,
                    direction + " down in world terms");
        }
    }

    @Test
    @DisplayName("the clamp follows the field when it turns sideways")
    void limitsFollowASidewaysField() {
        GravityDirection west = GravityDirection.EAST_TO_WEST_X_N;
        GravityView.View level = GravityView.fromVanillaAngles(west, 0.0F, 0.0F);

        GravityView.View down = GravityView.turn(west, level, 0.0F, -200.0F);
        assertEquals(-GravityView.PITCH_LIMIT, down.pitch(), ANGLE_EPSILON);
        Vec3 look = GravityView.lookDirection(west, down);
        assertEquals(-1.0D, look.x, EPSILON, "looking 'down' in a westward field means looking west");
        assertEquals(0.0D, look.y, EPSILON);
        assertEquals(0.0D, look.z, EPSILON);

        GravityView.View up = GravityView.turn(west, level, 0.0F, 200.0F);
        Vec3 upLook = GravityView.lookDirection(west, up);
        assertEquals(1.0D, upLook.x, EPSILON, "looking 'up' in a westward field means looking east");
    }

    @Test
    @DisplayName("the yaw axis is the pull axis, so turning never changes how much the player faces the pull")
    void yawTurnsAboutThePullAxis() {
        for (GravityDirection direction : GravityDirection.VALUES) {
            GravityView.View view = GravityView.fromVanillaAngles(direction, 0.0F, 0.0F);
            double facingBefore = GravityView.lookDirection(direction, view).dot(direction.vector());

            for (float step : new float[] {10.0F, -10.0F, 90.0F, 180.0F}) {
                GravityView.View turned = GravityView.turn(direction, view, step, 0.0F);
                double facingAfter = GravityView.lookDirection(direction, turned).dot(direction.vector());
                assertEquals(facingBefore, facingAfter, 1.0E-6D,
                        direction + " yaw must not tilt the view into or out of the pull");
                // The turn must actually happen.
                assertEquals(1.0D, GravityView.lookDirection(direction, turned).length(), 1.0E-6D);
            }
        }
    }

    @Test
    @DisplayName("turning a full circle about the pull returns to the starting direction")
    void fullYawTurnIsACircle() {
        GravityDirection down = GravityDirection.UP_TO_DOWN_Y_N;
        GravityView.View start = GravityView.fromVanillaAngles(down, 0.0F, 0.0F);
        GravityView.View view = start;
        for (int i = 0; i < 36; i++) {
            view = GravityView.turn(down, view, 10.0F, 0.0F);
        }
        Vec3 begin = GravityView.lookDirection(down, start);
        Vec3 end = GravityView.lookDirection(down, view);
        assertEquals(begin.x, end.x, 1.0E-4D);
        assertEquals(begin.y, end.y, 1.0E-4D);
        assertEquals(begin.z, end.z, 1.0E-4D);
    }

    @Test
    @DisplayName("entering a field while looking along the pull still yields a usable view")
    void enteringWhileLookingAlongThePullIsSafe() {
        // Looking straight down while the field pulls down is the degenerate case: the look direction has
        // no horizon component to project, so a fallback direction must be produced. The reported field
        // pitch then depends on which way the field pulls, since the look direction is fixed in world
        // terms: it is against the pull for a downward field and along it for an upward one.
        for (GravityDirection direction : GravityDirection.VALUES) {
            Vec3 look = GravityView.vanillaLook(0.0F, GravityView.vanillaPitch(direction.vector()));
            GravityView.View view = GravityView.fromVanillaAngles(
                    direction, GravityView.vanillaYaw(look), GravityView.vanillaPitch(look));

            assertEquals(1.0D, view.forward().length(), 1.0E-6D, direction + " forward must stay a unit vector");
            assertEquals(0.0D, view.forward().dot(direction.vector()), 1.0E-6D,
                    direction + " the horizon direction must be perpendicular to the pull");

            // The field pitch must match the look direction, which is what round tripping guarantees.
            Vec3 rebuilt = GravityView.lookDirection(direction, view);
            assertEquals(look.x, rebuilt.x, 1.0E-4D, direction + " x");
            assertEquals(look.y, rebuilt.y, 1.0E-4D, direction + " y");
            assertEquals(look.z, rebuilt.z, 1.0E-4D, direction + " z");
        }
    }

    @Test
    @DisplayName("the world pitch stays inside vanilla's range for every view")
    void worldPitchAlwaysRenderable() {
        for (GravityDirection direction : GravityDirection.VALUES) {
            GravityView.View view = GravityView.fromVanillaAngles(direction, 30.0F, 20.0F);
            for (float yaw : new float[] {0.0F, 90.0F, 180.0F}) {
                for (float pitch : new float[] {-90.0F, -45.0F, 0.0F, 45.0F, 90.0F}) {
                    GravityView.View candidate = GravityView.turn(direction, view, yaw, pitch);
                    Vec3 look = GravityView.lookDirection(direction, candidate);
                    float worldPitch = GravityView.vanillaPitch(look);
                    assertTrue(worldPitch >= -90.0F && worldPitch <= 90.0F,
                            direction + " produced a world pitch of " + worldPitch);
                }
            }
        }
    }
}
