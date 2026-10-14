package com.qince.starminer.gravity;

import com.qince.starminer.api.GravityDirection;
import com.qince.starminer.registry.SMEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** Exact, orthonormal rotations shared by physics, targeting and rendering.
 * Entity positions and velocities stay in world space; yaw/pitch stay in the gravity frame.
 * Local -Y is always the pull. No trigonometric rounding enters collision calculations.
 */
public final class GravityFrame {
    public static GravityDirection direction(Entity entity) {
        if (!(entity instanceof Player player) || !SMEntityData.GRAVITY_STATE.isBound()
                || !entity.hasData(SMEntityData.GRAVITY_STATE)) return GravityDirection.UP_TO_DOWN_Y_N;
        // Vanilla flight, fluids and vehicles retain their existing world-space mechanics for now.
        if (player.getAbilities().flying || player.isInWater() || player.isInLava()
                || player.isPassenger() || player.isFallFlying() || player.isSleeping()) return GravityDirection.UP_TO_DOWN_Y_N;
        var state = entity.getData(SMEntityData.GRAVITY_STATE);
        return state.inField() ? state.direction() : GravityDirection.UP_TO_DOWN_Y_N;
    }

    public static Vec3 toWorld(GravityDirection direction, Vec3 v) {
        return switch (direction) {
            case UP_TO_DOWN_Y_N -> v;
            case DOWN_TO_UP_Y_P -> new Vec3(v.x, -v.y, -v.z);
            case EAST_TO_WEST_X_N -> new Vec3(v.y, -v.x, v.z);
            case WEST_TO_EAST_X_P -> new Vec3(-v.y, v.x, v.z);
            case NORTH_TO_SOUTH_Z_P -> new Vec3(v.x, v.z, -v.y);
            case SOUTH_TO_NORTH_Z_N -> new Vec3(v.x, -v.z, v.y);
        };
    }

    public static Vec3 toLocal(GravityDirection direction, Vec3 v) {
        return switch (direction) {
            case UP_TO_DOWN_Y_N -> v;
            case DOWN_TO_UP_Y_P -> new Vec3(v.x, -v.y, -v.z);
            case EAST_TO_WEST_X_N -> new Vec3(-v.y, v.x, v.z);
            case WEST_TO_EAST_X_P -> new Vec3(v.y, -v.x, v.z);
            case NORTH_TO_SOUTH_Z_P -> new Vec3(v.x, -v.z, v.y);
            case SOUTH_TO_NORTH_Z_N -> new Vec3(v.x, v.z, -v.y);
        };
    }

    public static AABB toLocal(GravityDirection direction, AABB box, Vec3 origin) {
        return bounds(toLocal(direction, new Vec3(box.minX, box.minY, box.minZ).subtract(origin)),
                toLocal(direction, new Vec3(box.maxX, box.maxY, box.maxZ).subtract(origin)));
    }

    public static AABB box(GravityDirection direction, Vec3 feet, double width, double height) {
        double half = width / 2;
        return bounds(toWorld(direction, new Vec3(-half, 0, -half)).add(feet),
                toWorld(direction, new Vec3(half, height, half)).add(feet));
    }

    private static AABB bounds(Vec3 a, Vec3 b) {
        return new AABB(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z),
                Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z));
    }

    public static Quaternionf rotation(GravityDirection direction) {
        return switch (direction) {
            case UP_TO_DOWN_Y_N -> new Quaternionf();
            case DOWN_TO_UP_Y_P -> new Quaternionf().rotationX((float) Math.PI);
            case EAST_TO_WEST_X_N -> new Quaternionf().rotationZ(-(float) Math.PI / 2);
            case WEST_TO_EAST_X_P -> new Quaternionf().rotationZ((float) Math.PI / 2);
            case NORTH_TO_SOUTH_Z_P -> new Quaternionf().rotationX(-(float) Math.PI / 2);
            case SOUTH_TO_NORTH_Z_N -> new Quaternionf().rotationX((float) Math.PI / 2);
        };
    }

    private GravityFrame() {}
}
