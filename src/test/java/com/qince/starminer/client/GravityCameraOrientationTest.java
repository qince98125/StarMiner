package com.qince.starminer.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.qince.starminer.api.GravityDirection;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies the rotation applied to a rendered player model.
 *
 * <p>The definition of correct is stated directly: after the rotation, world-down must point along the
 * direction gravity pulls. Checking it that way rather than through hand-derived angles avoids the trap
 * of writing the same wrong expression in both the code and the test.
 */
class GravityCameraOrientationTest {

    @Test
    @DisplayName("world-down rotates onto the gravity vector for every direction")
    void downRotatesOntoGravity() {
        for (GravityDirection direction : GravityDirection.VALUES) {
            Quaternionf rotation = GravityCamera.orientationFor(direction, new Quaternionf());
            Vector3f down = new Vector3f(0.0F, -1.0F, 0.0F);
            rotation.transform(down);

            assertEquals(direction.stepX(), down.x, 1.0E-5F, direction + " x");
            assertEquals(direction.stepY(), down.y, 1.0E-5F, direction + " y");
            assertEquals(direction.stepZ(), down.z, 1.0E-5F, direction + " z");
        }
    }

    @Test
    @DisplayName("ordinary gravity produces no rotation at all")
    void ordinaryGravityIsIdentity() {
        Quaternionf rotation = GravityCamera.orientationFor(GravityDirection.UP_TO_DOWN_Y_N, new Quaternionf());
        Vector3f forward = new Vector3f(1.0F, 0.0F, 0.0F);
        rotation.transform(forward);
        assertEquals(1.0F, forward.x, 1.0E-6F);
        assertEquals(0.0F, forward.y, 1.0E-6F);
        assertEquals(0.0F, forward.z, 1.0E-6F);
        assertEquals(1.0F, Math.abs(rotation.w), 1.0E-6F, "an identity rotation has w = 1");
    }

    @Test
    @DisplayName("a sideways direction stands the model on its side")
    void sidewaysDirectionTipsTheModelOver() {
        Quaternionf rotation = GravityCamera.orientationFor(GravityDirection.EAST_TO_WEST_X_N, new Quaternionf());
        // The model's own up axis must end up pointing east, since gravity pulls west.
        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
        rotation.transform(up);
        assertEquals(1.0F, up.x, 1.0E-5F);
        assertEquals(0.0F, up.y, 1.0E-5F);
        assertEquals(0.0F, up.z, 1.0E-5F);
    }
}
