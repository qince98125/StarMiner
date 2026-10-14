package com.qince.starminer.content.blockentity;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.qince.starminer.api.GravityConst;

import net.minecraft.core.BlockPos;

/**
 * Walks the cells of a star shell, one at a time.
 *
 * <p>The original 1.7.10 implementation kept a flat cursor over the whole bounding cube and skipped
 * every non-shell cell, which is O(r^3) per placed block and silently truncated large stars. This walks
 * the shell layer by layer, and inside each layer only the perimeter ring, so the step cost is O(r) and
 * there is no size limit.
 *
 * <p>Layers run from the top of the star downwards, which makes a partly built star read as a dome.
 */
public final class StarShellCursor {

    private final int shell;
    private final int type;

    private int layer;
    private int ring;
    private int index;

    public StarShellCursor(int shell, int type) {
        this.shell = Math.max(0, shell);
        this.type = GravityConst.clampFieldType(type);
    }

    public int shell() {
        return this.shell;
    }

    public int type() {
        return this.type;
    }

    public int layer() {
        return this.layer;
    }

    public int ring() {
        return this.ring;
    }

    public int index() {
        return this.index;
    }

    /** Restores a persisted position. Values outside the valid range are clamped by the accessors. */
    public void restore(int layer, int ring, int index) {
        this.layer = layer;
        this.ring = ring;
        this.index = index;
    }

    /** The layer's offset on the gravity axis when it is treated as Y, used for the build height limit. */
    public int layerY() {
        return this.shell - this.layer;
    }

    /**
     * True when the offset lies on the shell of the given shape at the given radius.
     *
     * <p>The spherical band is the one block thick onion layer {@code [r-1, r]}, which keeps small stars
     * hollow without leaving holes: radius 1 passes only the six face neighbours of the core. Cylinders
     * consist of the round surface plus both flat end caps.
     */
    public static boolean isOnShell(int x, int y, int z, int shell, int type) {
        int d = x * x + y * y + z * z;
        return switch (GravityConst.clampFieldType(type)) {
            case GravityConst.FIELD_TYPE_SQUARE ->
                    shell == Math.max(Math.max(Math.abs(x), Math.abs(y)), Math.abs(z));
            case GravityConst.FIELD_TYPE_X_CYLINDER -> Math.abs(x) == shell && y * y + z * z <= shell * shell;
            case GravityConst.FIELD_TYPE_Y_CYLINDER -> Math.abs(y) == shell && x * x + z * z <= shell * shell;
            case GravityConst.FIELD_TYPE_Z_CYLINDER -> Math.abs(z) == shell && x * x + y * y <= shell * shell;
            default -> {
                int inner = Math.max(0, shell - 1);
                yield shell > 0 && d <= shell * shell && d >= inner * inner;
            }
        };
    }

    /** Counts the shell cells, so a GUI can show build progress. */
    public static int countCells(int shell, int type) {
        if (shell <= 0) {
            return 0;
        }
        int span = shell * 2 + 1;
        int count = 0;
        for (int cell = 0; cell < span * span * span; cell++) {
            int x = cell % span - shell;
            int z = cell / span % span - shell;
            int y = cell / (span * span) - shell;
            if ((x != 0 || y != 0 || z != 0) && isOnShell(x, y, z, shell, type)) {
                count++;
            }
        }
        return count;
    }

    /** A cursor positioned exactly like this one, so iterating does not disturb the original. */
    public StarShellCursor copy() {
        StarShellCursor copy = new StarShellCursor(this.shell, this.type);
        copy.restore(this.layer, this.ring, this.index);
        return copy;
    }

    /** Iterates the remaining shell cells in build order, without disturbing this cursor. */
    public Iterator<BlockPos> iterator() {
        StarShellCursor walker = copy();
        return new Iterator<>() {
            private BlockPos next = walker.advance();

            @Override
            public boolean hasNext() {
                return this.next != null;
            }

            @Override
            public BlockPos next() {
                BlockPos current = this.next;
                if (current == null) {
                    throw new NoSuchElementException();
                }
                this.next = walker.advance();
                return current;
            }
        };
    }

    /**
     * Advances this cursor in place and returns the next shell cell, or null when the shell is finished.
     *
     * <p>Because the position lives in the cursor, a caller can walk the shell across many ticks and
     * persist {@link #layer()}, {@link #ring()} and {@link #index()} to resume after a reload.
     */
    public BlockPos advance() {
        return computeNext();
    }

    private BlockPos computeNext() {
        while (this.layer <= this.shell * 2) {
            int y = this.shell - this.layer;
            while (this.ring <= this.shell) {
                while (this.index < ringLength(this.ring)) {
                    int r = this.ring;
                    int idx = this.index++;
                    int x;
                    int z;
                    if (r == 0) {
                        x = 0;
                        z = 0;
                    } else {
                        int run = 2 * r;
                        if (idx < run) {
                            x = r;
                            z = -r + idx;
                        } else if (idx < 2 * run) {
                            x = r - (idx - run);
                            z = r;
                        } else if (idx < 3 * run) {
                            x = -r;
                            z = r - (idx - 2 * run);
                        } else {
                            x = -r + (idx - 3 * run);
                            z = -r;
                        }
                    }
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    if (isOnShell(x, y, z, this.shell, this.type)) {
                        return new BlockPos(x, y, z);
                    }
                }
                this.index = 0;
                this.ring++;
            }
            this.ring = 0;
            this.layer++;
        }
        return null;
    }

    private static int ringLength(int ring) {
        return ring == 0 ? 1 : 8 * ring;
    }
}
