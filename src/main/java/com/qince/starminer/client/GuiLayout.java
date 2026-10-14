package com.qince.starminer.client;

/**
 * Geometry of the Gravity Core screen, kept free of rendering types so it can be unit tested.
 *
 * <p>The artwork fixes these numbers: the slot outlines, the dark readout panel and the empty squares
 * down the right hand side are all baked into the texture, so the screen has to align to them rather
 * than lay itself out freely. Getting one of these constants wrong crashes the client when the screen
 * opens, which is why the invariants are asserted by {@code GuiLayoutTest}.
 *
 * <p>This is the single source of truth for all of it: the menu lays its slots out with these values
 * and the screen draws with them.
 */
public final class GuiLayout {

    /** Overall size of the used area of the artwork. */
    public static final int GUI_WIDTH = 176;
    public static final int GUI_HEIGHT = 222;

    /** Slot pitch, matching the baked slot outlines. */
    public static final int SLOT_SIZE = 18;
    /** Number of slot columns in each row. */
    public static final int SLOT_COLUMNS = 9;
    /** The first slot outline starts one pixel above the slot surface itself. */
    public static final int SLOT_OUTLINE_INSET = 1;

    // Slot geometry, measured from the baked slot outlines.
    public static final int SLOT_GRID_X = 8;
    public static final int CORE_ROW_0_Y = 72;
    public static final int GRAVITIZE_Y = 54;
    public static final int PLAYER_INVENTORY_Y = 140;
    public static final int HOTBAR_Y = 198;

    // Label strips.
    public static final int TITLE_Y = 6;
    public static final int INVENTORY_LABEL_Y = 130;

    // Dark readout panel baked into the artwork.
    public static final int PANEL_X = 7;
    public static final int PANEL_Y = 7;
    public static final int PANEL_WIDTH = 36;
    public static final int PANEL_HEIGHT = 49;
    public static final int PANEL_TEXT_X = 3;
    public static final int PANEL_TEXT_Y = 3;
    public static final int PANEL_LINE_HEIGHT = 9;
    /** Number of readout lines the panel holds: range, radius, shape, speed and buffer. */
    public static final int PANEL_LINES = 5;
    /** Non-ASCII glyphs are nine pixels wide in the default font. */
    public static final int CJK_GLYPH_WIDTH = 9;

    /** Horizontal distance between the two control columns. */
    public static final int BUTTON_COLUMN_PITCH = 20;

    /**
     * The controls, as a 2 x 5 grid down the right hand side.
     *
     * <p>The first two rows hold the decrement/increment pairs for gravity range and star radius; the
     * remaining rows hold one cycling button each for shape, build speed and buffer. Five rows of 16 px
     * buttons do not fit between the top of the GUI and the slot grid at {@link #CORE_ROW_0_Y}, so the
     * buttons are drawn small. {@code GuiLayoutTest} asserts the rows and columns stay in bounds and clear
     * of the grid.
     */
    public static final int BUTTON_LEFT_X = 124;
    public static final int BUTTON_RIGHT_X = 144;
    public static final int[] BUTTON_Y = {9, 21, 33, 45, 57};
    public static final int BUTTON_SIZE = 10;
    /** Vertical gap kept clear between the lowest control and the slot grid. */
    public static final int BUTTON_GRID_CLEARANCE = 2;

    public static int slotX(int column) {
        return SLOT_GRID_X + column * SLOT_SIZE;
    }

    /** Left edge of a control, given its column index (0 = left, 1 = right). */
    public static int buttonX(int column) {
        return column == 0 ? BUTTON_LEFT_X : BUTTON_RIGHT_X;
    }

    public static int panelTextX() {
        return PANEL_X + PANEL_TEXT_X;
    }

    public static int panelTextY() {
        return PANEL_Y + PANEL_TEXT_Y;
    }

    /** Y of the first slot outline, which is one pixel above the first slot row. */
    public static int slotGridTop() {
        return CORE_ROW_0_Y - SLOT_OUTLINE_INSET;
    }

    /** Rightmost pixel the nine slot columns occupy. */
    public static int slotGridRight() {
        return slotX(SLOT_COLUMNS - 1) + SLOT_SIZE - 2;
    }

    private GuiLayout() {
    }
}
