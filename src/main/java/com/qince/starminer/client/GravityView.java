package com.qince.starminer.client;

import com.qince.starminer.api.GravityDirection;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The player's view, expressed relative to the gravity field rather than relative to the world.
 *
 * <p>Vanilla derives the view from two world-aligned angles: yaw turns about world Y, and pitch is clamped
 * to {@code [-90, 90]} against the world horizon. Inside a gravity field both references are wrong, and
 * neither can be fixed by rolling the camera afterwards — the clamp still happens about the world horizon
 * and the mouse still turns about world Y. So the view is held here as a look direction plus an up
 * direction, both derived from gravity, and only converted back to vanilla angles for rendering.
 *
 * <p>{@code forward} is always a unit vector perpendicular to the pull, and {@code pitch} is measured
 * from the field's horizon, so the two look limits are {@code +90} (straight up, away from the pull) and
 * {@code -90} (straight down, along the pull) — the same range vanilla allows, but relative to gravity.
 * That is what makes "look up" and "look down" follow the field when it turns.
 *
 * <p>Pure math over {@link Vec3}, so all of it is unit tested.
 */
public final class GravityView {

    /** Vanilla's pitch limit, applied here about the field's horizon instead of the world's. */
    public static final float PITCH_LIMIT = 90.0F;

    /** A look direction and the pitch that produced it, both relative to the gravity field. */
    public record View(Vec3 forward, float pitch) {
    }

    /**
     * Builds the field-relative view that matches the player's current world angles.
     *
     * <p>Called when a player first enters a field, so entering does not jerk the view.
     *
     * @param direction the direction gravity pulls
     * @param yaw       the player's world yaw in degrees
     * @param pitch     the player's world pitch in degrees
     */
    public static View fromVanillaAngles(GravityDirection direction, float yaw, float pitch) {
        Vec3 pull = direction.vector();
        Vec3 worldForward = vanillaLook(yaw, pitch);

        // Flatten the look direction into the field's horizon plane. If the player happens to be looking
        // straight along the pull there is nothing to project, so fall back to the field's north.
        Vec3 flat = worldForward.subtract(pull.scale(worldForward.dot(pull)));
        if (flat.lengthSqr() < 1.0E-8D) {
            flat = northInFieldPlane(direction);
        }
        flat = flat.normalize();

        // Field pitch is positive when looking away from the pull (up) and negative when looking along it
        // (down). A look direction with a positive component along the pull is therefore a negative pitch.
        double toward = worldForward.dot(pull);
        return new View(flat, (float) -Math.toDegrees(Math.asin(Mth.clamp(toward, -1.0D, 1.0D))));
    }

    /**
     * Turns the view by a mouse movement.
     *
     * <p>The tilt is clamped <em>before</em> it is applied, not only in the stored pitch: applying the raw
     * delta while clamping only the angle lets the look direction keep rotating past the limit, so looking
     * down far enough would swing the view over the top.
     *
     * @param view     the current view
     * @param yawDeg   degrees to turn about the pull axis; positive matches vanilla's sense
     * @param pitchDeg degrees to tilt, clamped against the field's horizon
     */
    public static View turn(GravityDirection direction, View view, float yawDeg, float pitchDeg) {
        Vec3 pull = direction.vector();

        float pitch = Mth.clamp(view.pitch() + pitchDeg, -PITCH_LIMIT, PITCH_LIMIT);
        float appliedPitch = pitch - view.pitch();

        // Yaw: turn about the pull axis.
        Vec3 forward = yawDeg == 0.0F ? view.forward() : rotateAbout(view.forward(), pull, yawDeg).normalize();

        // Pitch: tilt about the axis perpendicular to both the pull and the current look direction.
        // forward stays in the horizon plane; lookDirection applies pitch exactly once.

        return new View(forward, pitch);
    }

    /**
     * Reconstructs the look direction from a view.
     *
     * <p>{@code forward} is the horizon part of the look, i.e. it is perpendicular to the pull, and the
     * pitch tilts it away from the pull, so the pull is subtracted as the pitch goes negative.
     */
    public static Vec3 lookDirection(GravityDirection direction, View view) {
        Vec3 pull = direction.vector();
        double radians = Math.toRadians(view.pitch());
        return view.forward().scale(Math.cos(radians))
                .subtract(pull.scale(Math.sin(radians)))
                .normalize();
    }

    /** The world yaw that renders a look direction, matching vanilla's {@code calculateViewVector}. */
    public static float vanillaYaw(Vec3 look) {
        return (float) Math.toDegrees(Math.atan2(-look.x, look.z));
    }

    /** The world pitch that renders a look direction. Safe for every direction, including straight up. */
    public static float vanillaPitch(Vec3 look) {
        return (float) -Math.toDegrees(Math.asin(Mth.clamp(look.y, -1.0D, 1.0D)));
    }

    /**
     * Vanilla's own look vector formula, kept here so the round trip can be asserted in tests.
     *
     * <p>See {@code Entity.calculateViewVector}.
     */
    public static Vec3 vanillaLook(float yaw, float pitch) {
        float pitchRad = pitch * (float) (Math.PI / 180.0);
        float yawRad = -yaw * (float) (Math.PI / 180.0);
        float cosYaw = Mth.cos(yawRad);
        float sinYaw = Mth.sin(yawRad);
        float cosPitch = Mth.cos(pitchRad);
        float sinPitch = Mth.sin(pitchRad);
        return new Vec3(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
    }

    /** Rotates a vector about an axis by an angle in degrees, using the right-hand rule. */
    static Vec3 rotateAbout(Vec3 vector, Vec3 axis, float degrees) {
        Quaternionf rotation = new Quaternionf().fromAxisAngleRad(
                (float) axis.x, (float) axis.y, (float) axis.z, (float) Math.toRadians(degrees));
        Vector3f result = new Vector3f((float) vector.x, (float) vector.y, (float) vector.z);
        rotation.transform(result);
        return new Vec3(result.x, result.y, result.z);
    }

    /** A horizon direction for a field whose plane does not contain the world's north. */
    private static Vec3 northInFieldPlane(GravityDirection direction) {
        Vec3 pull = direction.vector();
        Vec3 north = new Vec3(0.0D, 0.0D, -1.0D);
        Vec3 flat = north.subtract(pull.scale(north.dot(pull)));
        if (flat.lengthSqr() < 1.0E-8D) {
            // The pull is along Z, so use east instead.
            Vec3 east = new Vec3(1.0D, 0.0D, 0.0D);
            flat = east.subtract(pull.scale(east.dot(pull)));
        }
        return flat.normalize();
    }

    private GravityView() {
    }
}
