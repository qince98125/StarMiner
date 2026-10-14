package com.qince.starminer.content.blockentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.qince.starminer.api.GravityConst;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers the star shell walker: shell membership per shape, that a walk yields every shell cell exactly
 * once, and that the cursor can be suspended and resumed through its persisted position.
 */
class StarShellCursorTest {

    @Test
    @DisplayName("a spherical shell is the one block thick band [r-1, r]")
    void sphericalShell() {
        int radius = 5;
        int type = GravityConst.FIELD_TYPE_SPHERE;

        // 3-4-5: exactly on the outer boundary.
        assertTrue(StarShellCursor.isOnShell(3, 4, 0, radius, type));
        assertTrue(StarShellCursor.isOnShell(0, -5, 0, radius, type));
        // One layer inside the band.
        assertTrue(StarShellCursor.isOnShell(0, -4, 0, radius, type));
        // Deeper inside, and one layer outside: neither belongs to the shell.
        assertFalse(StarShellCursor.isOnShell(0, 3, 0, radius, type));
        assertFalse(StarShellCursor.isOnShell(0, 5, 1, radius, type));
        // The core itself is never part of the shell.
        assertFalse(StarShellCursor.isOnShell(0, 0, 0, radius, type));
    }

    @Test
    @DisplayName("radius 1 passes only the six face neighbours, keeping the star hollow")
    void radiusOneIsAFacedHollowShell() {
        assertTrue(StarShellCursor.isOnShell(1, 0, 0, 1, GravityConst.FIELD_TYPE_SPHERE));
        assertTrue(StarShellCursor.isOnShell(0, -1, 0, 1, GravityConst.FIELD_TYPE_SPHERE));
        // Diagonals sit at distance sqrt(2) or sqrt(3), outside the band.
        assertFalse(StarShellCursor.isOnShell(1, 1, 0, 1, GravityConst.FIELD_TYPE_SPHERE));
        assertFalse(StarShellCursor.isOnShell(1, 1, 1, 1, GravityConst.FIELD_TYPE_SPHERE));
        assertEquals(6, StarShellCursor.countCells(1, GravityConst.FIELD_TYPE_SPHERE));
    }

    @Test
    @DisplayName("a square shell is the chebyshev boundary")
    void squareShell() {
        int radius = 5;
        int type = GravityConst.FIELD_TYPE_SQUARE;

        assertTrue(StarShellCursor.isOnShell(5, 0, 0, radius, type));
        assertTrue(StarShellCursor.isOnShell(5, 4, -2, radius, type));
        assertTrue(StarShellCursor.isOnShell(-5, 5, 5, radius, type));
        assertFalse(StarShellCursor.isOnShell(4, 4, 4, radius, type));
        // The surface of the (2r+1) cube: the full cube minus the (2r-1) interior.
        int side = radius * 2 + 1;
        int interior = radius * 2 - 1;
        assertEquals(side * side * side - interior * interior * interior,
                StarShellCursor.countCells(radius, type));
    }

    @Test
    @DisplayName("a cylinder shell is the round surface plus both flat end caps")
    void cylinderShells() {
        int radius = 5;

        // X cylinder: |x| must equal the radius, and the cap spans the whole circular end.
        assertTrue(StarShellCursor.isOnShell(5, 3, 4, radius, GravityConst.FIELD_TYPE_X_CYLINDER));
        assertTrue(StarShellCursor.isOnShell(5, 5, 0, radius, GravityConst.FIELD_TYPE_X_CYLINDER),
                "the flat end cap belongs to the shell");
        assertFalse(StarShellCursor.isOnShell(5, 5, 5, radius, GravityConst.FIELD_TYPE_X_CYLINDER),
                "the cap does not extend past the radius");
        assertFalse(StarShellCursor.isOnShell(4, 3, 4, radius, GravityConst.FIELD_TYPE_X_CYLINDER),
                "inside the band is not part of the shell");

        // Y cylinder: the same rule around the Y axis.
        assertTrue(StarShellCursor.isOnShell(3, 5, 4, radius, GravityConst.FIELD_TYPE_Y_CYLINDER));
        assertTrue(StarShellCursor.isOnShell(0, 5, 5, radius, GravityConst.FIELD_TYPE_Y_CYLINDER));
        assertFalse(StarShellCursor.isOnShell(3, 4, 4, radius, GravityConst.FIELD_TYPE_Y_CYLINDER));

        // Z cylinder.
        assertTrue(StarShellCursor.isOnShell(3, 4, 5, radius, GravityConst.FIELD_TYPE_Z_CYLINDER));
        assertTrue(StarShellCursor.isOnShell(5, 0, 5, radius, GravityConst.FIELD_TYPE_Z_CYLINDER));
        assertFalse(StarShellCursor.isOnShell(3, 4, 4, radius, GravityConst.FIELD_TYPE_Z_CYLINDER));
    }

    @Test
    @DisplayName("an out of range shape index falls back to the sphere")
    void unknownShapeFallsBackToSphere() {
        assertTrue(StarShellCursor.isOnShell(0, 5, 0, 5, 99));
        assertFalse(StarShellCursor.isOnShell(0, 5, 1, 5, 99));
    }

    @Test
    @DisplayName("a full walk yields every shell cell exactly once, and the count matches")
    void walkVisitsEveryShellCellOnce() {
        for (int shape = 0; shape < GravityConst.FIELD_TYPE_COUNT; shape++) {
            for (int radius : new int[] {1, 2, 3, 5}) {
                StarShellCursor cursor = new StarShellCursor(radius, shape);
                Set<BlockPos> seen = new HashSet<>();
                List<BlockPos> order = new ArrayList<>();
                for (BlockPos cell = cursor.advance(); cell != null; cell = cursor.advance()) {
                    assertTrue(seen.add(cell), "duplicate cell " + cell + " for shape " + shape + " r" + radius);
                    assertTrue(StarShellCursor.isOnShell(cell.getX(), cell.getY(), cell.getZ(), radius, shape),
                            "cell " + cell + " is not on the shell");
                    order.add(cell);
                }
                assertEquals(StarShellCursor.countCells(radius, shape), seen.size(),
                        "walk length must match countCells for shape " + shape + " r" + radius);
                assertNull(cursor.advance(), "a finished cursor keeps returning null");
            }
        }
    }

    @Test
    @DisplayName("the walk starts at the top layer and works downwards")
    void walkStartsAtTheTop() {
        StarShellCursor cursor = new StarShellCursor(3, GravityConst.FIELD_TYPE_SPHERE);
        BlockPos first = cursor.advance();
        assertNotNull(first);
        assertEquals(3, first.getY(), "the top layer is built first");
    }

    @Test
    @DisplayName("the cursor position can be saved and resumed")
    void cursorResumesFromAPersistedPosition() {
        StarShellCursor full = new StarShellCursor(4, GravityConst.FIELD_TYPE_SPHERE);
        List<BlockPos> expected = new ArrayList<>();
        for (BlockPos cell = full.advance(); cell != null; cell = full.advance()) {
            expected.add(cell);
        }

        // Walk a few cells, then persist the position the way the block entity does.
        StarShellCursor firstRun = new StarShellCursor(4, GravityConst.FIELD_TYPE_SPHERE);
        List<BlockPos> collected = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            collected.add(firstRun.advance());
        }

        StarShellCursor resumed = new StarShellCursor(4, GravityConst.FIELD_TYPE_SPHERE);
        resumed.restore(firstRun.layer(), firstRun.ring(), firstRun.index());
        for (BlockPos cell = resumed.advance(); cell != null; cell = resumed.advance()) {
            collected.add(cell);
        }

        assertEquals(expected, collected, "resuming must continue exactly where the walk stopped");
    }

    @Test
    @DisplayName("a zero radius shell has no cells and yields nothing")
    void zeroRadiusIsEmpty() {
        assertEquals(0, StarShellCursor.countCells(0, GravityConst.FIELD_TYPE_SPHERE));
        assertNull(new StarShellCursor(0, GravityConst.FIELD_TYPE_SPHERE).advance());
    }

    @Test
    @DisplayName("iterating a cursor leaves its own position untouched")
    void iterationDoesNotDisturbTheCursor() {
        StarShellCursor cursor = new StarShellCursor(3, GravityConst.FIELD_TYPE_SPHERE);
        cursor.advance();
        cursor.advance();
        int layer = cursor.layer();
        int ring = cursor.ring();
        int index = cursor.index();

        int iterated = 0;
        for (var it = cursor.iterator(); it.hasNext(); it.next()) {
            iterated++;
        }

        assertTrue(iterated > 0);
        assertEquals(layer, cursor.layer());
        assertEquals(ring, cursor.ring());
        assertEquals(index, cursor.index());
    }
}
