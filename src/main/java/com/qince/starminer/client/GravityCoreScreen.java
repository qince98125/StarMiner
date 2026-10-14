package com.qince.starminer.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.qince.starminer.Config;
import com.qince.starminer.StarMiner;
import com.qince.starminer.api.BuildSpeed;
import com.qince.starminer.api.GravityConst;
import com.qince.starminer.content.menu.GravityCoreMenu;
import com.qince.starminer.network.GravityCoreSettingsPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Screen for the Gravity Core.
 *
 * <p>The artwork contains the slot outlines, a 36 x 49 dark readout panel at {@code x 7..42 / y 7..55},
 * and three empty squares down the right hand side. Text is kept inside those bounds and the squares
 * become the controls; the geometry lives in {@link GuiLayout} and is asserted by {@code GuiLayoutTest}.
 *
 * <p>Each value has a decrease and an increase button. Right-clicking any control steps it backwards,
 * and holding shift or control multiplies the step, so a large star radius does not need a hundred
 * clicks. Everything the screen sends is re-clamped on the server.
 */
public class GravityCoreScreen extends AbstractContainerScreen<GravityCoreMenu> {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(StarMiner.MODID, "textures/gui/gui_gravitycore.png");

    private static final int LABEL_COLOUR = 0xE0E0E0;
    private static final int VALUE_COLOUR = 0x9BE7FF;
    private static final int STATE_WORKING_COLOUR = 0x6FE06F;
    private static final int STATE_IDLE_COLOUR = 0x9A9A9A;

    /**
     * Ordered exactly as the buttons are laid out.
     *
     * <p>{@code DECREMENT} and {@code INCREMENT} are the two columns of a paired row; the single-button
     * fields occupy the left column of the rows below.
     */
    private enum Action {
        GRAVITY_RANGE_DECREMENT(Field.GRAVITY_RANGE, -1, "minus"),
        GRAVITY_RANGE_INCREMENT(Field.GRAVITY_RANGE, 1, "plus"),
        STAR_RADIUS_DECREMENT(Field.STAR_RADIUS, -1, "minus"),
        STAR_RADIUS_INCREMENT(Field.STAR_RADIUS, 1, "plus"),
        FIELD_TYPE_CYCLE(Field.FIELD_TYPE, 1, "cycle"),
        BUILD_SPEED_CYCLE(Field.BUILD_SPEED, 1, "cycle"),
        BUFFER_TOGGLE(Field.BUFFER, 1, "cycle");

        private final Field field;
        private final int direction;
        private final String labelSuffix;

        Action(Field field, int direction, String labelSuffix) {
            this.field = field;
            this.direction = direction;
            this.labelSuffix = labelSuffix;
        }

        /** The sign this button applies; the keyboard only scales the magnitude. */
        int sign() {
            return this.direction;
        }
    }

    /** The values that can be edited, each pairing a translation key with its readout line. */
    private enum Field {
        GRAVITY_RANGE("starInfo.titleGrange"),
        STAR_RADIUS("starInfo.titleSSize"),
        FIELD_TYPE("starInfo.titleType"),
        BUILD_SPEED("starInfo.titleSpeed"),
        BUFFER("starInfo.bufferzone");

        private final String nameKey;

        Field(String nameKey) {
            this.nameKey = nameKey;
        }
    }

    private record Control(Button button, Field field) {
    }

    /** How many buttons share a row at the top; the rest sit one per row below. */
    private static final int PAIRED_ROWS = 2;

    private final List<Control> controls = new ArrayList<>();

    public GravityCoreScreen(GravityCoreMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GuiLayout.GUI_WIDTH;
        this.imageHeight = GuiLayout.GUI_HEIGHT;
        this.titleLabelY = GuiLayout.TITLE_Y;
        this.inventoryLabelY = GuiLayout.INVENTORY_LABEL_Y;
    }

    @Override
    protected void init() {
        super.init();
        this.controls.clear();

        Action[] actions = Action.values();
        for (int index = 0; index < actions.length; index++) {
            Action action = actions[index];
            // The paired fields take both columns of their row; everything else takes the left column.
            int row = index < PAIRED_ROWS * 2 ? index / 2 : PAIRED_ROWS + (index - PAIRED_ROWS * 2);
            int column = index < PAIRED_ROWS * 2 ? index % 2 : 0;
            if (row >= GuiLayout.BUTTON_Y.length) {
                // A layout mistake must not take the client down when the screen opens.
                StarMiner.LOGGER.error("Gravity core screen has no button slot for {}, ignoring", action);
                continue;
            }
            addControl(action, row, column);
        }
    }

    private void addControl(Action action, int row, int column) {
        Component label = Component.translatable(
                "starInfo.ctl." + action.field.name().toLowerCase(java.util.Locale.ROOT) + "." + action.labelSuffix);
        Button button = Button.builder(label, b -> this.onControl(action, Screen.hasShiftDown(), Screen.hasControlDown()))
                .bounds(this.leftPos + GuiLayout.buttonX(column),
                        this.topPos + GuiLayout.BUTTON_Y[row],
                        GuiLayout.BUTTON_SIZE, GuiLayout.BUTTON_SIZE)
                .build();
        this.addRenderableWidget(button);
        this.controls.add(new Control(button, action.field));
    }

    /** Applies a click: the button decides the sign, the keyboard decides the size of the step. */
    private void onControl(Action action, boolean shift, boolean control) {
        int step = (control ? 100 : shift ? 10 : 1) * action.sign();
        int gravityRange = this.menu.getGravityRange();
        int starRadius = this.menu.getStarRadius();
        int fieldType = this.menu.getFieldType();
        int speedIndex = this.menu.getSpeedIndex();
        boolean buffer = this.menu.isUsingBufferArea();

        switch (action.field) {
            case GRAVITY_RANGE -> gravityRange = clamp(gravityRange + step, 0, Config.MAX_GRAVITY_RANGE.get());
            case STAR_RADIUS -> starRadius = clamp(starRadius + step, 0, Config.MAX_STAR_RADIUS.get());
            case FIELD_TYPE -> fieldType = Math.floorMod(fieldType + step, GravityConst.FIELD_TYPE_COUNT);
            case BUILD_SPEED -> speedIndex = Math.floorMod(speedIndex + step, BuildSpeed.VALUES.length);
            case BUFFER -> buffer = !buffer;
        }
        send(gravityRange, starRadius, fieldType, buffer, speedIndex);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void send(int gravityRange, int starRadius, int fieldType, boolean useBufferArea, int speedIndex) {
        BlockPos pos = this.menu.getCorePos();
        if (pos == null || pos.equals(BlockPos.ZERO)) {
            // Without a known position the server could not resolve the core; do nothing rather than
            // sending a request that would be silently dropped.
            StarMiner.LOGGER.warn("Gravity core screen has no core position; settings change ignored");
            return;
        }
        PacketDistributor.sendToServer(new GravityCoreSettingsPayload(
                pos, gravityRange, starRadius, fieldType, useBufferArea, speedIndex));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderControlTooltip(graphics, mouseX, mouseY);
    }

    /**
     * Explains the control under the mouse, including the full name of the value it edits.
     *
     * <p>The readout panel is only 36 px wide so it shows short labels; this is where the full names and
     * the step behaviour are spelled out.
     */
    private void renderControlTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        for (Control control : this.controls) {
            if (!control.button().isMouseOver(mouseX, mouseY)) {
                continue;
            }
            graphics.renderTooltip(this.font,
                    java.util.List.of(
                            Component.translatable(control.field().nameKey),
                            Component.translatable("starInfo.ctl.hint")),
                    java.util.Optional.empty(), mouseX, mouseY);
            return;
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // The artwork has no title text, so draw both labels in the strips above their slots.
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                0x404040, false);

        // The panel is only 36 px wide, so short labels are used here and the full names appear as
        // tooltips on the controls. The order mirrors the controls on the right hand side.
        int x = GuiLayout.panelTextX();
        int y = GuiLayout.panelTextY();

        graphics.drawString(this.font, Component.translatable("starInfo.short.grange",
                Integer.toString(this.menu.getGravityRange())), x, y, VALUE_COLOUR, false);
        y += GuiLayout.PANEL_LINE_HEIGHT;

        graphics.drawString(this.font, Component.translatable("starInfo.short.starrad",
                Integer.toString(this.menu.getStarRadius())), x, y, VALUE_COLOUR, false);
        y += GuiLayout.PANEL_LINE_HEIGHT;

        graphics.drawString(this.font, Component.translatable(GravityConst.fieldTypeTranslationKey(this.menu.getFieldType())),
                x, y, VALUE_COLOUR, false);
        y += GuiLayout.PANEL_LINE_HEIGHT;

        graphics.drawString(this.font, Component.translatable(this.menu.getBuildSpeed().translationKey()),
                x, y, VALUE_COLOUR, false);
        y += GuiLayout.PANEL_LINE_HEIGHT;

        graphics.drawString(this.font, Component.translatable(this.menu.isUsingBufferArea() ? "options.on" : "options.off"),
                x, y, VALUE_COLOUR, false);

        // Working state and build progress go directly under the panel.
        int stateY = GuiLayout.PANEL_Y + GuiLayout.PANEL_HEIGHT + 3;
        graphics.drawString(this.font,
                Component.translatable(this.menu.isWorking() ? "starInfo.working" : "starInfo.idle"),
                x, stateY, this.menu.isWorking() ? STATE_WORKING_COLOUR : STATE_IDLE_COLOUR, false);
        graphics.drawString(this.font,
                Component.literal(this.menu.getPlacedBlocks() + "/" + this.menu.getShellCellCount()),
                x, stateY + GuiLayout.PANEL_LINE_HEIGHT, VALUE_COLOUR, false);
    }
}
