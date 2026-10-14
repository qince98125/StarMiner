package com.qince.starminer.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the Gravity Core screen's geometry.
 *
 * <p>These constants must line up with the artwork, and the control grid must be large enough for every
 * button the screen registers. Getting either wrong crashes the client at screen-open time — which has
 * already happened twice during development — so both are asserted here instead.
 */
class GuiLayoutTest {

    /**
     * Buttons the screen registers: a decrement and an increment for gravity range, the same for star
     * radius, then one cycling button each for shape, build speed and buffer.
     */
    private static final int CONTROL_COUNT = 7;

    /** Rows that hold two side by side buttons. */
    private static final int PAIRED_ROWS = 2;

    /** Longest label the readout panel has to show: one CJK glyph plus three ASCII ones. */
    private static final int LONGEST_PANEL_LABEL_CJK_GLYPHS = 1;
    private static final int LONGEST_PANEL_LABEL_ASCII_GLYPHS = 3;
    private static final int ASCII_GLYPH_WIDTH = 6;

    /** Cells the layout offers: two per row. */
    private static int gridCells() {
        return GuiLayout.BUTTON_Y.length * 2;
    }

    @Test
    @DisplayName("the control grid has a cell for every button the screen registers")
    void gridFitsControlCount() {
        assertTrue(gridCells() >= CONTROL_COUNT,
                "the grid has " + gridCells() + " cells but the screen registers " + CONTROL_COUNT + " buttons");
    }

    /**
     * The paired rows plus the single rows must exactly fill the grid: two paired rows consume four
     * cells, and each remaining button takes the left cell of a later row.
     */
    @Test
    @DisplayName("the paired rows plus the single rows exactly fill the grid")
    void gridIsNotLargerThanNeeded() {
        int singleButtons = CONTROL_COUNT - PAIRED_ROWS * 2;
        int rowsNeeded = PAIRED_ROWS + singleButtons;
        assertEquals(GuiLayout.BUTTON_Y.length, rowsNeeded,
                "the grid should have exactly the rows the buttons need, no empty rows");
    }

    @Test
    @DisplayName("controls stay inside the GUI and never overlap each other")
    void buttonsFitAndDoNotOverlap() {
        for (int row = 0; row < GuiLayout.BUTTON_Y.length; row++) {
            int top = GuiLayout.BUTTON_Y[row];
            assertTrue(top >= 0, "row " + row + " starts above the GUI");
            assertTrue(top + GuiLayout.BUTTON_SIZE <= GuiLayout.GUI_HEIGHT,
                    "row " + row + " hangs below the GUI");
            if (row > 0) {
                int previousBottom = GuiLayout.BUTTON_Y[row - 1] + GuiLayout.BUTTON_SIZE;
                assertTrue(top >= previousBottom, "row " + row + " overlaps row " + (row - 1));
            }
        }
        for (int column = 0; column < 2; column++) {
            int left = GuiLayout.buttonX(column);
            assertTrue(left >= 0, "column " + column + " starts off the left edge");
            assertTrue(left + GuiLayout.BUTTON_SIZE <= GuiLayout.GUI_WIDTH,
                    "column " + column + " hangs off the right edge");
        }
        assertTrue(GuiLayout.buttonX(0) + GuiLayout.BUTTON_SIZE <= GuiLayout.buttonX(1),
                "the two control columns overlap");
        assertEquals(GuiLayout.BUTTON_COLUMN_PITCH, GuiLayout.buttonX(1) - GuiLayout.buttonX(0),
                "the column pitch must match the constant used to reserve space");
    }

    @Test
    @DisplayName("controls do not cover the slot grid")
    void buttonsClearTheSlotGrid() {
        int gridTop = GuiLayout.slotGridTop();
        int lowestBottom = GuiLayout.BUTTON_Y[GuiLayout.BUTTON_Y.length - 1] + GuiLayout.BUTTON_SIZE;
        assertTrue(lowestBottom + GuiLayout.BUTTON_GRID_CLEARANCE <= gridTop,
                "the lowest control ends at " + lowestBottom + ", which reaches into the slot grid at " + gridTop);
    }

    @Test
    @DisplayName("the slot grid fits inside the GUI")
    void slotGridFits() {
        assertTrue(GuiLayout.slotGridRight() <= GuiLayout.GUI_WIDTH,
                "the slot grid reaches x=" + GuiLayout.slotGridRight());
        assertTrue(GuiLayout.HOTBAR_Y + GuiLayout.SLOT_SIZE <= GuiLayout.GUI_HEIGHT,
                "the hotbar hangs below the GUI");
        assertTrue(GuiLayout.PLAYER_INVENTORY_Y > GuiLayout.CORE_ROW_0_Y + GuiLayout.SLOT_SIZE * 3,
                "the player inventory would overlap the core grid");
    }

    @Test
    @DisplayName("the readout panel's longest label fits on one line inside it")
    void panelTextFitsHorizontally() {
        int available = GuiLayout.PANEL_WIDTH - GuiLayout.PANEL_TEXT_X * 2;
        int needed = LONGEST_PANEL_LABEL_CJK_GLYPHS * GuiLayout.CJK_GLYPH_WIDTH
                + LONGEST_PANEL_LABEL_ASCII_GLYPHS * ASCII_GLYPH_WIDTH;
        assertTrue(needed <= available,
                "the longest readout line needs " + needed + " px but only " + available + " px are available");
    }

    @Test
    @DisplayName("every readout line fits vertically inside the panel")
    void panelTextFitsVertically() {
        int used = GuiLayout.PANEL_TEXT_Y + GuiLayout.PANEL_LINE_HEIGHT * GuiLayout.PANEL_LINES;
        assertTrue(used <= GuiLayout.PANEL_HEIGHT,
                "the readout needs " + used + " px but the panel is only " + GuiLayout.PANEL_HEIGHT + " px tall");
    }
}
