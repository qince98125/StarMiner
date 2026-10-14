package com.qince.starminer.api;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The six discrete gravity directions the original mod used.
 *
 * <p>Names follow the original enum so the reference decompilation stays readable: {@code upTOdown_YN}
 * means "up to down along the Y axis, negative" - i.e. ordinary Minecraft gravity. The first three
 * letters are the world axis, the trailing two are the sign.
 */
public enum GravityDirection {
    /** Ordinary gravity: falls towards -Y. */
    UP_TO_DOWN_Y_N(0, -1, 0),
    /** Inverted gravity: falls towards +Y. */
    DOWN_TO_UP_Y_P(0, 1, 0),
    /** Falls towards -X (from east to west). */
    EAST_TO_WEST_X_N(-1, 0, 0),
    /** Falls towards +X (from west to east). */
    WEST_TO_EAST_X_P(1, 0, 0),
    /** Falls towards +Z (from north to south). */
    NORTH_TO_SOUTH_Z_P(0, 0, 1),
    /** Falls towards -Z (from south to north). */
    SOUTH_TO_NORTH_Z_N(0, 0, -1);

    public static final GravityDirection[] VALUES = values();
    public static final int COUNT = VALUES.length;

    private final int dx;
    private final int dy;
    private final int dz;

    GravityDirection(int dx, int dy, int dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    /** Unit vector pointing the way gravity pulls. */
    public Vec3 vector() {
        return new Vec3(this.dx, this.dy, this.dz);
    }

    public int stepX() {
        return this.dx;
    }

    public int stepY() {
        return this.dy;
    }

    public int stepZ() {
        return this.dz;
    }

    /** True when this direction is ordinary downward gravity. */
    public boolean isVanilla() {
        return this == UP_TO_DOWN_Y_N;
    }

    /** True when the gravity pull has no vertical component (walls / ceilings become floors). */
    public boolean isSideways() {
        return this.dy == 0;
    }

    /** The closest axis-aligned cardinal direction, for facing/step logic. */
    public Direction toDirection() {
        return switch (this) {
            case UP_TO_DOWN_Y_N -> Direction.DOWN;
            case DOWN_TO_UP_Y_P -> Direction.UP;
            case EAST_TO_WEST_X_N -> Direction.WEST;
            case WEST_TO_EAST_X_P -> Direction.EAST;
            case NORTH_TO_SOUTH_Z_P -> Direction.SOUTH;
            case SOUTH_TO_NORTH_Z_N -> Direction.NORTH;
        };
    }

    /** The opposite direction (used by the gravity controller's reverse mode). */
    public GravityDirection opposite() {
        return switch (this) {
            case UP_TO_DOWN_Y_N -> DOWN_TO_UP_Y_P;
            case DOWN_TO_UP_Y_P -> UP_TO_DOWN_Y_N;
            case EAST_TO_WEST_X_N -> WEST_TO_EAST_X_P;
            case WEST_TO_EAST_X_P -> EAST_TO_WEST_X_N;
            case NORTH_TO_SOUTH_Z_P -> SOUTH_TO_NORTH_Z_N;
            case SOUTH_TO_NORTH_Z_N -> NORTH_TO_SOUTH_Z_P;
        };
    }

    public static GravityDirection byIndex(int index) {
        return VALUES[Mth.clamp(index, 0, COUNT - 1)];
    }

    /**
     * The layer of an axis-aligned box that faces the given gravity direction, i.e. the centre of the
     * "underside" of the entity for that direction. Mirrors the original mod's per-direction entity
     * sampling, but exposed as an explicit layer index so it can be unit tested.
     */
    public static double faceLayer(AABB box, GravityDirection dir) {
        return switch (dir) {
            case UP_TO_DOWN_Y_N -> box.minY;
            case DOWN_TO_UP_Y_P -> box.maxY;
            case EAST_TO_WEST_X_N -> box.minX;
            case WEST_TO_EAST_X_P -> box.maxX;
            case SOUTH_TO_NORTH_Z_N -> box.minZ;
            case NORTH_TO_SOUTH_Z_P -> box.maxZ;
        };
    }

    /**
     * Picks the direction the gravity of a star centred at the offset origin pulls a body towards.
     *
     * <p>Only the dominant axis matters; see {@link #axisDirection} for the axis convention, which is
     * the part that is easy to get backwards.
     *
     * @param current   the body's current direction, or {@code null} when unknown
     * @param useBuffer when true, keep {@code current} while its axis is within
     *                  {@link GravityConst#AXIS_BUFFER_WIDTH} blocks of the leading axis. That hysteresis
     *                  is what stops a body flip-flopping as it walks along a boundary between two axes.
     */
    public static GravityDirection pickByDominantAxis(double xRel, double yRel, double zRel,
                                                      GravityDirection current, boolean useBuffer) {
        double ax = Math.abs(xRel);
        double ay = Math.abs(yRel);
        double az = Math.abs(zRel);

        // Leading axis, ties resolved X > Z > Y so the result is deterministic.
        double lead = Math.max(ax, Math.max(ay, az));

        if (useBuffer && current != null) {
            double currentCoefficient = switch (current) {
                case EAST_TO_WEST_X_N, WEST_TO_EAST_X_P -> ax;
                case NORTH_TO_SOUTH_Z_P, SOUTH_TO_NORTH_Z_N -> az;
                case UP_TO_DOWN_Y_N, DOWN_TO_UP_Y_P -> ay;
            };
            boolean stillTowardCenter = xRel * current.dx + yRel * current.dy + zRel * current.dz <= 0;
            if (stillTowardCenter && lead - currentCoefficient < GravityConst.AXIS_BUFFER_WIDTH) {
                return current;
            }
        }

        if (ax >= ay && ax >= az) {
            return axisDirection(xRel, 0, 0);
        }
        if (az >= ay) {
            return axisDirection(0, 0, zRel);
        }
        return axisDirection(0, yRel, 0);
    }

    /**
     * The direction a body at the given offset along a single axis is pulled towards.
     *
     * <p>This is the whole axis convention in one place, and it is deliberately <em>not</em> uniform —
     * the original 1.7.10 mod used two different readings of its own enum names, and both are load
     * bearing:
     *
     * <ul>
     *   <li><b>X and Z name the pull.</b> A body east of the core ({@code xRel > 0}) falls
     *       <em>west</em>, so this returns {@link #EAST_TO_WEST_X_N}. A body south of the core falls
     *       north, returning {@link #SOUTH_TO_NORTH_Z_N}.</li>
     *   <li><b>Y names the body.</b> A body above the core ({@code yRel > 0}) falls <em>down</em>, so
     *       this returns {@link #UP_TO_DOWN_Y_N}. Reading Y like X would make a body above the core
     *       fall upwards, which breaks the entire planet mechanic.</li>
     * </ul>
     *
     * <p>Only this Y reading makes ordinary gravity the default, which is why the original chose it.
     * Exactly one component of the offset must be non-zero.
     */
    static GravityDirection axisDirection(double xRel, double yRel, double zRel) {
        if (yRel != 0.0D) {
            // Y names where the body is: above the core means it falls down.
            return yRel > 0.0D ? UP_TO_DOWN_Y_N : DOWN_TO_UP_Y_P;
        }
        if (xRel != 0.0D) {
            // X names where the body is pulled to: east of the core means it falls west.
            return xRel > 0.0D ? EAST_TO_WEST_X_N : WEST_TO_EAST_X_P;
        }
        if (zRel != 0.0D) {
            // Z likewise: south of the core means it falls north.
            return zRel > 0.0D ? SOUTH_TO_NORTH_Z_N : NORTH_TO_SOUTH_Z_P;
        }
        return UP_TO_DOWN_Y_N;
    }
}
