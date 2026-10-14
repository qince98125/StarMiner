package com.qince.starminer.api;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Client/server shared helpers for the gravity data attached to an entity.
 *
 * <p>The 1.7.10 mod injected an extended entity property through a coremod; on 1.21.1 this is a plain
 * data attachment plus a few pure functions.
 */
public final class GravityHelper {

    /**
     * The point used when testing whether an entity is inside a field.
     *
     * <p>Field shapes are measured from the core block's centre, so entities are sampled at the centre
     * of the face pointing along their current gravity direction, matching the original mod's sampling.
     */
    public static Vec3 samplePosition(Entity entity, GravityDirection dir) {
        AABB box = entity.getBoundingBox();
        double cx = (box.minX + box.maxX) * 0.5D;
        double cy = (box.minY + box.maxY) * 0.5D;
        double cz = (box.minZ + box.maxZ) * 0.5D;
        double half = entity.getBbWidth() * 0.5D;
        return switch (dir) {
            case UP_TO_DOWN_Y_N -> new Vec3(cx, box.minY + half, cz);
            case DOWN_TO_UP_Y_P -> new Vec3(cx, box.maxY - half, cz);
            case EAST_TO_WEST_X_N -> new Vec3(box.minX + half, cy, cz);
            case WEST_TO_EAST_X_P -> new Vec3(box.maxX - half, cy, cz);
            case SOUTH_TO_NORTH_Z_N -> new Vec3(cx, cy, box.minZ + half);
            case NORTH_TO_SOUTH_Z_P -> new Vec3(cx, cy, box.maxZ - half);
        };
    }

    /**
     * The sample point for an entity, using {@code current} when it is inside a field and ordinary
     * downward gravity otherwise.
     */
    public static Vec3 resolveSamplePosition(Entity entity, GravityDirection current) {
        return samplePosition(entity, current == null ? GravityDirection.UP_TO_DOWN_Y_N : current);
    }

    private GravityHelper() {
    }
}
