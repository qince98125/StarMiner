package com.qince.starminer.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * A gravity field projected by a Gravity Core, reduced to the data needed for spatial queries.
 */
public record GravityField(BlockPos center, double gravityRange, int type, boolean useBuffer) {
    public GravityField(BlockPos center, double gravityRange, int type) {
        this(center, gravityRange, type, true);
    }

    public static final GravityField NONE = new GravityField(BlockPos.ZERO, 0.0D, GravityConst.FIELD_TYPE_SPHERE);

    /** The field's centre point; the core block's own centre, not its corner. */
    public Vec3 centerVec() {
        return new Vec3(this.center.getX() + 0.5D, this.center.getY() + 0.5D, this.center.getZ() + 0.5D);
    }

    public boolean isActive() {
        return this.gravityRange > 0.0D;
    }

    /** True when {@code pos} lies inside the configured shape. */
    public boolean contains(Vec3 pos) {
        return contains(this.center.getX() + 0.5D, this.center.getY() + 0.5D, this.center.getZ() + 0.5D, pos);
    }

    public boolean contains(double cx, double cy, double cz, Vec3 pos) {
        double xRel = pos.x - cx;
        double yRel = pos.y - cy;
        double zRel = pos.z - cz;
        double r = this.gravityRange;
        double r2 = r * r;

        return switch (GravityConst.clampFieldType(this.type)) {
            case GravityConst.FIELD_TYPE_SQUARE ->
                    Math.abs(xRel) <= r && Math.abs(yRel) <= r && Math.abs(zRel) <= r;
            case GravityConst.FIELD_TYPE_X_CYLINDER ->
                    Math.abs(xRel) <= r && (yRel * yRel + zRel * zRel) <= r2;
            case GravityConst.FIELD_TYPE_Y_CYLINDER ->
                    Math.abs(yRel) <= r && (xRel * xRel + zRel * zRel) <= r2;
            case GravityConst.FIELD_TYPE_Z_CYLINDER ->
                    Math.abs(zRel) <= r && (xRel * xRel + yRel * yRel) <= r2;
            default -> (xRel * xRel + yRel * yRel + zRel * zRel) <= r2;
        };
    }

    /**
     * The direction this field pulls a body at world position {@code (x, y, z)} towards.
     *
     * <p>{@code current} is the body's existing direction, used for the anti-flicker hysteresis; pass
     * {@code null} for a fresh resolution. Fills {@code out} and returns it to avoid allocating.
     */
    public GravityDirection directionAt(double x, double y, double z, GravityDirection current, GravityDirection[] out) {
        GravityDirection dir = directionAt(new Vec3(x, y, z), current, this.useBuffer);
        out[0] = dir;
        return dir;
    }

    /**
     * The direction this field pulls a body at {@code pos} towards.
     *
     * <p>{@code useBuffer} selects whether the anti-flicker rule applies. Callers that already know the
     * body's current direction should pass true; pure resolution (tests, spawning) should pass false.
     */
    public GravityDirection directionAt(Vec3 pos, GravityDirection current, boolean useBuffer) {
        Vec3 c = centerVec();
        double x = pos.x - c.x;
        double y = pos.y - c.y;
        double z = pos.z - c.z;
        boolean cylinder = this.type >= GravityConst.FIELD_TYPE_X_CYLINDER && this.type <= GravityConst.FIELD_TYPE_Z_CYLINDER;
        if (!cylinder) return GravityDirection.pickByDominantAxis(x, y, z, current, useBuffer);

        // The original cylinders are habitats on the INSIDE of a tube: ignore the length axis,
        // and pull outward in its cross-section. Spheres/cubes instead pull toward their center.
        boolean eligible = current != null && switch (this.type) {
            case GravityConst.FIELD_TYPE_X_CYLINDER -> current.stepX() == 0;
            case GravityConst.FIELD_TYPE_Y_CYLINDER -> current.stepY() == 0;
            default -> current.stepZ() == 0;
        };
        if (this.type == GravityConst.FIELD_TYPE_X_CYLINDER) x = 0;
        if (this.type == GravityConst.FIELD_TYPE_Y_CYLINDER) y = 0;
        if (this.type == GravityConst.FIELD_TYPE_Z_CYLINDER) z = 0;
        if (eligible && useBuffer) {
            double along = x * current.stepX() + y * current.stepY() + z * current.stepZ();
            double lead = Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z)));
            if (along >= 0 && lead - along < GravityConst.AXIS_BUFFER_WIDTH) return current;
        }
        return switch (this.type) {
            case GravityConst.FIELD_TYPE_X_CYLINDER -> Math.abs(z) > Math.abs(y)
                    ? (z < 0 ? GravityDirection.SOUTH_TO_NORTH_Z_N : GravityDirection.NORTH_TO_SOUTH_Z_P)
                    : (y < 0 ? GravityDirection.UP_TO_DOWN_Y_N : GravityDirection.DOWN_TO_UP_Y_P);
            case GravityConst.FIELD_TYPE_Y_CYLINDER -> Math.abs(x) > Math.abs(z)
                    ? (x < 0 ? GravityDirection.EAST_TO_WEST_X_N : GravityDirection.WEST_TO_EAST_X_P)
                    : (z < 0 ? GravityDirection.SOUTH_TO_NORTH_Z_N : GravityDirection.NORTH_TO_SOUTH_Z_P);
            default -> Math.abs(x) > Math.abs(y)
                    ? (x < 0 ? GravityDirection.EAST_TO_WEST_X_N : GravityDirection.WEST_TO_EAST_X_P)
                    : (y < 0 ? GravityDirection.UP_TO_DOWN_Y_N : GravityDirection.DOWN_TO_UP_Y_P);
        };
    }
}
