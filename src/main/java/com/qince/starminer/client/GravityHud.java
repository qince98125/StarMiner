package com.qince.starminer.client;

import com.qince.starminer.api.GravityState;
import com.qince.starminer.registry.SMEntityData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Draws the current gravity direction above the hotbar while the player is inside a field.
 *
 * <p>Registered explicitly on the game event bus from the client entrypoint, so this class is never
 * loaded on a dedicated server.
 */
public final class GravityHud {

    private static final int MARGIN_BOTTOM = 62;
    private static final int TEXT_COLOUR = 0x9BE7FF;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }

        GravityState state = minecraft.player.getData(SMEntityData.GRAVITY_STATE);
        if (!state.inField() || state.direction().isVanilla()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Component label = Component.translatable("starInfo.gdir", Component.translatable(directionKey(state)));
        int x = (graphics.guiWidth() - minecraft.font.width(label)) / 2;
        int y = graphics.guiHeight() - MARGIN_BOTTOM;
        graphics.drawString(minecraft.font, label, x, y, TEXT_COLOUR, true);
    }

    private static String directionKey(GravityState state) {
        return "starInfo.gdir." + state.direction().name();
    }

    private GravityHud() {
    }
}
