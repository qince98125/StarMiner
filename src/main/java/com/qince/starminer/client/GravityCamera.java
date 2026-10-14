package com.qince.starminer.client;

import com.qince.starminer.api.GravityDirection;

import net.minecraft.util.Mth;
import org.joml.Quaternionf;

/**
 * Rotates the first-person view so that "down" on screen points along gravity.
 *
 * <p>Vanilla always treats world-down as screen-down. Inside a gravity field that is wrong for five of
 * the six directions, so the camera needs a roll that brings the gravity vector onto the screen's down
 * axis. This class computes that roll; it is pure geometry, so it is unit tested against all six
 * directions at several yaw angles rather than being eyeballed in game.
 *
 * <p>For a vertical direction the required roll is exactly zero: ordinary gravity already points down
 * the screen, and inverted gravity is deliberately left un-rolled (the player sees the world upside
 * down, which is the familiar "flipped gravity" look).
 */
public final class GravityCamera {

    /**
     * The roll, in degrees, to hand to {@code ViewportEvent.ComputeCameraAngles}.
     *
     * @param direction the direction gravity pulls
     * @param yaw       the camera's yaw in degrees, in Minecraft's convention (0 = south, 90 = west)
     */
    public static float rollFor(GravityDirection direction, float yaw) {
        if (!direction.isSideways()) {
            // Ordinary gravity needs no correction; inverted gravity is left upside down.
            return 0.0F;
        }

        // Screen basis. Screen-down is the direction a positive pitch looks along, which vanilla derives
        // as (-sin(yaw), 0, cos(yaw)); screen-right is perpendicular to it in the horizontal plane.
        double yawRad = Math.toRadians(yaw);
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);
        double downX = -Math.sin(yawRad);
        double downZ = Math.cos(yawRad);

        // The pull is purely horizontal for every sideways direction.
        double gravityX = direction.stepX();
        double gravityZ = direction.stepZ();

        double alongDown = downX * gravityX + downZ * gravityZ;
        double alongRight = rightX * gravityX + rightZ * gravityZ;

        // Rolling by theta rotates screen-down to down*cos(theta) - right*sin(theta), so aligning it with
        // the pull gives atan2(-alongRight, alongDown) in that frame. Minecraft's roll uses the opposite
        // sense, which is why the sign below is positive: a westward pull facing north must roll +90.
        return (float) Math.toDegrees(Math.atan2(alongRight, alongDown));
    }

    /**
     * The effective yaw of the camera, which is what the player's look direction is built from.
     *
     * <p>The screen roll is applied on top of the vanilla yaw, so the yaw itself is unchanged; this
     * exists so callers have one place to ask rather than re-deriving it.
     */
    public static float yawFor(float playerYaw) {
        return playerYaw;
    }

    /** Normalises an angle into {@code (-180, 180]}, matching how Minecraft reports rotations. */
    public static float wrapDegrees(float degrees) {
        return Mth.wrapDegrees(degrees);
    }

    /**
     * The rotation that takes ordinary world-down onto the direction gravity pulls.
     *
     * <p>Applying this to a rendered player model makes the body stand on whatever surface the gravity
     * field considers the floor. The model sits at the entity's position, so rotating the pose about its
     * origin is enough — no translation is needed.
     *
     * @param dest receives the rotation; returned for reuse
     */
    public static Quaternionf orientationFor(GravityDirection direction, Quaternionf dest) {
        return dest.set(com.qince.starminer.gravity.GravityFrame.rotation(direction));
    }

    private GravityCamera() {
    }
}
