package com.qince.starminer.gravity;

import com.qince.starminer.api.GravityDirection;

import net.minecraft.world.phys.Vec3;

/**
 * The motion model for arbitrary-direction gravity.
 *
 * <p>Vanilla only knows how to pull entities towards world-down, so inside a gravity field the pull is
 * disabled on the entity ({@code setNoGravity}) and applied here instead. Pure math over {@link Vec3}, so
 * every rule below is unit tested rather than eyeballed in game.
 *
 * <p>Three rules, matching what vanilla does along Y but generalised to any axis:
 * <ul>
 *   <li><b>Acceleration.</b> The pull is added every tick. Vanilla's default strength is
 *       {@code 0.08} per tick, which is also {@code Attributes.GRAVITY}'s default.</li>
 *   <li><b>Terminal speed.</b> Vanilla multiplies the vertical component by {@code 0.98} each tick, which
 *       caps a fall at about {@code 3.92} blocks per tick. The same damping is applied along the gravity
 *       axis here, so ordinary gravity behaves exactly like vanilla.</li>
 *   <li><b>Friction.</b> When the body is resting against a surface that blocks the pull, its velocity is
 *       split: the component along the gravity axis is dropped (the surface supports it) while the
 *       tangential part is damped by friction. That split is what makes a sideways surface behave like a
 *       floor instead of like ice.</li>
 * </ul>
 */
public final class GravityMotion {

    /** Vanilla's per-tick gravity acceleration, and the default of {@code Attributes.GRAVITY}. */
    public static final double DEFAULT_GRAVITY = 0.08D;

    /** Vanilla's vertical damping factor, `d2 * 0.98F` in {@code LivingEntity.travel}. */
    public static final double AIR_DRAG = 0.98D;

    /** Vanilla's per-tick damping while on the ground, `f3 = friction * 0.91F`. */
    public static final double GROUND_DRAG = 0.91D;

    /** Vanilla's default block friction, which becomes {@code 0.6} once multiplied by {@code 0.91}. */
    public static final double DEFAULT_FRICTION = 0.6D;

    /**
     * Terminal speed of an ordinary vanilla fall.
     *
     * <p>Vanilla's loop is {@code v = (v - g) * d} with {@code g = 0.08} and {@code d = 0.98}. Its fixed
     * point solves {@code v = (v - g) * d}, giving {@code v = g * d / (1 - d) = 3.92} blocks per tick —
     * not the continuous limit {@code g / (1 - d)}. The model here reproduces that value exactly.
     */
    public static final double TERMINAL_SPEED = DEFAULT_GRAVITY * AIR_DRAG / (1.0D - AIR_DRAG);

    /**
     * Integrates one tick of motion under an arbitrary gravity direction.
     *
     * @param velocity  the current velocity
     * @param direction the direction gravity pulls
     * @param grounded  true when a surface is blocking the pull, i.e. the body is standing on it
     * @return the new velocity
     */
    public static Vec3 update(Vec3 velocity, GravityDirection direction, boolean grounded) {
        return update(velocity, direction, grounded, DEFAULT_GRAVITY);
    }

    /**
     * Integrates one tick of motion under an arbitrary gravity direction.
     *
     * @param velocity  the current velocity
     * @param direction the direction gravity pulls
     * @param grounded  true when a surface is blocking the pull, i.e. the body is standing on it
     * @param gravity   the pull strength per tick, normally {@link #DEFAULT_GRAVITY}
     */
    public static Vec3 update(Vec3 velocity, GravityDirection direction, boolean grounded, double gravity) {
        Vec3 pull = direction.vector();
        Vec3 next = velocity.add(pull.scale(gravity));

        // Damp along the gravity axis only: vanilla damps only Y, everything else is untouched until the
        // ground friction pass below.
        double alongGravity = next.dot(pull);
        double damped = alongGravity * AIR_DRAG;
        next = next.add(pull.scale(damped - alongGravity));

        if (grounded) {
            // The surface supports the body: remove the component pushing into it, damp what slides along.
            double intoSurface = next.dot(pull);
            next = next.subtract(pull.scale(intoSurface));

            double friction = DEFAULT_FRICTION * GROUND_DRAG;
            next = next.scale(friction).add(pull.scale(next.dot(pull) * (1.0D - friction)));
        }

        return next;
    }

    /**
     * Whether a surface is blocking the pull, derived from the entity's collision flags.
     *
     * <p>There is no {@code onGround} equivalent for an arbitrary axis: vanilla computes
     * {@code verticalCollisionBelow} as "collided while moving with {@code y < 0}".
     *
     * @param horizontalCollision {@code Entity.horizontalCollision} from the previous move
     * @param verticalCollision   {@code Entity.verticalCollision} from the previous move
     */
    public static boolean grounded(GravityDirection direction, boolean horizontalCollision, boolean verticalCollision) {
        // A vertical pull is blocked by the collision flag that matches its sign: downward by "collided
        // below", upward by any vertical collision (vanilla has no "collided above" flag).
        if (direction.stepY() < 0) {
            return verticalCollision;
        }
        if (direction.stepY() > 0) {
            return verticalCollision;
        }
        return horizontalCollision;
    }

    /**
     * Removes the velocity component along the gravity axis, used when the pull direction changes so the
     * body does not inherit a shove from the axis it used to fall along.
     *
     * <p>Vanilla's own guard for this is {@code if (onGround) setDeltaMovement(0, d, 0)}; the equivalent
     * here is dropping whatever the body was doing along the old axis.
     */
    public static Vec3 projectOntoPlane(Vec3 velocity, GravityDirection direction) {
        Vec3 pull = direction.vector();
        return velocity.subtract(pull.scale(velocity.dot(pull)));
    }

    private GravityMotion() {
    }
}
